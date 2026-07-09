package com.careconnect.repository

import com.careconnect.model.Request
import com.careconnect.model.RequestStatus
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class RequestRepositoryImpl(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : RequestRepository {

    private val collection = firestore.collection("requests")

    override suspend fun creaRichiesta(request: Request): Result<String> = runCatching {
        val docRef = collection.document()
        val requestConId = request.copy(id = docRef.id)
        docRef.set(requestConId.toFirestoreMap()).await()
        docRef.id
    }

    override suspend fun getRichiesta(requestId: String): Result<Request> = runCatching {
        val snapshot = collection.document(requestId).get().await()
        snapshot.toRequest()
            ?: throw NoSuchElementException("Richiesta non trovata: $requestId")
    }

    override suspend fun getRichiesteAperte(): Result<List<Request>> = runCatching {
        // Stessa forma di osservaRichiesteAperte() (solo whereEqualTo, nessun orderBy):
        // così NON serve alcun indice composito su Firestore. Qui però leggiamo una
        // volta sola con get() invece di registrare un listener realtime.
        val snapshot = collection
            .whereEqualTo("stato", RequestStatus.APERTA.firestoreValue)
            .get()
            .await()
        snapshot.documents.mapNotNull { it.toRequest() }
    }

    override suspend fun getRichiestePerAnziano(anzianoId: String): Result<List<Request>> = runCatching {
        // Stessa forma di osservaRichiestePerAnziano() (solo whereEqualTo("autoreId")):
        // nessun indice composito. Qui leggiamo una volta sola con get().
        val snapshot = collection
            .whereEqualTo("autoreId", anzianoId)
            .get()
            .await()
        snapshot.documents.mapNotNull { it.toRequest() }
    }

    override suspend fun aggiornaStato(
        requestId: String,
        nuovoStato: RequestStatus,
        nuovoVolontarioId: String?,
        nuovoVolontarioNome: String?
    ): Result<Unit> = runCatching {
        val docRef = collection.document(requestId)

        // FASE 5: Transaction già presente per evitare che due volontari
        // "vincano" la stessa richiesta nello stesso istante (vedi commento
        // storico più sotto). FASE 7: aggiungo solo la scrittura di
        // volontarioNome nella stessa transazione, nessun cambiamento alla
        // logica di validazione già esistente.
        firestore.runTransaction { transaction ->
            val attuale = transaction.get(docRef).toRequest()
                ?: throw NoSuchElementException("Richiesta non trovata: $requestId")

            if (!attuale.stato.canTransitionTo(nuovoStato)) {
                throw IllegalStateException(
                    "Transizione non ammessa: ${attuale.stato} -> $nuovoStato"
                )
            }

            val aggiornamenti = mutableMapOf<String, Any?>("stato" to nuovoStato.firestoreValue)
            if (nuovoVolontarioId != null || nuovoStato == RequestStatus.APERTA) {
                // APERTA senza volontarioId esplicito => rilascio, azzera i campi del volontario
                aggiornamenti["volontarioId"] = nuovoVolontarioId
                aggiornamenti["volontarioNome"] = if (nuovoVolontarioId != null) nuovoVolontarioNome else null
            }

            transaction.update(docRef, aggiornamenti)
        }.await()
    }

    override suspend fun modificaRichiesta(
        requestId: String,
        nuovoTipo: String,
        nuovaDescrizione: String
    ): Result<Unit> = runCatching {
        val docRef = collection.document(requestId)
        val attuale = docRef.get().await().toRequest()
            ?: throw NoSuchElementException("Richiesta non trovata: $requestId")

        if (attuale.stato != RequestStatus.APERTA) {
            throw IllegalStateException(
                "Impossibile modificare: la richiesta non è più APERTA (stato attuale: ${attuale.stato})"
            )
        }

        docRef.update(
            mapOf(
                "tipo" to nuovoTipo,
                "descrizione" to nuovaDescrizione
            )
        ).await()
    }

    override fun osservaRichiesteAperte(): Flow<List<Request>> = callbackFlow {
        val listener = collection
            .whereEqualTo("stato", RequestStatus.APERTA.firestoreValue)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val richieste = snapshot?.documents?.mapNotNull { it.toRequest() } ?: emptyList()
                trySend(richieste)
            }
        awaitClose { listener.remove() }
    }

    override fun osservaRichiestePerAnziano(anzianoId: String): Flow<List<Request>> = callbackFlow {
        val listener = collection
            .whereEqualTo("autoreId", anzianoId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val richieste = snapshot?.documents?.mapNotNull { it.toRequest() } ?: emptyList()
                trySend(richieste)
            }
        awaitClose { listener.remove() }
    }

    override fun osservaRichiestePerVolontario(volontarioId: String): Flow<List<Request>> = callbackFlow {
        val statiAttivi = listOf(
            RequestStatus.PRESA_IN_CARICO.firestoreValue,
            RequestStatus.COMPLETATA_DAL_VOLONTARIO.firestoreValue
        )
        val listener = collection
            .whereEqualTo("volontarioId", volontarioId)
            .whereIn("stato", statiAttivi)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val richieste = snapshot?.documents?.mapNotNull { it.toRequest() } ?: emptyList()
                trySend(richieste)
            }
        awaitClose { listener.remove() }
    }

    // Mapping firestore <-> modello di dominio

    private fun DocumentSnapshot.toRequest(): Request? {
        if (!exists()) return null
        val statoRaw = getString("stato") ?: return null
        return Request(
            id = id,
            autoreId = getString("autoreId") ?: "",
            autoreNome = getString("autoreNome") ?: "",
            autoreIndirizzo = getString("autoreIndirizzo") ?: "",
            tipo = getString("tipo") ?: "",
            descrizione = getString("descrizione") ?: "",
            stato = RequestStatus.fromFirestoreValue(statoRaw),
            volontarioId = getString("volontarioId"),
            volontarioNome = getString("volontarioNome"),
            timestampCreazione = getTimestamp("timestampCreazione") ?: com.google.firebase.Timestamp.now(),
            posizione = getGeoPoint("posizione")
        )
    }

    private fun Request.toFirestoreMap(): Map<String, Any?> = mapOf(
        "autoreId" to autoreId,
        "autoreNome" to autoreNome,
        "autoreIndirizzo" to autoreIndirizzo,
        "tipo" to tipo,
        "descrizione" to descrizione,
        "stato" to stato.firestoreValue,
        "volontarioId" to volontarioId,
        "volontarioNome" to volontarioNome,
        "timestampCreazione" to timestampCreazione,
        "posizione" to posizione
    )
}
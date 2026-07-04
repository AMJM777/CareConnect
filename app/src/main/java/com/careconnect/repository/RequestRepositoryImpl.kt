package com.careconnect.repository

import com.careconnect.model.Request
import com.careconnect.model.RequestStatus
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ktx.toObject
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

    override suspend fun aggiornaStato(
        requestId: String,
        nuovoStato: RequestStatus,
        nuovoVolontarioId: String?
    ): Result<Unit> = runCatching {
        val docRef = collection.document(requestId)

        // FASE 5: prima qui c'era un get() + update() separati. Con più
        // volontari che possono premere "Prendi in carico" sulla stessa
        // richiesta nello stesso momento, quel pattern permetteva una race
        // condition (entrambi leggono "APERTA" prima che l'altro scriva).
        //
        // Con una Transaction, Firestore garantisce che lettura e scrittura
        // avvengano come un'unica operazione atomica: se due transazioni si
        // scontrano sullo stesso documento, una delle due viene rieseguita
        // automaticamente dall'SDK con i dati aggiornati. Al secondo
        // tentativo, "attuale.stato" non è più APERTA, quindi
        // "canTransitionTo" fallisce e la transazione lancia un errore
        // gestito normalmente come Result.failure (es. Toast "richiesta già
        // presa in carico da un altro volontario").
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
                // APERTA senza volontarioId esplicito => rilascio, azzera il campo
                aggiornamenti["volontarioId"] = nuovoVolontarioId
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

        // Controllo di sicurezza lato server (oltre a quello già fatto in UI,
        // che nasconde il bottone "Modifica" se non APERTA): protegge dal
        // caso raro ma reale in cui un volontario prende in carico la
        // richiesta proprio mentre l'anziano sta ancora compilando il form.
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
        // "Attive" = presa in carico oppure completata dal volontario ma non
        // ancora confermata dal garante. CONFERMATA/ANNULLATA non compaiono
        // qui: sono uno stato terminale, il volontario non deve più agire.
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
            tipo = getString("tipo") ?: "",
            descrizione = getString("descrizione") ?: "",
            stato = RequestStatus.fromFirestoreValue(statoRaw),
            volontarioId = getString("volontarioId"),
            timestampCreazione = getTimestamp("timestampCreazione") ?: com.google.firebase.Timestamp.now(),
            posizione = getGeoPoint("posizione")
        )
    }

    private fun Request.toFirestoreMap(): Map<String, Any?> = mapOf(
        "autoreId" to autoreId,
        "tipo" to tipo,
        "descrizione" to descrizione,
        "stato" to stato.firestoreValue,
        "volontarioId" to volontarioId,
        "timestampCreazione" to timestampCreazione,
        "posizione" to posizione
    )
}
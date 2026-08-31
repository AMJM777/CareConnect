package com.careconnect.repository

import com.careconnect.model.User
import com.careconnect.model.UserRole
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

// implementazione di UserRepository
class UserRepositoryImpl(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : UserRepository {

    private val collection = firestore.collection("users")
    private val ratingsCollection = firestore.collection("ratings")

    override suspend fun salvaUtente(user: User): Result<Unit> = runCatching {
        collection.document(user.uid).set(user.toFirestoreMap()).await()
    }

    override suspend fun getUtente(uid: String): Result<User> = runCatching {
        val snapshot = collection.document(uid).get().await()
        snapshot.toUser()
            ?: throw NoSuchElementException("Utente non trovato: $uid")
    }

    // segnala che un candidato è già in uso, così il ciclo riprova con un altro
    private class CodiceOccupatoException : Exception()

    override suspend fun ottieniOCreaCodiceInvito(anzianoId: String): Result<String> = runCatching {
        val anzianoRef = collection.document(anzianoId)
        val anziano = anzianoRef.get().await().toUser()
            ?: throw NoSuchElementException("Utente non trovato: $anzianoId")

        if (anziano.ruolo != UserRole.ANZIANO) {
            throw IllegalStateException("Solo un anziano può avere un codice invito")
        }

        // se esiste già, lo riusa
        anziano.codiceInvito?.let { return@runCatching it }

        val codiciCollection = firestore.collection("codiciInvito")

        // prova qualche candidato: l'unicità è garantita dall'id del documento
        var tentativi = 0
        while (tentativi < 5) {
            val candidato = generaCodiceCasuale()
            val candidatoRef = codiciCollection.document(candidato)

            val esito = runCatching {
                firestore.runTransaction { txn ->
                    // la lettura va fatta prima di ogni scrittura nella transazione
                    if (txn.get(candidatoRef).exists()) {
                        throw CodiceOccupatoException()
                    }
                    // registra la mappa codice -> anziano e salva il codice sull'utente
                    txn.set(candidatoRef, mapOf("anzianoId" to anzianoId))
                    txn.update(anzianoRef, "codiceInvito", candidato)
                    null
                }.await()
            }

            if (esito.isSuccess) {
                return@runCatching candidato
            }

            // se è fallita perché il codice era occupato riprova, altrimenti propaga l'errore
            val causa = esito.exceptionOrNull()
            val occupato = causa is CodiceOccupatoException || causa?.cause is CodiceOccupatoException
            if (!occupato) {
                throw causa ?: IllegalStateException("Errore durante la generazione del codice")
            }
            tentativi++
        }
        throw IllegalStateException("Impossibile generare un codice invito univoco, riprova")
    }

    override suspend fun trovaAnzianoPerCodiceInvito(codice: String): Result<User> = runCatching {
        val risultato = collection
            .whereEqualTo("codiceInvito", codice)
            .whereEqualTo("ruolo", UserRole.ANZIANO.firestoreValue)
            .limit(1)
            .get()
            .await()

        risultato.documents.firstOrNull()?.toUser()
            ?: throw NoSuchElementException("Nessun anziano trovato con questo codice")
    }

    override suspend fun collegaFamiliareAdAnziano(
        anzianoId: String,
        familiareId: String
    ): Result<Unit> = runCatching {
        val anzianoDocRef = collection.document(anzianoId)
        val familiareDocRef = collection.document(familiareId)

        // Controllo che i ruoli siano giusti e che non sia già collegato a questo anziano
        val anziano = anzianoDocRef.get().await().toUser()
            ?: throw NoSuchElementException("Anziano non trovato: $anzianoId")
        val familiare = familiareDocRef.get().await().toUser()
            ?: throw NoSuchElementException("Familiare non trovato: $familiareId")

        if (anziano.ruolo != UserRole.ANZIANO) {
            throw IllegalStateException("L'utente collegato non è un anziano")
        }
        if (familiare.ruolo != UserRole.FAMILIARE) {
            throw IllegalStateException("Solo un account Familiare può collegarsi con un codice invito")
        }
        if (anzianoId in familiare.anzianiCollegatiIds) {
            throw IllegalStateException("Segui già questa persona")
        }

        // includo anche gli anziani già seguiti (il mapper li migra dal vecchio
        // campo singolo): così il primo collegamento non fa perdere lo storico
        val anzianiDaScrivere: List<Any> = (familiare.anzianiCollegatiIds + anzianoId).distinct()

        // WriteBatch: le due scritture vanno a buon fine insieme o falliscono
        // insieme; arrayUnion evita che collegamenti in parallelo si sovrascrivano
        firestore.batch()
            .update(anzianoDocRef, "familiariCollegatiIds", FieldValue.arrayUnion(familiareId))
            .update(familiareDocRef, "anzianiCollegatiIds", FieldValue.arrayUnion(*anzianiDaScrivere.toTypedArray()))
            .commit()
            .await()
    }


    override suspend fun aggiornaRatingMedio(volontarioId: String): Result<Unit> = runCatching {
        val valutazioni = ratingsCollection
            .whereEqualTo("volontarioId", volontarioId)
            .get()
            .await()

        val stelle = valutazioni.documents.mapNotNull { it.getLong("stelle")?.toInt() }

        // se per qualche motivo la lista risultasse vuota non si scrive una media senza senso
        if (stelle.isEmpty()) return@runCatching

        val media = stelle.average()
        collection.document(volontarioId).update("ratingMedio", media).await()
    }

    override suspend fun aggiornaFcmToken(uid: String, token: String): Result<Unit> = runCatching {
        // update che tocca solo il campo "fcmToken".
        collection.document(uid).update("fcmToken", token).await()
    }


    // Alfabeto senza 0/O/1/I: caratteri facili da confondere
    private fun generaCodiceCasuale(): String {
        val alfabeto = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return (1..6).map { alfabeto.random() }.joinToString("")
    }

    // funzione di mapping

    private fun DocumentSnapshot.toUser(): User? {
        if (!exists()) return null
        val ruoloRaw = getString("ruolo") ?: return null
        return User(
            uid = id,
            nome = getString("nome") ?: "",
            ruolo = UserRole.fromFirestoreValue(ruoloRaw),
            familiariCollegatiIds = (get("familiariCollegatiIds") as? List<*>)
                ?.filterIsInstance<String>() ?: emptyList(),
            codiceInvito = getString("codiceInvito"),
            indirizzo = getString("indirizzo"),
            anzianoCollegatoId = getString("anzianoCollegatoId"),
            anzianiCollegatiIds = run {
                val lista = (get("anzianiCollegatiIds") as? List<*>)?.filterIsInstance<String>()
                // migrazione: se manca la lista uso il vecchio campo singolo, se presente
                lista ?: getString("anzianoCollegatoId")?.let { listOf(it) } ?: emptyList()
            },
            ratingMedio = getDouble("ratingMedio"),
            bio = getString("bio")
        )
    }
    //funzione di mapping
    private fun User.toFirestoreMap(): Map<String, Any?> = mapOf(
        "nome" to nome,
        "ruolo" to ruolo.firestoreValue,
        "familiariCollegatiIds" to familiariCollegatiIds,
        "codiceInvito" to codiceInvito,
        "indirizzo" to indirizzo,
        "anzianoCollegatoId" to anzianoCollegatoId,
        "anzianiCollegatiIds" to anzianiCollegatiIds,
        "ratingMedio" to ratingMedio,
        "bio" to bio
    )
}
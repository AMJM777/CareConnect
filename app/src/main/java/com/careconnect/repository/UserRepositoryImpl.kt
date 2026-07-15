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

    override suspend fun ottieniOCreaCodiceInvito(anzianoId: String): Result<String> = runCatching {
        val anziano = collection.document(anzianoId).get().await().toUser()
            ?: throw NoSuchElementException("Utente non trovato: $anzianoId")

        if (anziano.ruolo != UserRole.ANZIANO) {
            throw IllegalStateException("Solo un anziano può avere un codice invito")
        }

        // Se esiste già, lo riusa
        anziano.codiceInvito?.let { return@runCatching it }

        // egnera un candidato e verifica che nessun altro lo stia già usando.
        // 6 caratteri da un alfabeto di 32 simboli (~1 miliardo di combinaazioni)
        var codiceTrovato: String? = null
        var tentativi = 0
        while (codiceTrovato == null && tentativi < 5) {
            val candidato = generaCodiceCasuale()
            val giaUsato = collection
                .whereEqualTo("codiceInvito", candidato)
                .limit(1)
                .get()
                .await()
            if (giaUsato.isEmpty) {
                codiceTrovato = candidato
            }
            tentativi++
        }
        val codiceFinale = codiceTrovato
            ?: throw IllegalStateException("Impossibile generare un codice invito univoco, riprova")

        // fa l'update solo di "codiceInvito"
        collection.document(anzianoId).update("codiceInvito", codiceFinale).await()
        codiceFinale
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

        // Controllo che i ruoli siano giusti e che il familiare non sia già
        // collegato altrove
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
        if (familiare.anzianoCollegatoId != null) {
            throw IllegalStateException("Sei già collegato a un altro anziano")
        }

        // WriteBatch: le due scritture vanno a buon fine insieme o falliscono
        // insieme: se due familiari si collegano nello stesso
        // istante, nessuno dei due sovrascrive l'aggiunta dell'altro
        firestore.batch()
            .update(anzianoDocRef, "familiariCollegatiIds", FieldValue.arrayUnion(familiareId))
            .update(familiareDocRef, "anzianoCollegatoId", anzianoId)
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
        "ratingMedio" to ratingMedio,
        "bio" to bio
    )
}
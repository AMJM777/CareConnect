package com.careconnect.repository

import com.careconnect.model.User
import com.careconnect.model.UserRole
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class UserRepositoryImpl(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : UserRepository {

    private val collection = firestore.collection("users")

    // ATTENZIONE: questo è un .set() completo, non un update parziale.
    // Chi chiama questo metodo per modificare UN SOLO campo deve comunque
    // passare l'intero oggetto User aggiornato, altrimenti i campi non
    // inclusi verrebbero cancellati su Firestore. I nuovi metodi di questa
    // fase (sotto) usano invece .update() parziale apposta per evitare
    // questo rischio quando serve toccare un solo campo.
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

        // Se esiste già, lo riusiamo: nessuna scrittura necessaria.
        anziano.codiceInvito?.let { return@runCatching it }

        // Genera un candidato e verifica che nessun altro lo stia già
        // usando. Con 6 caratteri da un alfabeto di 32 simboli le
        // combinazioni possibili sono ~1 miliardo: al massimo 5 tentativi
        // sono più che sufficienti in pratica, ma il controllo esplicito
        // è comunque più solido che "confidare" senza verificare.
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

        // Update parziale: tocca solo "codiceInvito", non l'intero documento.
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

        // Letture di controllo PRIMA di scrivere: vogliamo essere sicuri
        // che i ruoli siano quelli giusti e che il familiare non sia già
        // collegato altrove, per non lasciare il dato in uno stato ambiguo.
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
        // insieme. FieldValue.arrayUnion() è già atomico e concorrenza-sicuro
        // lato server: anche se due familiari si collegano nello stesso
        // istante, nessuno dei due sovrascrive l'aggiunta dell'altro.
        firestore.batch()
            .update(anzianoDocRef, "familiariCollegatiIds", FieldValue.arrayUnion(familiareId))
            .update(familiareDocRef, "anzianoCollegatoId", anzianoId)
            .commit()
            .await()
    }

    /** Alfabeto senza 0/O/1/I: caratteri facili da confondere se il codice viene letto ad alta voce o scritto a mano. */
    private fun generaCodiceCasuale(): String {
        val alfabeto = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return (1..6).map { alfabeto.random() }.joinToString("")
    }

    // --- Mapping Firestore <-> modello di dominio ---

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
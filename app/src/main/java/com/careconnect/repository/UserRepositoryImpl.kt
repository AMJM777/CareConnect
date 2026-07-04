package com.careconnect.repository

import com.careconnect.model.User
import com.careconnect.model.UserRole
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class UserRepositoryImpl(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : UserRepository {

    private val collection = firestore.collection("users")

    // ATTENZIONE: questo è un .set() completo, non un update parziale.
    // Chi chiama questo metodo per modificare UN SOLO campo (es. solo la bio)
    // deve comunque passare l'intero oggetto User aggiornato (letto prima con
    // getUtente() e poi copiato con .copy()), altrimenti i campi non inclusi
    // nella chiamata verrebbero cancellati su Firestore.
    override suspend fun salvaUtente(user: User): Result<Unit> = runCatching {
        collection.document(user.uid).set(user.toFirestoreMap()).await()
    }

    override suspend fun getUtente(uid: String): Result<User> = runCatching {
        val snapshot = collection.document(uid).get().await()
        snapshot.toUser()
            ?: throw NoSuchElementException("Utente non trovato: $uid")
    }

    // --- Mapping Firestore <-> modello di dominio ---

    private fun DocumentSnapshot.toUser(): User? {
        if (!exists()) return null
        val ruoloRaw = getString("ruolo") ?: return null
        return User(
            uid = id,
            nome = getString("nome") ?: "",
            ruolo = UserRole.fromFirestoreValue(ruoloRaw),
            familiareCollegatoId = getString("familiareCollegatoId"),
            anzianoCollegatoId = getString("anzianoCollegatoId"),
            ratingMedio = getDouble("ratingMedio"),
            bio = getString("bio")
        )
    }

    private fun User.toFirestoreMap(): Map<String, Any?> = mapOf(
        "nome" to nome,
        "ruolo" to ruolo.firestoreValue,
        "familiareCollegatoId" to familiareCollegatoId,
        "anzianoCollegatoId" to anzianoCollegatoId,
        "ratingMedio" to ratingMedio,
        "bio" to bio
    )
}
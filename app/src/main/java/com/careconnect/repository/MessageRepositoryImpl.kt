package com.careconnect.repository

import com.careconnect.model.Message
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

// implementazione di MessageRepository su Firestore
class MessageRepositoryImpl(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : MessageRepository {

    private val collection = firestore.collection("messaggi")

    override suspend fun inviaMessaggio(messaggio: Message): Result<String> = runCatching {
        val docRef = collection.document()
        val messaggioConId = messaggio.copy(id = docRef.id)
        docRef.set(messaggioConId.toFirestoreMap()).await()
        docRef.id
    }

    // listener realtime filtrato per requestId E per il campo del partecipante
    // corrente (anzianoId o volontarioId = proprio uid): il doppio filtro serve
    // a soddisfare le security rules (le regole non sono filtri). Due filtri di
    // uguaglianza non richiedono un indice composito.
    override fun osservaMessaggiPerRichiesta(
        requestId: String,
        campoUtente: String,
        uidUtente: String
    ): Flow<List<Message>> = callbackFlow {
        val listener = collection
            .whereEqualTo("requestId", requestId)
            .whereEqualTo(campoUtente, uidUtente)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val messaggi = snapshot?.documents?.mapNotNull { it.toMessage() } ?: emptyList()
                trySend(messaggi)
            }
        awaitClose { listener.remove() }
    }

    // mapping da documento Firestore a Message
    private fun DocumentSnapshot.toMessage(): Message? {
        if (!exists()) return null
        val testo = getString("testo") ?: return null
        return Message(
            id = id,
            requestId = getString("requestId") ?: "",
            anzianoId = getString("anzianoId") ?: "",
            volontarioId = getString("volontarioId") ?: "",
            mittenteId = getString("mittenteId") ?: "",
            testo = testo,
            timestamp = getTimestamp("timestamp") ?: Timestamp.now()
        )
    }

    // mapping da Message a mappa chiave-valore per Firestore
    private fun Message.toFirestoreMap(): Map<String, Any?> = mapOf(
        "requestId" to requestId,
        "anzianoId" to anzianoId,
        "volontarioId" to volontarioId,
        "mittenteId" to mittenteId,
        "testo" to testo,
        "timestamp" to timestamp
    )
}
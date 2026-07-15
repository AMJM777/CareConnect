package com.careconnect.repository

import com.careconnect.model.SosAlert
import com.careconnect.model.SosStatus
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

//implentazione di SosRepository
class SosRepositoryImpl(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : SosRepository {

    private val collection = firestore.collection("sosAlerts")

    override suspend fun creaAlert(alert: SosAlert): Result<String> = runCatching {
        val docRef = collection.document()
        val alertConId = alert.copy(id = docRef.id)
        docRef.set(alertConId.toFirestoreMap()).await()
        docRef.id
    }

    override suspend fun aggiornaStato(alertId: String, nuovoStato: SosStatus): Result<Unit> = runCatching {
        val docRef = collection.document(alertId)
        val statoRaw = docRef.get().await().getString("stato")
            ?: throw NoSuchElementException("Alert non trovato: $alertId")
        val statoAttuale = SosStatus.fromFirestoreValue(statoRaw)

        // un alert chiuso è uno stato terminale
        if (statoAttuale == SosStatus.CHIUSO) {
            throw IllegalStateException("Impossibile modificare un alert già CHIUSO")
        }

        docRef.update("stato", nuovoStato.firestoreValue).await()
    }

    // registra un listener realtime su Firestore filtrato per familiareId,
    // così il familiare vede comparire l'SOS nell'istante in cui viene creato
    override fun osservaAlertPerFamiliare(familiareId: String): Flow<List<SosAlert>> = callbackFlow {
        val listener = collection
            .whereEqualTo("familiareId", familiareId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val alerts = snapshot?.documents?.mapNotNull { it.toSosAlert() } ?: emptyList()
                trySend(alerts)
            }
        awaitClose { listener.remove() }
    }

//funzione di mapping che converte l'alert in una mappa chiave-valore per firestore
    private fun DocumentSnapshot.toSosAlert(): SosAlert? {
        if (!exists()) return null
        val statoRaw = getString("stato") ?: return null
        return SosAlert(
            id = id,
            anzianoId = getString("anzianoId") ?: "",
            familiareId = getString("familiareId") ?: "",
            stato = SosStatus.fromFirestoreValue(statoRaw),
            messaggio = getString("messaggio"),
            timestampCreazione = getTimestamp("timestampCreazione") ?: Timestamp.now()
        )
    }

    private fun SosAlert.toFirestoreMap(): Map<String, Any?> = mapOf(
        "anzianoId" to anzianoId,
        "familiareId" to familiareId,
        "stato" to stato.firestoreValue,
        "messaggio" to messaggio,
        "timestampCreazione" to timestampCreazione
    )
}
package com.careconnect.repository

import com.careconnect.model.Rating
import com.careconnect.model.RequestStatus
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import com.google.firebase.firestore.DocumentSnapshot

// implementazione di RatingRepository
class RatingRepositoryImpl(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : RatingRepository {

    private val ratingsCollection = firestore.collection("ratings")
    private val requestsCollection = firestore.collection("requests")

    override suspend fun creaRatingEConfermaRichiesta(rating: Rating): Result<String> = runCatching {
        val ratingDocRef = ratingsCollection.document()
        val requestDocRef = requestsCollection.document(rating.requestId)

        firestore.runTransaction { transaction ->
            val requestSnapshot = transaction.get(requestDocRef)
            val statoRaw = requestSnapshot.getString("stato")
                ?: throw NoSuchElementException("Richiesta non trovata: ${rating.requestId}")
            val statoAttuale = RequestStatus.fromFirestoreValue(statoRaw)

            if (!statoAttuale.canTransitionTo(RequestStatus.CONFERMATA)) {
                throw IllegalStateException(
                    "Impossibile confermare: stato attuale $statoAttuale non ammette CONFERMATA"
                )
            }

            val ratingConId = rating.copy(id = ratingDocRef.id)
            transaction.set(ratingDocRef, ratingConId.toFirestoreMap())
            transaction.update(requestDocRef, "stato", RequestStatus.CONFERMATA.firestoreValue)

            ratingDocRef.id
        }.await()
    }

    // funzione di mapping

    private fun Rating.toFirestoreMap(): Map<String, Any?> = mapOf(
        "requestId" to requestId,
        "volontarioId" to volontarioId,
        "stelle" to stelle,
        "commento" to commento,
        "valutatoreId" to valutatoreId
    )

    override suspend fun getRatingsPerVolontario(volontarioId: String): Result<List<Rating>> = runCatching {
        ratingsCollection
            .whereEqualTo("volontarioId", volontarioId)
            .get()
            .await()
            .documents
            .mapNotNull { it.toRating() }
    }

    // funzione di mapping da Firestore a Rating
    private fun DocumentSnapshot.toRating(): Rating? {
        if (!exists()) return null
        return Rating(
            id = id,
            requestId = getString("requestId") ?: "",
            volontarioId = getString("volontarioId") ?: "",
            stelle = (getLong("stelle") ?: 0L).toInt(),
            commento = getString("commento"),
            valutatoreId = getString("valutatoreId") ?: ""
        )
    }
}
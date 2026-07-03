package com.careconnect.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint

data class Request(
    val id: String = "",
    val autoreId: String = "",                  // uid dell'anziano
    val tipo: String = "",                       // "spesa" | "bolletta" | "assistenza_digitale" | "altro"
    val descrizione: String = "",
    val stato: RequestStatus = RequestStatus.APERTA,
    val volontarioId: String? = null,
    val timestampCreazione: Timestamp = Timestamp.now(),
    val posizione: GeoPoint? = null
)
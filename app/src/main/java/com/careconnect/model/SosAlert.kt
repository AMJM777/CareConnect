package com.careconnect.model

import com.google.firebase.Timestamp

// rappresenta una segnalazione di emergenza (SOS) lanciata dall'anziano
data class SosAlert(
    val id: String = "",
    val anzianoId: String = "",
    val familiareId: String = "",
    val stato: SosStatus = SosStatus.ATTIVO,
    val messaggio: String? = null,
    val timestampCreazione: Timestamp = Timestamp.now()
)
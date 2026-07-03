package com.careconnect.model

import com.google.firebase.Timestamp

data class SosAlert(
    val id: String = "",
    val anzianoId: String = "",
    val familiareId: String = "",
    val stato: SosStatus = SosStatus.ATTIVO,
    val messaggio: String? = null,
    val timestampCreazione: Timestamp = Timestamp.now()
)
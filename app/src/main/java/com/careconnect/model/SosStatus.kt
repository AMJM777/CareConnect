package com.careconnect.model

enum class SosStatus(val firestoreValue: String) {
    ATTIVO("attivo"),
    VISTO("visto"),
    CHIUSO("chiuso");

    companion object {
        fun fromFirestoreValue(value: String): SosStatus =
            entries.firstOrNull { it.firestoreValue == value }
                ?: throw IllegalArgumentException("Stato SOS sconosciuto: $value")
    }
}
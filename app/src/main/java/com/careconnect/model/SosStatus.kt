package com.careconnect.model
// rappresenta lo stato di una segnalazione SOS: attiva appena lanciata,
// vista quando il familiare la apre, chiusa quando l'emergenza è risolta
enum class SosStatus(val firestoreValue: String) {
    ATTIVO("attivo"),
    VISTO("visto"),
    CHIUSO("chiuso");

    companion object {
        // funzione per ottenere lo stato a partire dal valore salvato su Firestore

        fun fromFirestoreValue(value: String): SosStatus =
            entries.firstOrNull { it.firestoreValue == value }
                ?: throw IllegalArgumentException("Stato SOS sconosciuto: $value")
    }
}
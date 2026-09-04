package com.careconnect.model

// rappresenta il ruolo di un utente nell'app: anziano, volontario o familiare/garante
enum class UserRole(val firestoreValue: String) {
    ANZIANO("anziano"),
    VOLONTARIO("volontario"),
    FAMILIARE("familiare");

    companion object {
        // funzione per ottenere il ruolo a partire dal valore salvato su Firestore

        fun fromFirestoreValue(value: String): UserRole =
            entries.firstOrNull { it.firestoreValue == value }
                ?: throw IllegalArgumentException("Ruolo utente sconosciuto: $value")
    }
}
package com.careconnect.model

enum class UserRole(val firestoreValue: String) {
    ANZIANO("anziano"),
    VOLONTARIO("volontario"),
    FAMILIARE("familiare");

    companion object {
        fun fromFirestoreValue(value: String): UserRole =
            entries.firstOrNull { it.firestoreValue == value }
                ?: throw IllegalArgumentException("Ruolo utente sconosciuto: $value")
    }
}
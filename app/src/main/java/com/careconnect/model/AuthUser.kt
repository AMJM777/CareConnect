package com.careconnect.model

/**
 * è l'identità autenticata (Firebase Auth), separata dal tipo
 * FirebaseUser dell'SDK, non contiene il ruolo (che vive nel profilo
 * Firestore, vedi User.kt), ma include il nome quando disponibile
 * (es. fornito da Google al primo accesso)
 */
data class AuthUser(
    val uid: String,
    val email: String?,
    val nome: String? = null
)
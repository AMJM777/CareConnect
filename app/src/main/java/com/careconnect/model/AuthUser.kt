package com.careconnect.model

/**
 * è l'identità autenticata (Firebase Auth), separata dal tipo
 * FirebaseUser dell'SDK, non contiene il ruolo (che vive nel profilo
 * Firestore, User.kt), ma include il nome quando disponibile
 */
data class AuthUser(
    val uid: String,
    val email: String?,
    val nome: String? = null
)
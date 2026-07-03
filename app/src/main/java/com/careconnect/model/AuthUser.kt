package com.careconnect.model

/**
 * Rappresenta l'identità autenticata (Firebase Auth), disaccoppiata
 * dal tipo FirebaseUser dell'SDK. Non contiene il ruolo (vive nel
 * profilo Firestore, vedi User.kt), ma include il nome quando
 * disponibile (es. fornito automaticamente da Google al primo accesso),
 * utile per pre-compilare il form di completamento profilo.
 */
data class AuthUser(
    val uid: String,
    val email: String?,
    val nome: String? = null
)
package com.careconnect.repository

import com.careconnect.model.AuthUser
import kotlinx.coroutines.flow.Flow

// interfaccia che definisce le operazioni di autenticazione disponibili,
// indipendentemente dal provider usato (Firebase, nell'implementazione).
interface AuthRepository {

    // stato di autenticazione in tempo reale: emette null quando non c'è
    // utente loggato (es. dopo logout). usato per il check di sessione all'avvio dell'app
    fun osservaStatoAutenticazione(): Flow<AuthUser?>

    // funzione per leggere l'utente correntemente autenticato, se presente
    fun utenteCorrente(): AuthUser?

    // funzione per creare una nuova credenziale Firebase Auth. non salva nulla su Firestore
    suspend fun registraConEmail(email: String, password: String): Result<AuthUser>

    // funzione per autenticare un utente già registrato con email e password
    suspend fun loginConEmail(email: String, password: String): Result<AuthUser>

    /**
     * funzione per autenticare su Firebase con l'id token di Google.
     * il recupero del token (Credential Manager, richiede Context/Activity)
     * resta fuori da questo repository, qui arriva pronto per lo scambio con Firebase
     */
    suspend fun loginConGoogle(idToken: String): Result<AuthUser>
    //funzione di logout
    fun logout()
}
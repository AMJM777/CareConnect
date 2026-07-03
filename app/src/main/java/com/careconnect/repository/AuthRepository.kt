package com.careconnect.repository

import com.careconnect.model.AuthUser
import kotlinx.coroutines.flow.Flow

interface AuthRepository {

    /**
     * Stato di autenticazione in tempo reale.
     * Emette null quando non c'è utente loggato (es. dopo logout).
     * Usato per l'auto-login/check di sessione all'avvio dell'app.
     */
    fun osservaStatoAutenticazione(): Flow<AuthUser?>

    /** Utente correntemente autenticato, se presente. Lettura sincrona, nessuna chiamata di rete. */
    fun utenteCorrente(): AuthUser?

    /** Crea una nuova credenziale Firebase Auth. Non salva nulla su Firestore. */
    suspend fun registraConEmail(email: String, password: String): Result<AuthUser>

    suspend fun loginConEmail(email: String, password: String): Result<AuthUser>

    /**
     * Autentica su Firebase con l'ID token Google.
     * Il recupero del token (Credential Manager, richiede Context/Activity)
     * resta fuori da questo repository: qui arriva già pronto per lo scambio con Firebase.
     * Nota: Firebase gestisce login e registrazione Google come un'unica operazione;
     * la distinzione "utente nuovo vs esistente" va gestita a livello di ViewModel
     */
    suspend fun loginConGoogle(idToken: String): Result<AuthUser>

    fun logout()
}
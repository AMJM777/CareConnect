package com.careconnect.repository

import com.careconnect.model.AuthUser
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

// implementazione di AuthRepository
class AuthRepositoryImpl(
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
) : AuthRepository {

    override fun osservaStatoAutenticazione(): Flow<AuthUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser?.toAuthUser())
        }
        firebaseAuth.addAuthStateListener(listener)
        awaitClose { firebaseAuth.removeAuthStateListener(listener) }
    }

    override fun utenteCorrente(): AuthUser? =
        firebaseAuth.currentUser?.toAuthUser()

    override suspend fun registraConEmail(email: String, password: String): Result<AuthUser> =
        try {
            val result = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user
                ?: return Result.failure(IllegalStateException("Registrazione riuscita ma utente nullo"))
            Result.success(user.toAuthUser())
        } catch (e: Exception) {
            Result.failure(e)
        }

    override suspend fun loginConEmail(email: String, password: String): Result<AuthUser> =
        try {
            val result = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val user = result.user
                ?: return Result.failure(IllegalStateException("Login riuscito ma utente nullo"))
            Result.success(user.toAuthUser())
        } catch (e: Exception) {
            Result.failure(e)
        }

    override suspend fun loginConGoogle(idToken: String): Result<AuthUser> =
        try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = firebaseAuth.signInWithCredential(credential).await()
            val user = result.user
                ?: return Result.failure(IllegalStateException("Login Google riuscito ma utente nullo"))
            Result.success(user.toAuthUser())
        } catch (e: Exception) {
            Result.failure(e)
        }

    override fun logout() {
        firebaseAuth.signOut()
    }
//funzione che coverte l'utente Firebase nel modello del dominio AuthUser
    private fun FirebaseUser.toAuthUser(): AuthUser =
        AuthUser(uid = uid, email = email, nome = displayName)
}
package com.careconnect.repository

import com.careconnect.model.User

interface UserRepository {

    /** Salva (crea o sovrascrive) il profilo utente su Firestore, alla registrazione. */
    suspend fun salvaUtente(user: User): Result<Unit>

    /** Legge il profilo utente per uid. */
    suspend fun getUtente(uid: String): Result<User>
}
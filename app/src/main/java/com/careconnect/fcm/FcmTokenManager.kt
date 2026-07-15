package com.careconnect.fcm

import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await

/**
 * wrapper attorno all'SDK di firebase cloud messaging: nasconde ai
 * ViewModel i dettagli dell'SDK esponendo solo un metodo semplice
 */
object FcmTokenManager {

    //funzione per recuperare il token FCM corrente del dispositvo
    suspend fun tokenCorrente(): Result<String> = runCatching {
        FirebaseMessaging.getInstance().token.await()
    }
}
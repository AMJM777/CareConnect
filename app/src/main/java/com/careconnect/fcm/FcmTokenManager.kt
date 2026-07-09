package com.careconnect.fcm

import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await

/**
 * FASE 12 — Wrapper attorno all'SDK di Firebase Cloud Messaging per recuperare
 * il token del dispositivo senza esporre l'SDK ai ViewModel. Stessa idea di
 * SessionCache, che incapsula le SharedPreferences.
 */
object FcmTokenManager {

    /** Recupera il token FCM corrente di questo dispositivo. */
    suspend fun tokenCorrente(): Result<String> = runCatching {
        FirebaseMessaging.getInstance().token.await()
    }
}
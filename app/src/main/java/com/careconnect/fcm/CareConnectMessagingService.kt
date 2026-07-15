package com.careconnect.fcm

import android.util.Log
import com.careconnect.util.NotificationHelper
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.careconnect.repository.UserRepositoryImpl
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
/**
 * Servizio che riceve i messaggi push da firebase cloud messaging (FCM).
 * anche quando l'app è chiusa  e li trasforma in notifiche visibili
 * (riusando il NotificationHelper)
 */
class CareConnectMessagingService : FirebaseMessagingService() {

    // Chiamato da Firebase quando genera o rinnova il token di questo dispositivo.
    // Il token è salvato sul profilo utente per ricevere le push in futuro.

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Nuovo token FCM: $token")

        // Se l'utente è già loggato aggiorna subito il token sul suo profilo,
        // altrimenti verrà salvato al prossimo login
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        CoroutineScope(Dispatchers.IO).launch {
            UserRepositoryImpl().aggiornaFcmToken(uid, token)
        }
    }

     // Chiamato quando arriva un messaggio push, costruisce e mostra la notifica

    override fun onMessageReceived(messaggio: RemoteMessage) {
        super.onMessageReceived(messaggio)

        // Un messaggio FCM può contenere titolo/testo pronti ("notification")
        // oppure dati grezzi decisi da noi ("data"): diamo priorità ai dati.
        val dati = messaggio.data
        val titolo = dati["titolo"] ?: messaggio.notification?.title ?: "CareConnect"
        val testo = dati["testo"] ?: messaggio.notification?.body ?: "Hai una nuova notifica"

        // L'SOS usa un canale di notifica ad alta priorità, gli altri messaggi uno normale.
        val canaleId = if (dati["tipo"] == "sos") {
            NotificationHelper.CANALE_SOS_ID
        } else {
            NotificationHelper.CANALE_GENERALE_ID
        }

        NotificationHelper.mostraNotifica(
            context = applicationContext,
            canaleId = canaleId,
            titolo = titolo,
            testo = testo,
            notificaId = ID_NOTIFICA_PUSH
        )
    }

    private companion object {
        const val TAG = "CareConnectFCM"
        const val ID_NOTIFICA_PUSH = 2001
    }
}
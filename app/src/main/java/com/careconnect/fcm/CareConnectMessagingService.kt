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
 * FASE 12 — Servizio che riceve i messaggi Firebase Cloud Messaging (FCM).
 *
 * Viene invocato dall'SDK Firebase (anche ad app chiusa) quando arriva una
 * push per questo dispositivo. Qui trasformiamo il messaggio in una notifica
 * visibile, riusando il NotificationHelper condiviso con la Fase 11.
 */
class CareConnectMessagingService : FirebaseMessagingService() {

    /**
     * Chiamato quando FCM assegna o rinnova il token di questo dispositivo.
     * Il token identifica il dispositivo come destinatario delle push: la
     * Cloud Function (Fase 12b) lo userà per sapere a chi inviare l'SOS.
     * Per ora lo registriamo nel Log; il salvataggio su Firestore è il prossimo passo.
     */

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Nuovo token FCM: $token")

        // Il token può cambiare mentre l'utente è già loggato: se c'è una
        // sessione attiva aggiorniamo subito il token sul suo profilo, altrimenti
        // le push non lo raggiungerebbero più. Se non è loggato non facciamo
        // nulla: al prossimo login il token verrà salvato comunque (AuthViewModel).
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        CoroutineScope(Dispatchers.IO).launch {
            UserRepositoryImpl().aggiornaFcmToken(uid, token)
        }
    }

    /**
     * Chiamato quando arriva un messaggio con l'app in primo piano, e sempre
     * per i messaggi di tipo "data". Costruiamo qui la notifica così decidiamo
     * NOI titolo, testo e soprattutto quale canale usare (conta per l'SOS).
     */
    override fun onMessageReceived(messaggio: RemoteMessage) {
        super.onMessageReceived(messaggio)

        // Un messaggio FCM può avere una parte "notification" (titolo/testo già
        // pronti) e/o una parte "data" (coppie chiave-valore decise da noi).
        // Diamo priorità ai dati, con fallback sulla parte notification.
        val dati = messaggio.data
        val titolo = dati["titolo"] ?: messaggio.notification?.title ?: "CareConnect"
        val testo = dati["testo"] ?: messaggio.notification?.body ?: "Hai una nuova notifica"

        // Se il messaggio è marcato come SOS usiamo il canale ad alta importanza,
        // altrimenti quello generale.
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
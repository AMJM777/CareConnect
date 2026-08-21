package com.careconnect.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.careconnect.R
import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import com.careconnect.MainActivity

// helper unico per mostrare le notifiche dell'app: usato sia dai task in
// background (WorkManager) sia dalle notifiche push (FCM), così il
// comportamento resta coerente in tutta l'app.
object NotificationHelper {

    // canale "generale": notifiche informative a importanza normale
    const val CANALE_GENERALE_ID = "careconnect_generale"
    private const val CANALE_GENERALE_NOME = "Notifiche generali"

    // canale "SOS": notifiche ad alta importanza per le emergenze
    const val CANALE_SOS_ID = "careconnect_sos"
    private const val CANALE_SOS_NOME = "Emergenze SOS"
    // id della notifica full-screen di conferma SOS: la crea il Service,
    // la rimuove l'Activity appena si apre. Condiviso per non duplicare il numero
    const val ID_NOTIFICA_CONFERMA = 4202

    // canale "servizio": bassa importanza, per la notifica permanente del
    // Foreground Service che tiene attivo lo scuotimento ad app chiusa
    const val CANALE_SERVIZIO_ID = "careconnect_servizio_sos"
    private const val CANALE_SERVIZIO_NOME = "Protezione SOS in background"

    // funzione per creare i canali di notifica dell'app
    fun creaCanali(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java)

            val canaleGenerale = NotificationChannel(
                CANALE_GENERALE_ID,
                CANALE_GENERALE_NOME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Aggiornamenti sulle richieste di aiuto"
            }
            manager.createNotificationChannel(canaleGenerale)

            // alta importanza: canale separato così
            // non può essere silenziato insieme alle notifiche ordinarie.
            val canaleSos = NotificationChannel(
                CANALE_SOS_ID,
                CANALE_SOS_NOME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Allarmi SOS inviati dall'anziano che assisti"
            }
            manager.createNotificationChannel(canaleSos)

            // bassa importanza: la notifica è sempre visibile ma non fa suonare o vibrare il cel
            // IMPORTANCE_LOW (non MIN) cosi non viene nascosta del tutto dal sistema
            val canaleServizio = NotificationChannel(
                CANALE_SERVIZIO_ID,
                CANALE_SERVIZIO_NOME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notifica sempre presente mentre la protezione SOS è attiva"
                setShowBadge(false)
            }
            manager.createNotificationChannel(canaleServizio)
        }
    }

    // funzione per mostrare una notifica.
    // notificaId: notifiche con id diverso convivono, con lo stesso id si
    // sovrascrivono (utile per aggiornarle invece di accumularle).

    fun mostraNotifica(
        context: Context,
        canaleId: String,
        titolo: String,
        testo: String,
        notificaId: Int
    ) {
        // si assicura che il canale esista anche se l'app è appena partita in background.
        creaCanali(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val concesso = ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!concesso) return
        }

        val notifica = NotificationCompat.Builder(context, canaleId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(titolo)
            .setContentText(testo)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(notificaId, notifica)
    }

    // costruisce la notifica permanente del Foreground Service
    // Toccandola si apre l'app
    fun costruisciNotificaServizio(context: Context): Notification {
        // si assicura che il canale esista anche se il service parte per primo
        creaCanali(context)

        // tocco sulla notifica -> apre la Home dell'app
        val intentApp = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingApp = PendingIntent.getActivity(
            context,
            0,
            intentApp,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(context, CANALE_SERVIZIO_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Protezione SOS attiva")
            .setContentText("Scuoti il telefono per chiedere aiuto, anche con l'app chiusa.")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setOngoing(true)          // l'utente non può scartarla mentre il service è attivo
            .setContentIntent(pendingApp)
            .build()
    }
}
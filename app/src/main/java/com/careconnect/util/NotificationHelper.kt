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
}
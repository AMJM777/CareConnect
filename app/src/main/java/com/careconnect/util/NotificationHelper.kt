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

/**
 * Helper unico per mostrare le notifiche dell'app (FASE 11 e FASE 12).
 *
 * Perché un helper condiviso: sia i task in background (WorkManager, Fase 11)
 * sia le notifiche push (Firebase Cloud Messaging, Fase 12) devono mostrare
 * una notifica allo stesso modo. Concentrare qui la logica evita di duplicarla
 * e rende il comportamento coerente in tutta l'app.
 *
 * È un "object" (singleton): non ha stato, quindi una sola istanza va benissimo.
 */
object NotificationHelper {

    // Canale "generale": notifiche informative a importanza normale
    // (es. "ci sono nuove richieste disponibili" per il Volontario, Fase 11).
    // In FASE 12 aggiungeremo un secondo canale dedicato all'SOS, ad alta importanza.
    const val CANALE_GENERALE_ID = "careconnect_generale"
    private const val CANALE_GENERALE_NOME = "Notifiche generali"

    const val CANALE_SOS_ID = "careconnect_sos"
    private const val CANALE_SOS_NOME = "Emergenze SOS"
    /**
     * Crea i canali di notifica dell'app.
     * Da Android 8 (API 26) OGNI notifica deve appartenere a un canale.
     * Il minSdk del progetto è già 26, quindi la condizione qui sotto è sempre
     * vera nel nostro caso: la lasciamo per correttezza e perché è il pattern
     * mostrato a lezione. Ricrearlo è innocuo (operazione idempotente).
     */
    fun creaCanali(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java)

            // Canale generale: notifiche informative a importanza normale
            // (es. nuove richieste per il volontario, richieste da confermare).
            val canaleGenerale = NotificationChannel(
                CANALE_GENERALE_ID,
                CANALE_GENERALE_NOME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Aggiornamenti sulle richieste di aiuto"
            }
            manager.createNotificationChannel(canaleGenerale)

            // Canale SOS: ALTA importanza (suono + banner heads-up) perché è
            // un'emergenza. Canale separato così non può essere silenziato
            // insieme alle notifiche ordinarie.
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
    /**
     * Mostra una notifica.
     *
     * @param canaleId   quale canale usare (per ora sempre CANALE_GENERALE_ID)
     * @param notificaId id numerico: notifiche con id diverso convivono,
     *                   notifiche con lo stesso id si sovrascrivono (utile per
     *                   aggiornare una notifica esistente invece di accumularle).
     */
    fun mostraNotifica(
        context: Context,
        canaleId: String,
        titolo: String,
        testo: String,
        notificaId: Int
    ) {
        // Ci assicuriamo che il canale esista, anche se l'app è appena partita
        // in background (Worker/servizio FCM) senza passare da MainActivity.
        creaCanali(context)

        // Da Android 13 (API 33) mostrare notifiche richiede un permesso runtime.
        // Se l'utente non l'ha concesso, usciamo senza fare nulla: nessun crash.
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
            .setAutoCancel(true) // la notifica sparisce quando l'utente la tocca
            .build()

        NotificationManagerCompat.from(context).notify(notificaId, notifica)
    }
}
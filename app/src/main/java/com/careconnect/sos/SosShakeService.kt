package com.careconnect.sos

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.careconnect.CareConnectApp
import com.careconnect.R
import com.careconnect.ui.anziano.ConfermaSosActivity
import com.careconnect.util.NotificationHelper
import com.careconnect.util.ShakeDetector
import android.app.PendingIntent

// servizio che lavora in background per tenere attivo il sensore di movimento
// se il telefono viene scosso apre la schermata per confermare l'sos
class SosShakeService : Service() {

    private lateinit var shakeDetector: ShakeDetector

    // serve a non attivare il sensore due volte per errore
    private var attivo = false

    override fun onCreate() {
        super.onCreate()
        // stesso rilevatore usato ad app aperta
        shakeDetector = ShakeDetector(this) { onScuotimentoRilevato() }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        //controlla quale comando è arrivato dall'esterno
        when (intent?.action) {
            ACTION_STOP -> {
                fermaService()
                return START_NOT_STICKY
            }
            else -> avviaService()
        }
        // START_STICKY: se il sistema uccide il service, prova a riavviarlo
        return START_STICKY
    }

    // porta il service in foreground (notifica permanente) e attiva il sensore
    private fun avviaService() {
        if (attivo) return
        val notifica = NotificationHelper.costruisciNotificaServizio(this)
        startForeground(ID_NOTIFICA_SERVIZIO, notifica)
        shakeDetector.avvia()
        attivo = true
    }

    //spegne il sensore e rimuove la notifica fissa
    private fun fermaService() {
        shakeDetector.ferma()
        attivo = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    // funzione che decide come viene mostrato l'allarme quando il telefono viene scosso
    private fun onScuotimentoRilevato() {
        // app aperta o ha permessi speciallii -> apre la schermata
        if (CareConnectApp.inPrimoPiano || Settings.canDrawOverlays(this)) {
            apriConfermaDiretta()
        } else {
            //altrimenti usa una notifica gigante a schermo intero
            mostraConfermaFullScreen()
        }
    }

    // apertura diretta dell'overlay di conferma di aiuto
    private fun apriConfermaDiretta() {
        val intent = Intent(this, ConfermaSosActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(intent)
    }

    // app chiusa/bloccata: notifica ad alta importanza con full-screen intent
    private fun mostraConfermaFullScreen() {
        NotificationHelper.creaCanali(this)

        val intentConferma = Intent(this, ConfermaSosActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val pendingConferma = PendingIntent.getActivity(
            this,
            0,
            intentConferma,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notifica = NotificationCompat.Builder(this, NotificationHelper.CANALE_SOS_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Confermi la richiesta di aiuto?")
            .setContentText("Tocca per aprire. Parte tra pochi secondi.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setAutoCancel(true)
            .setFullScreenIntent(pendingConferma, true)
            .build()

        NotificationManagerCompat.from(this).notify(NotificationHelper.ID_NOTIFICA_CONFERMA, notifica)
    }

    override fun onDestroy() {
        super.onDestroy()
        // fa pulizia e spegne il sensore quando il servizio viene distrutto
        shakeDetector.ferma()
        attivo = false
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // comandi rapidi per accendere o spegnere questo servizio da altre schermate
    companion object {
        private const val ID_NOTIFICA_SERVIZIO = 4201

        const val ACTION_START = "com.careconnect.sos.START"
        const val ACTION_STOP = "com.careconnect.sos.STOP"

        // avvia il service in foreground (dal profilo o all'apertura dell'app)
        fun avvia(context: Context) {
            val intent = Intent(context, SosShakeService::class.java).apply {
                action = ACTION_START
            }
            ContextCompat.startForegroundService(context, intent)
        }

        // ferma il service (toggle "disattiva" presente nel profilo)
        fun ferma(context: Context) {
            val intent = Intent(context, SosShakeService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
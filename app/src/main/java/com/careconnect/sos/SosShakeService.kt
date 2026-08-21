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

// Foreground Service che tiene attivo l'accelerometro anche ad app chiusa
// Usa lo ShakeDetector e alla rilevazione di uno scuotimento apre la
// conferma SOS
class SosShakeService : Service() {

    private lateinit var shakeDetector: ShakeDetector

    // evita di registrare due volte il sensore se arrivano più START
    private var attivo = false

    override fun onCreate() {
        super.onCreate()
        // stesso rilevatore usato ad app aperta
        shakeDetector = ShakeDetector(this) { onScuotimentoRilevato() }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // un intent null arriva quando il sistema riavvia il service (START_STICKY):
        // in quel caso vogliamo comunque riattivare la protezione -> ramo "else"
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
    // Il tipo del foreground service ("specialUse") viene preso dal manifest
    private fun avviaService() {
        if (attivo) return
        val notifica = NotificationHelper.costruisciNotificaServizio(this)
        startForeground(ID_NOTIFICA_SERVIZIO, notifica)
        shakeDetector.avvia()
        attivo = true
    }

    private fun fermaService() {
        shakeDetector.ferma()
        attivo = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    // scuotimento rilevato. Apro sempre l'overlay direttamente quando posso:
    // - app in primo piano: un'app in foreground puo' lanciare Activity liberamente;
    // - permesso "compari sopra le altre app" concesso: autorizza l'avvio di Activity
    //   dal background, quindi l'overlay si apre anche sulla home del telefono.
    // Solo se NON ho nessuna delle due, ripiego sulla notifica full-screen (che
    // Android apre da sola a schermo bloccato).
    private fun onScuotimentoRilevato() {
        if (CareConnectApp.inPrimoPiano || Settings.canDrawOverlays(this)) {
            apriConfermaDiretta()
        } else {
            mostraConfermaFullScreen()
        }
    }

    // apertura diretta dell'overlay di conferma
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
        shakeDetector.ferma()
        attivo = false
    }

    // non è un bound service
    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val ID_NOTIFICA_SERVIZIO = 4201

        const val ACTION_START = "com.careconnect.sos.START"
        const val ACTION_STOP = "com.careconnect.sos.STOP"

        // avvia il service in foreground (dal Profilo o all'apertura dell'app)
        fun avvia(context: Context) {
            val intent = Intent(context, SosShakeService::class.java).apply {
                action = ACTION_START
            }
            ContextCompat.startForegroundService(context, intent)
        }

        // ferma il service (toggle "disattiva" nel Profilo)
        fun ferma(context: Context) {
            val intent = Intent(context, SosShakeService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
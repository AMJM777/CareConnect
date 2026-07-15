package com.careconnect.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

// pianificazione dei task in background di volontario e familiare, così i
// Fragment chiamano un metodo dal nome chiaro senza conoscere i dettagli di WorkManager.
object WorkScheduler {

    private const val NOME_LAVORO_PERIODICO = "controllo_richieste_periodico"
    private const val NOME_LAVORO_DEMO = "controllo_richieste_demo"

    // il Worker interroga Firestore: senza rete non ha senso eseguirlo
    private val vincoloRete = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    private const val NOME_LAVORO_CONFERME_PERIODICO = "controllo_conferme_periodico"
    private const val NOME_LAVORO_CONFERME_DEMO = "controllo_conferme_demo"

    // funzione per pianificare il controllo periodico delle nuove richieste
    // (15 minuti, minimo consentito da WorkManager). KEEP: nessun doppione se già attivo
    fun pianificaControlloPeriodico(context: Context) {
        val richiesta = PeriodicWorkRequestBuilder<ControlloRichiesteWorker>(
            15, TimeUnit.MINUTES
        )
            .setConstraints(vincoloRete)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            NOME_LAVORO_PERIODICO,
            ExistingPeriodicWorkPolicy.KEEP,
            richiesta
        )
    }

    // funzione che esegue subito, una volta sola, lo stesso Worker: serve
    // per la demo, dato che il lavoro periodico ha un intervallo minimo di 15 minuti
    fun eseguiOraPerDemo(context: Context) {
        val richiesta = OneTimeWorkRequestBuilder<ControlloRichiesteWorker>()
            .setConstraints(vincoloRete)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            NOME_LAVORO_DEMO,
            ExistingWorkPolicy.REPLACE,
            richiesta
        )
    }

    // funzione per pianificare il controllo periodico delle richieste da
    // confermare, per il familiare (stessa struttura del controllo del volontario)
    fun pianificaControlloConfermePeriodico(context: Context) {
        val richiesta = PeriodicWorkRequestBuilder<ControlloConfermeFamiliareWorker>(
            15, TimeUnit.MINUTES
        )
            .setConstraints(vincoloRete)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            NOME_LAVORO_CONFERME_PERIODICO,
            ExistingPeriodicWorkPolicy.KEEP,
            richiesta
        )
    }

    // funzione che esegue subito, una volta sola, il controllo delle richieste da confermare: solo per la presentazione
    fun eseguiControlloConfermeOraPerDemo(context: Context) {
        val richiesta = OneTimeWorkRequestBuilder<ControlloConfermeFamiliareWorker>()
            .setConstraints(vincoloRete)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            NOME_LAVORO_CONFERME_DEMO,
            ExistingWorkPolicy.REPLACE,
            richiesta
        )
    }
}
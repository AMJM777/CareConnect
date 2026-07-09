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

/**
 * FASE 11 — Pianificazione del task in background del volontario.
 *
 * Concentra qui la logica di scheduling di WorkManager, così i Fragment
 * chiamano un metodo dal nome chiaro e non devono conoscere i dettagli di
 * PeriodicWorkRequest / OneTimeWorkRequest.
 */
object WorkScheduler {

    private const val NOME_LAVORO_PERIODICO = "controllo_richieste_periodico"
    private const val NOME_LAVORO_DEMO = "controllo_richieste_demo"

    // Il Worker interroga Firestore: senza rete non ha senso eseguirlo.
    private val vincoloRete = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    private const val NOME_LAVORO_CONFERME_PERIODICO = "controllo_conferme_periodico"
    private const val NOME_LAVORO_CONFERME_DEMO = "controllo_conferme_demo"

    /**
     * Pianifica il controllo periodico delle nuove richieste (comportamento reale).
     * 15 minuti è l'intervallo minimo consentito da WorkManager per il lavoro periodico.
     *
     * Policy KEEP: se il lavoro è già pianificato (es. il volontario riapre la
     * home), NON lo ripianifichiamo, evitando doppioni.
     */
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

    /**
     * Esegue SUBITO il controllo una volta sola. Serve per la DEMO: il lavoro
     * periodico ha intervallo minimo 15 minuti, troppo per mostrarlo dal vivo.
     * Fa girare lo stesso identico Worker immediatamente.
     */
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

    /**
     * FASE 11b — Pianifica il controllo periodico delle richieste da confermare,
     * per il FAMILIARE. Stessa struttura del controllo del volontario: 15 min,
     * vincolo di rete, policy KEEP per non creare doppioni.
     */
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

    /**
     * FASE 11b (DEMO) — Esegue subito, una volta sola, il controllo delle
     * richieste da confermare del familiare. Come per il volontario, serve a
     * mostrare il task all'orale senza aspettare l'intervallo di 15 minuti.
     */
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
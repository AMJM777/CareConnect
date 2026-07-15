package com.careconnect.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.careconnect.model.UserRole
import com.careconnect.repository.RequestRepositoryImpl
import com.careconnect.util.NotificationHelper
import com.careconnect.util.SessionCache
import com.google.firebase.auth.FirebaseAuth

/**
 * task in background per il volontario: controlla periodicamente se sono
 * comparse nuove richieste aperte e mostra una notifica
 * CoroutineWorker (non Worker base) perché i repository sono `suspend`:
 * si possono chiamare direttamente in doWork() senza bloccare un thread
 */
class ControlloRichiesteWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val requestRepository = RequestRepositoryImpl()
    private val sessionCache = SessionCache(applicationContext)

    override suspend fun doWork(): Result {
        // ha senso solo per un volontario loggato
        FirebaseAuth.getInstance().currentUser ?: return Result.success()
        if (sessionCache.getRuoloSalvato() != UserRole.VOLONTARIO) return Result.success()

        val richieste = requestRepository.getRichiesteAperte().getOrElse {
            return Result.retry()
        }

        // tiene solo le richieste create dopo l'ultimo controllo, per non
        // rinotificare sempre le stesse
        val ultimoControllo = leggiUltimoControllo()
        val nuove = richieste.filter { it.timestampCreazione.toDate().time > ultimoControllo }

        if (nuove.isNotEmpty()) {
            val testo = if (nuove.size == 1) {
                "C'è una nuova richiesta di aiuto disponibile"
            } else {
                "Ci sono ${nuove.size} nuove richieste di aiuto disponibili"
            }
            NotificationHelper.mostraNotifica(
                context = applicationContext,
                canaleId = NotificationHelper.CANALE_GENERALE_ID,
                titolo = "CareConnect",
                testo = testo,
                notificaId = ID_NOTIFICA_RICHIESTE
            )
            salvaUltimoControllo(System.currentTimeMillis())
        }

        return Result.success()
    }

    //  "segno" locale dell'ultimo controllo con notifica inviata
    private fun leggiUltimoControllo(): Long =
        prefs().getLong(CHIAVE_ULTIMO_CONTROLLO, 0L)

    private fun salvaUltimoControllo(millis: Long) {
        prefs().edit().putLong(CHIAVE_ULTIMO_CONTROLLO, millis).apply()
    }

    private fun prefs() =
        applicationContext.getSharedPreferences(PREFS_WORKER, Context.MODE_PRIVATE)

    private companion object {
        const val PREFS_WORKER = "careconnect_worker"
        const val CHIAVE_ULTIMO_CONTROLLO = "ultimo_controllo_richieste"
        const val ID_NOTIFICA_RICHIESTE = 1001
    }
}
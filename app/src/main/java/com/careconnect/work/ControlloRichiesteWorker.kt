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
 * FASE 11 — Task in background per il VOLONTARIO.
 *
 * Controlla periodicamente se sono comparse NUOVE richieste di aiuto aperte
 * e, in caso, mostra una notifica. Così il volontario viene avvisato anche
 * ad app chiusa, senza dover tenere aperta la lista delle richieste.
 *
 * Perché CoroutineWorker e non Worker (quello mostrato a lezione):
 * i nostri repository espongono funzioni `suspend`. CoroutineWorker permette
 * di chiamarle direttamente dentro doWork() senza bloccare un thread con
 * runBlocking. È comunque WorkManager: cambia solo che doWork() è `suspend`.
 */
class ControlloRichiesteWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    // Il Worker può girare ad app chiusa: creiamo qui le dipendenze con i
    // costruttori di default già usati nel resto del progetto (nessun DI container).
    private val requestRepository = RequestRepositoryImpl()
    private val sessionCache = SessionCache(applicationContext)

    override suspend fun doWork(): Result {
        // 1) Il task ha senso solo per un VOLONTARIO loggato. Se non c'è sessione
        //    o il ruolo non è volontario, non c'è lavoro: usciamo con success
        //    (non è un errore, semplicemente non dobbiamo fare nulla).
        FirebaseAuth.getInstance().currentUser ?: return Result.success()
        if (sessionCache.getRuoloSalvato() != UserRole.VOLONTARIO) return Result.success()

        // 2) Leggiamo le richieste aperte con una query singola.
        val richieste = requestRepository.getRichiesteAperte().getOrElse {
            // Errore di rete/Firestore: chiediamo a WorkManager di riprovare più tardi.
            return Result.retry()
        }

        // 3) Teniamo solo le richieste "nuove", create DOPO l'ultimo controllo.
        //    Senza questo confronto il volontario riceverebbe la stessa notifica
        //    a ogni esecuzione, anche per richieste già viste: sarebbe spam.
        val ultimoControllo = leggiUltimoControllo()
        val nuove = richieste.filter { it.timestampCreazione.toDate().time > ultimoControllo }

        // 4) Se ci sono richieste nuove, notifichiamo e spostiamo in avanti il
        //    "segnalibro" temporale, così la prossima volta non le riconteremo.
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

    // --- "Segnalibro" locale: istante dell'ultimo controllo con notifica inviata.
    //     Vive in uno SharedPreferences dedicato al Worker perché è stato locale
    //     del task, non riguarda il ruolo (SessionCache) né Firestore.

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
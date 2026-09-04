package com.careconnect.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.careconnect.model.RequestStatus
import com.careconnect.model.UserRole
import com.careconnect.repository.RequestRepositoryImpl
import com.careconnect.repository.UserRepositoryImpl
import com.careconnect.util.NotificationHelper
import com.careconnect.util.SessionCache
import com.google.firebase.auth.FirebaseAuth

/**
 * task in background per il familiare: avvisa quando c'è almeno una
 * richiesta del suo assistito completata dal volontario e in attesa di
 * conferma. L'evento è un cambio di stato, quindi tiene l'insieme degli ID
 * già notificati invece di un timestamp
 */
class ControlloConfermeFamiliareWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val requestRepository = RequestRepositoryImpl()
    private val userRepository = UserRepositoryImpl()
    private val sessionCache = SessionCache(applicationContext)

    override suspend fun doWork(): Result {
        val utente = FirebaseAuth.getInstance().currentUser ?: return Result.success()
        if (sessionCache.getRuoloSalvato() != UserRole.FAMILIARE) return Result.success()

        val familiare = userRepository.getUtente(utente.uid).getOrElse {
            return Result.retry()
        }
        val anzianiIds = familiare.anzianiCollegatiIds
        if (anzianiIds.isEmpty()) return Result.success()

        // raccoglie le richieste da confermare di tutti gli anziani seguiti
        val daConfermare = mutableSetOf<String>()
        for (anzianoId in anzianiIds) {
            val richieste = requestRepository.getRichiestePerAnziano(anzianoId).getOrElse {
                return Result.retry()
            }
            richieste
                .filter { it.stato == RequestStatus.COMPLETATA_DAL_VOLONTARIO }
                .forEach { daConfermare.add(it.id) }
        }

        val giaNotificate = leggiIdNotificati()
        val nuove = daConfermare - giaNotificate

        if (nuove.isNotEmpty()) {
            val testo = if (nuove.size == 1) {
                "Hai una richiesta completata da confermare"
            } else {
                "Hai ${nuove.size} richieste completate da confermare"
            }
            NotificationHelper.mostraNotifica(
                context = applicationContext,
                canaleId = NotificationHelper.CANALE_GENERALE_ID,
                titolo = "CareConnect",
                testo = testo,
                notificaId = ID_NOTIFICA_CONFERME
            )
        }

        // aggiorna l'insieme salvato: le richieste confermate spariscono da
        // sole, quelle già viste non vengono rinotificate
        salvaIdNotificati(daConfermare)

        return Result.success()
    }

    private fun leggiIdNotificati(): Set<String> =
        prefs().getStringSet(CHIAVE_ID_NOTIFICATI, emptySet()) ?: emptySet()

    private fun salvaIdNotificati(ids: Set<String>) {
        prefs().edit().putStringSet(CHIAVE_ID_NOTIFICATI, HashSet(ids)).apply()
    }

    private fun prefs() =
        applicationContext.getSharedPreferences(PREFS_WORKER, Context.MODE_PRIVATE)

    private companion object {
        const val PREFS_WORKER = "careconnect_worker"
        const val CHIAVE_ID_NOTIFICATI = "conferme_gia_notificate"
        const val ID_NOTIFICA_CONFERME = 1002
    }
}
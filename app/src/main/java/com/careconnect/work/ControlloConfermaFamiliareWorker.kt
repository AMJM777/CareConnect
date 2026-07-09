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
 * FASE 11b — Task in background per il FAMILIARE / garante.
 *
 * Avvisa il familiare quando c'è almeno una richiesta del suo assistito che
 * il volontario ha segnato come completata e che aspetta la SUA conferma
 * (stato COMPLETATA_DAL_VOLONTARIO). È un promemoria "azionabile": notifichiamo
 * solo quando c'è qualcosa che richiede davvero un'azione del familiare.
 *
 * Differenza chiave rispetto al Worker del volontario: lì l'evento che conta è
 * la CREAZIONE di una richiesta (confronto di timestamp). Qui l'evento è un
 * CAMBIO DI STATO, per cui non esiste un timestamp adatto: teniamo invece
 * l'insieme degli ID già notificati e avvisiamo solo per quelli nuovi.
 */
class ControlloConfermeFamiliareWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val requestRepository = RequestRepositoryImpl()
    private val userRepository = UserRepositoryImpl()
    private val sessionCache = SessionCache(applicationContext)

    override suspend fun doWork(): Result {
        // 1) Ha senso solo per un FAMILIARE loggato.
        val utente = FirebaseAuth.getInstance().currentUser ?: return Result.success()
        if (sessionCache.getRuoloSalvato() != UserRole.FAMILIARE) return Result.success()

        // 2) Scopriamo quale anziano segue questo familiare.
        val familiare = userRepository.getUtente(utente.uid).getOrElse {
            return Result.retry() // errore di rete: riprova più tardi
        }
        val anzianoId = familiare.anzianoCollegatoId ?: return Result.success() // non ancora collegato

        // 3) Richieste dell'assistito in attesa di conferma del garante.
        val richieste = requestRepository.getRichiestePerAnziano(anzianoId).getOrElse {
            return Result.retry()
        }
        val daConfermare = richieste
            .filter { it.stato == RequestStatus.COMPLETATA_DAL_VOLONTARIO }
            .map { it.id }
            .toSet()

        // 4) Notifichiamo solo le richieste NON ancora segnalate in precedenza.
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

        // 5) Aggiorniamo l'insieme salvato a quello attuale: le richieste confermate
        //    (uscite dallo stato) spariscono da sole, e non rinotifichiamo quelle
        //    ancora in attesa che il familiare ha già visto una volta.
        salvaIdNotificati(daConfermare)

        return Result.success()
    }

    // --- Insieme locale degli ID già notificati (stato locale del task). ---

    private fun leggiIdNotificati(): Set<String> =
        prefs().getStringSet(CHIAVE_ID_NOTIFICATI, emptySet()) ?: emptySet()

    private fun salvaIdNotificati(ids: Set<String>) {
        // Passiamo una NUOVA collezione (HashSet): a SharedPreferences non va dato
        // lo stesso Set che potrebbe poi essere modificato altrove.
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
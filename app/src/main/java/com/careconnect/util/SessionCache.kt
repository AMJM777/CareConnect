package com.careconnect.util

import android.content.Context
import com.careconnect.model.UserRole

/**
 * cache locale (SharedPreferences) del ruolo dell'utente loggato.
 * FirebaseAuth.currentUser dice solo se esiste una sessione valida, non il
 * ruolo (che sta su Firestore)
 */
class SessionCache(context: Context) {

    // applicationContext per non tenere in memoria un riferimento a
    // un'Activity/Fragment che verrebbe distrutta prima di questa classe.
    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // funzione per salvare il ruolo dopo un login o una registrazione riusciti.
    fun salvaRuolo(ruolo: UserRole) {
        prefs.edit()
            .putString(CHIAVE_RUOLO, ruolo.firestoreValue)
            .apply()
    }

    // funzione per leggere il ruolo salvato. null se non c'è mai stato
    // salvato nulla o se il valore non è più valido.
    fun getRuoloSalvato(): UserRole? {
        val valoreSalvato = prefs.getString(CHIAVE_RUOLO, null) ?: return null
        return try {
            UserRole.fromFirestoreValue(valoreSalvato)
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    // funzione per cancellare il ruolo salvato. da chiamare al logout.
    fun pulisci() {
        prefs.edit()
            .remove(CHIAVE_RUOLO)
            .apply()
    }

    private companion object {
        const val PREFS_NAME = "careconnect_session"
        const val CHIAVE_RUOLO = "ruolo_utente"
    }
}
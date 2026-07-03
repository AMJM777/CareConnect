package com.careconnect.util

import android.content.Context
import com.careconnect.model.UserRole

/**
 * Cache locale (SharedPreferences) del ruolo dell'utente loggato.
 *
 * Perché serve: FirebaseAuth.currentUser ci dice SOLO se esiste una sessione
 * valida, non il ruolo (anziano/volontario/familiare), che sta su Firestore.
 * Senza questa cache, ogni riavvio dell'app richiederebbe una query di rete
 * a Firestore solo per sapere quale home mostrare — lento e inutile offline,
 * dato che il ruolo non cambia mai dopo la registrazione.
 *
 * Non è un repository (non parla con Firestore): è un util di persistenza
 * locale, per questo vive nel package `util` e non in `repository`.
 */
class SessionCache(context: Context) {

    // applicationContext per evitare di tenere in memoria un riferimento
    // a un'Activity/Fragment (che verrebbe distrutta prima di questa classe).
    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Salva il ruolo dopo un login o una registrazione riusciti. */
    fun salvaRuolo(ruolo: UserRole) {
        prefs.edit()
            .putString(CHIAVE_RUOLO, ruolo.firestoreValue)
            .apply()
    }

    /**
     * Legge il ruolo salvato, se presente.
     * Ritorna null se non è mai stato salvato nulla (es. prima installazione,
     * dati app cancellati) oppure se il valore salvato non è più un ruolo
     * valido: in quel caso chi chiama deve ricadere sulla query a Firestore.
     */
    fun getRuoloSalvato(): UserRole? {
        val valoreSalvato = prefs.getString(CHIAVE_RUOLO, null) ?: return null
        return try {
            UserRole.fromFirestoreValue(valoreSalvato)
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    /** Cancella il ruolo salvato. Da chiamare al logout. */
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
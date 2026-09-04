package com.careconnect.util

import android.content.Context

// preferenza locale (SharedPreferences) per la protezione SOS in background.
// modello opt-out: di default è attiva, l'anziano può disattivarla dal profilo
class ProtezioneSosPrefs(context: Context) {

    // applicationContext così non trattengo Activity o Fragment ed evito memory leak
    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isAttiva(): Boolean = prefs.getBoolean(CHIAVE_ATTIVA, DEFAULT_ATTIVA)

    fun setAttiva(attiva: Boolean) {
        prefs.edit().putBoolean(CHIAVE_ATTIVA, attiva).apply()
    }

    // ricorda se abbiamo già mandato l'utente a concedere il permesso overlay,
    // così lo chiediamo una volta sola e non a ogni apertura della home
    fun permessoFullScreenGiaChiesto(): Boolean =
        prefs.getBoolean(CHIAVE_PERMESSO_CHIESTO, false)

    fun segnaPermessoFullScreenChiesto() {
        prefs.edit().putBoolean(CHIAVE_PERMESSO_CHIESTO, true).apply()
    }

    private companion object {
        const val PREFS_NAME = "careconnect_protezione_sos"
        const val CHIAVE_ATTIVA = "protezione_attiva"
        const val CHIAVE_PERMESSO_CHIESTO = "permesso_fullscreen_chiesto"
        const val DEFAULT_ATTIVA = true
    }
}

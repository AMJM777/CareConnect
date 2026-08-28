package com.careconnect.util

import android.content.Context

// Preferenza locale (SharedPreferences) per la protezione SOS in background (T4).
// Modello "opt-out": di default e' ATTIVA. L'anziano puo' disattivarla dal Profilo.
class ProtezioneSosPrefs(context: Context) {

    // applicationContext: non trattiene Activity/Fragment (evita memory leak)
    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isAttiva(): Boolean = prefs.getBoolean(CHIAVE_ATTIVA, DEFAULT_ATTIVA)

    fun setAttiva(attiva: Boolean) {
        prefs.edit().putBoolean(CHIAVE_ATTIVA, attiva).apply()
    }

    // ricorda se abbiamo già mandato l'utente a concedere il permesso full-screen:
    // così non glielo chiediamo a ogni apertura della Home (lo chiediamo una volta).
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

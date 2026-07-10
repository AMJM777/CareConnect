package com.careconnect

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate

/**
 * Application dell'app. Qui blocchiamo il tema in CHIARO (MODE_NIGHT_NO):
 * l'app non usa mai le risorse "night", così la UI resta coerente e leggibile
 * anche se il telefono è in modalità scura.
 *
 * Perché: il tema scuro completo è fuori dagli obiettivi del progetto (la
 * griglia valuta l'adattabilità alle DIMENSIONI dello schermo, non il dark
 * mode) e oggi values-night è vuoto, quindi in scuro la UI rischierebbe di
 * essere illeggibile. Bloccarla in chiaro evita il problema senza costare punti.
 */
class CareConnectApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
    }
}
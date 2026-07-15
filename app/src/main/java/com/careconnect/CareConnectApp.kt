package com.careconnect

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate

// blocca l'app sul tema in chiaro: values-night è vuoto, quindi in scuro
// la UI sarebbe illeggibile
class CareConnectApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
    }
}
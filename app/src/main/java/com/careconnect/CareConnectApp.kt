package com.careconnect

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.appcompat.app.AppCompatDelegate

// blocca l'app sul tema in chiaro: values-night è vuoto, quindi in scuro
// la UI sarebbe illeggibile.
// Tiene anche traccia se l'app è in primo piano: serve al SosShakeService (T4)
// per decidere se aprire la conferma direttamente (app aperta) o via notifica
// full-screen (app chiusa/bloccata).
class CareConnectApp : Application() {

    override fun onCreate() {
        super.onCreate()
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        registerActivityLifecycleCallbacks(TracciaPrimoPiano())
    }

    // conta le Activity avviate: se sono più di zero, l'app è in primo piano
    private class TracciaPrimoPiano : ActivityLifecycleCallbacks {
        private var avviate = 0

        override fun onActivityStarted(activity: Activity) {
            avviate++
            inPrimoPiano = avviate > 0
        }

        override fun onActivityStopped(activity: Activity) {
            avviate--
            inPrimoPiano = avviate > 0
        }

        // callback non usati: richiesti dall'interfaccia
        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
        override fun onActivityResumed(activity: Activity) {}
        override fun onActivityPaused(activity: Activity) {}
        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
        override fun onActivityDestroyed(activity: Activity) {}
    }

    companion object {
        // true quando almeno una Activity dell'app è in primo piano
        @Volatile
        var inPrimoPiano: Boolean = false
            private set
    }
}
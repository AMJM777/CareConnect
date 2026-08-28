package com.careconnect.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

// Gestisce il permesso "Compari sopra le altre app" (SYSTEM_ALERT_WINDOW).
// Con questo permesso la protezione SOS (T4) puo' aprire l'overlay di conferma
// DIRETTAMENTE in ogni situazione (app aperta, home del telefono, schermo
// bloccato), senza una notifica intermedia da toccare: il permesso autorizza
// l'avvio di una Activity dal background, altrimenti bloccato su Android moderno.
//
// NB: file ancora chiamato FullScreenSosPermesso.kt per un limite tecnico di
// questa sessione; rinominarlo in OverlaySosPermesso.kt in Android Studio.
object OverlaySosPermesso {

    // true se possiamo aprire l'overlay senza tap dell'utente (permesso concesso)
    fun concesso(context: Context): Boolean = Settings.canDrawOverlays(context)

    // porta l'utente alla schermata di sistema dove concedere il permesso, una volta
    fun intentImpostazioni(context: Context): Intent =
        Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        )
}

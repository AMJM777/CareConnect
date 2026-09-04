package com.careconnect.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

// gestisce il permesso "Compari sopra le altre app" (SYSTEM_ALERT_WINDOW);
// con questo permesso la protezione SOS può aprire l'overlay di conferma
// direttamente in ogni situazione (app aperta, home, schermo bloccato) senza
// una notifica intermedia, perché autorizza l'avvio di una Activity dal
// background, altrimenti bloccato su Android moderno
object OverlaySosPermesso {

    // true se possiamo aprire l'overlay senza tap dell'utente (permesso concesso)
    fun concesso(context: Context): Boolean = Settings.canDrawOverlays(context)

    // porta l'utente alla schermata di sistema dove concedere il permesso
    fun intentImpostazioni(context: Context): Intent =
        Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        )
}

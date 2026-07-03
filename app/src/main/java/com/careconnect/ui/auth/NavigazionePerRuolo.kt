package com.careconnect.ui.auth

import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import com.careconnect.R
import com.careconnect.model.UserRole

/**
 * Naviga dalla schermata di autenticazione corrente verso la home
 * corretta per il ruolo dell'utente appena autenticato.
 * popUpTo(nav_graph_auth, inclusive = true) rimuove tutto il flusso di
 * login/registrazione dallo stack: da qui in poi il tasto Indietro non
 * deve riportare l'utente al login già completato.
 */
fun Fragment.navigaAllaHomePerRuolo(ruolo: UserRole) {
    val destinazione = when (ruolo) {
        UserRole.ANZIANO -> R.id.homeAnzianoFragment
        UserRole.VOLONTARIO -> R.id.homeVolontarioFragment
        UserRole.FAMILIARE -> R.id.homeFamiliareFragment
    }
    val opzioni = navOptions {
        popUpTo(R.id.nav_graph_auth) { inclusive = true }
    }
    findNavController().navigate(destinazione, null, opzioni)
}
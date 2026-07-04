package com.careconnect.ui.anziano

import android.os.Bundle
import android.view.View
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.navOptions
import com.careconnect.R
import com.google.android.material.bottomnavigation.BottomNavigationView

/**
 * Contenitore della sezione Anziano: ospita il grafo annidato
 * (nav_graph_anziano) e la relativa BottomNavigationView.
 *
 * NON usiamo NavigationUI.setupWithNavController(): il suo comportamento
 * di default (saveState/restoreState) creava un back stack inconsistente
 * insieme al nostro back-handling custom. Colleghiamo i tab a mano.
 */
class HomeAnzianoFragment : Fragment(R.layout.fragment_home_anziano) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val navHostFragment = childFragmentManager
            .findFragmentById(R.id.anzianoNavHostFragment) as NavHostFragment
        val navController = navHostFragment.navController

        collegaBottomNav(view, navController)
        gestisciTastoIndietro(navController)
    }

    private fun collegaBottomNav(view: View, navController: NavController) {
        val bottomNav = view.findViewById<BottomNavigationView>(R.id.anzianoBottomNav)

        bottomNav.setOnItemSelectedListener { item ->
            if (item.itemId == navController.currentDestination?.id) {
                return@setOnItemSelectedListener true
            }

            val opzioni = navOptions {
                popUpTo(navController.graph.startDestinationId)
                launchSingleTop = true
            }
            navController.navigate(item.itemId, null, opzioni)
            true
        }

        // Unico scopo di questo listener: tenere selezionato il tab giusto
        // in UI, anche quando la navigazione avviene per altre vie (es.
        // tasto Indietro). Non gestisce il tasto Indietro stesso.
        navController.addOnDestinationChangedListener { _, destination, _ ->
            bottomNav.menu.findItem(destination.id)?.isChecked = true
        }
    }

    /** TASTO ESC PC
     * Gestione manuale del tasto Indietro: il NavHostFragment annidato non
     * lo intercetta da solo (solo quello principale in activity_main.xml
     * può farlo, con app:defaultNavHost="true"). Il callback resta sempre
     * attivo e controlla la destinazione corrente ogni volta che Indietro
     * viene premuto, senza affidarsi a nessuno stato "cache" esterno.
     */
    private fun gestisciTastoIndietro(navController: NavController) {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            if (navController.currentDestination?.id != R.id.dashboardAnzianoFragment) {
                // Non siamo sulla dashboard: Indietro torna lì.
                navController.popBackStack()
            } else {
                // Siamo già sulla dashboard: lasciamo che Indietro segua il
                // comportamento di sistema (es. uscita dall'app), poi il
                // callback si "riattiva" per la prossima pressione.
                isEnabled = false
                requireActivity().onBackPressedDispatcher.onBackPressed()
                isEnabled = true
            }
        }
    }
}
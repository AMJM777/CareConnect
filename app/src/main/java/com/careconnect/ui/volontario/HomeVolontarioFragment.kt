package com.careconnect.ui.volontario

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
 * Contenitore della sezione Volontario: ospita il grafo annidato
 * (nav_graph_volontario) e la relativa BottomNavigationView.
 *
 * Stesso schema di HomeAnzianoFragment: collegamento bottom nav <-> NavController
 * fatto a mano (non NavigationUI.setupWithNavController()) per lo stesso motivo
 * già documentato lì (back stack inconsistente con saveState/restoreState).
 */
class HomeVolontarioFragment : Fragment(R.layout.fragment_home_volontario) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val navHostFragment = childFragmentManager
            .findFragmentById(R.id.volontarioNavHostFragment) as NavHostFragment
        val navController = navHostFragment.navController

        collegaBottomNav(view, navController)
        gestisciTastoIndietro(navController)
    }

    private fun collegaBottomNav(view: View, navController: NavController) {
        val bottomNav = view.findViewById<BottomNavigationView>(R.id.volontarioBottomNav)

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

        // Tiene selezionato il tab giusto anche quando la navigazione avviene
        // per altre vie (es. tasto Indietro), senza gestire il tasto stesso.
        navController.addOnDestinationChangedListener { _, destination, _ ->
            bottomNav.menu.findItem(destination.id)?.isChecked = true
        }
    }

    /**
     * Gestione manuale del tasto Indietro, stesso schema di HomeAnzianoFragment:
     * se non siamo sulla startDestination (Richieste disponibili), Indietro
     * torna lì; se siamo già lì, lascia il comportamento di sistema.
     */
    private fun gestisciTastoIndietro(navController: NavController) {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            if (navController.currentDestination?.id != R.id.richiesteDisponibiliFragment) {
                navController.popBackStack()
            } else {
                isEnabled = false
                requireActivity().onBackPressedDispatcher.onBackPressed()
                isEnabled = true
            }
        }
    }
}
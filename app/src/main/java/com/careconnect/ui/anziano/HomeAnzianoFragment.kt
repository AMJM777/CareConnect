package com.careconnect.ui.anziano

import android.os.Bundle
import android.view.View
import androidx.activity.addCallback
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.navOptions
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.careconnect.R
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.careconnect.ui.common.nascondiBottomNavQuandoTastieraAperta

// contenitore della sezione Anziano: Toolbar del ruolo + NavHost annidato
// (nav_graph_anziano) + BottomNavigationView
class HomeAnzianoFragment : Fragment(R.layout.fragment_home_anziano) {

    private lateinit var appBarConfiguration: AppBarConfiguration

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val navHostFragment = childFragmentManager
            .findFragmentById(R.id.anzianoNavHostFragment) as NavHostFragment
        val navController = navHostFragment.navController

        // nasconde la bottom nav mentre la tastiera è aperta, altrimenti "salta" sopra di essa
        nascondiBottomNavQuandoTastieraAperta(
            view,
            view.findViewById(R.id.anzianoBottomNav),
            viewLifecycleOwner
        )
        // stessa cosa applicata direttamente alla bottom nav: usa isVisible(ime())
        // e non l'altezza, perché con la finestra ridimensionata l'altezza
        // risulterebbe 0 pur essendo la tastiera aperta
        val bottomNav = view.findViewById<View>(R.id.anzianoBottomNav)
        ViewCompat.setOnApplyWindowInsetsListener(bottomNav) { v, insets ->
            val tastieraAperta = insets.isVisible(WindowInsetsCompat.Type.ime())
            v.visibility = if (tastieraAperta) View.GONE else View.VISIBLE
            insets
        }
        collegaToolbar(view, navController)
        collegaBottomNav(view, navController)
        gestisciTastoIndietro(navController)
    }

    // Collega la Toolbar del ruolo al grafo annidatop
    private fun collegaToolbar(view: View, navController: NavController) {
        val toolbar = view.findViewById<Toolbar>(R.id.anzianoToolbar)

        // solo la Home è "di primo livello": lì la freccia non compare
        appBarConfiguration = AppBarConfiguration(setOf(R.id.nuovaRichiestaHomeFragment))
        toolbar.setupWithNavController(navController, appBarConfiguration)
    }

    // funzione per collegare la BottomNavigationView al grafo di navigazione annidato
    private fun collegaBottomNav(view: View, navController: NavController) {
        val bottomNav = view.findViewById<BottomNavigationView>(R.id.anzianoBottomNav)

        bottomNav.setOnItemSelectedListener { item ->
            // Se tocco il tab su cui sono già, non faccio nulla.
            if (item.itemId == navController.currentDestination?.id) {
                return@setOnItemSelectedListener true
            }
            // popUpTo(start) senza inclusive: lo stack resta [Home, tab scelto],
            // così l'Indietro da un tab secondario riporta sempre alla Home.
            val opzioni = navOptions {
                popUpTo(navController.graph.startDestinationId)
                launchSingleTop = true
            }
            navController.navigate(item.itemId, null, opzioni)
            true
        }

        // tiene evidenziato il tab giusto anche quando la navigazione avviene per altre vie
        navController.addOnDestinationChangedListener { _, destination, _ ->
            bottomNav.menu.findItem(destination.id)?.isChecked = true
        }
    }

    // funzione che gestisce il tasto Indietro di sistema in modo esplicito e prevedibile
    private fun gestisciTastoIndietro(navController: NavController) {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            // popBackStack() restituisce false se non c'è più nulla da togliere.
            val tornatoIndietro = navController.popBackStack()
            if (!tornatoIndietro) {
                // si è sulla Home: si lascia agire il sistema, l'app si chiude
                isEnabled = false
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
        }
    }
}
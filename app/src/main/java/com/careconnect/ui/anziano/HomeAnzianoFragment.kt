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
import androidx.core.view.updatePadding
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
        // bottom nav: nascosta con la tastiera aperta (isVisible(ime()), non l'altezza,
        // che con la finestra ridimensionata risulterebbe 0) + inset basso della barra di sistema
        val bottomNav = view.findViewById<View>(R.id.anzianoBottomNav)
        ViewCompat.setOnApplyWindowInsetsListener(bottomNav) { v, insets ->
            v.visibility = if (insets.isVisible(WindowInsetsCompat.Type.ime())) View.GONE else View.VISIBLE
            v.updatePadding(bottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom)
            insets
        }
        // inset alto applicato alla RADICE (prugna), non alla toolbar: la radice non è
        // gestita da NavigationUI, quindi la barra resta alta uguale su ogni schermata
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            v.updatePadding(top = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top)
            insets
        }
        view.post { ViewCompat.requestApplyInsets(view) }

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
            ViewCompat.requestApplyInsets(view)
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
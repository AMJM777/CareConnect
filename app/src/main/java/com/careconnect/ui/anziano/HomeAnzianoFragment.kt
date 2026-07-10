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

/**
 * Contenitore della sezione Anziano: Toolbar del ruolo + NavHost annidato
 * (nav_graph_anziano) + BottomNavigationView.
 *
 * Gli spazi delle barre di sistema sono gestiti nel layout con
 * fitsSystemWindows="true" (vedi fragment_home_anziano.xml): niente da fare
 * qui a runtime.
 */
class HomeAnzianoFragment : Fragment(R.layout.fragment_home_anziano) {

    private lateinit var appBarConfiguration: AppBarConfiguration

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val navHostFragment = childFragmentManager
            .findFragmentById(R.id.anzianoNavHostFragment) as NavHostFragment
        val navController = navHostFragment.navController

        // Nasconde la bottom nav mentre la tastiera è aperta (altrimenti
        // "salta" sopra la tastiera). Funziona misurando l'area visibile,
        // non gli insets, che qui sono inaffidabili.
        nascondiBottomNavQuandoTastieraAperta(
            view,
            view.findViewById(R.id.anzianoBottomNav),
            viewLifecycleOwner
        )
        // Tastiera + bottom nav: quando la tastiera è aperta nascondiamo la
        // barra in basso (i tab non servono mentre si scrive e altrimenti
        // "saltano" sopra la tastiera). La rimostriamo quando si chiude.
        // Usiamo isVisible(ime()) e NON l'altezza: se la finestra si
        // ridimensiona, l'altezza della tastiera risulta 0 pur essendo aperta,
        // mentre isVisible resta affidabile.
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

    // Collega la Toolbar del ruolo al grafo annidato.
    private fun collegaToolbar(view: View, navController: NavController) {
        val toolbar = view.findViewById<Toolbar>(R.id.anzianoToolbar)

        // Solo la Home (dashboard) è "di primo livello": lì la freccia NON compare.
        appBarConfiguration = AppBarConfiguration(setOf(R.id.dashboardAnzianoFragment))
        toolbar.setupWithNavController(navController, appBarConfiguration)
    }

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

        // Tiene evidenziato il tab giusto anche quando la navigazione avviene
        // per altre vie (es. tasto Indietro). Non gestisce il tasto stesso.
        navController.addOnDestinationChangedListener { _, destination, _ ->
            bottomNav.menu.findItem(destination.id)?.isChecked = true
        }
    }

    // Tasto Indietro di sistema, gestito in modo esplicito e prevedibile.
    private fun gestisciTastoIndietro(navController: NavController) {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            // Provo a tornare indietro nello stack del ruolo (tab secondario o
            // Nuova richiesta -> Home). popBackStack() restituisce false se non
            // c'è più nulla da togliere, cioè se siamo già sulla Home.
            val tornatoIndietro = navController.popBackStack()
            if (!tornatoIndietro) {
                // Siamo sulla Home: disabilito questo callback e lascio agire il
                // sistema. Non essendoci altro nello stack, l'app si chiude.
                isEnabled = false
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
        }
    }
}
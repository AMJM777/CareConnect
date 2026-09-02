package com.careconnect.ui.volontario

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
import com.careconnect.work.WorkScheduler
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.careconnect.ui.common.nascondiBottomNavQuandoTastieraAperta

// contenitore della sezione Volontario: Toolbar del ruolo + NavHost annidato
// (nav_graph_volontario) + BottomNavigationView. Stesso schema di HomeAnzianoFragment.
class HomeVolontarioFragment : Fragment(R.layout.fragment_home_volontario) {

    private lateinit var appBarConfiguration: AppBarConfiguration

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val navHostFragment = childFragmentManager
            .findFragmentById(R.id.volontarioNavHostFragment) as NavHostFragment
        val navController = navHostFragment.navController

        nascondiBottomNavQuandoTastieraAperta(
            view,
            view.findViewById(R.id.volontarioBottomNav),
            viewLifecycleOwner
        )

        // bottom nav: inset basso della barra di sistema
        val bottomNav = view.findViewById<View>(R.id.volontarioBottomNav)
        ViewCompat.setOnApplyWindowInsetsListener(bottomNav) { v, insets ->
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
        // pianifica il controllo periodico delle nuove richieste (KEEP: nessun doppione se già attivo).
        WorkScheduler.pianificaControlloPeriodico(requireContext())
    }

    // funzione per collegare la Toolbar del ruolo al grafo di navigazione annidato.
    private fun collegaToolbar(view: View, navController: NavController) {
        val toolbar = view.findViewById<Toolbar>(R.id.volontarioToolbar)

        // solo la home (Richieste disponibili) è "di primo livello": lì la freccia non compare.
        appBarConfiguration = AppBarConfiguration(setOf(R.id.richiesteDisponibiliFragment))
        toolbar.setupWithNavController(navController, appBarConfiguration)

        // scorciatoia solo per la demo: long-press avvia subito il Worker,
        // senza aspettare i 15 minuti. non è per l'utente finale.
        toolbar.setOnLongClickListener {
            WorkScheduler.eseguiOraPerDemo(requireContext())
            Toast.makeText(requireContext(), "Controllo richieste avviato…", Toast.LENGTH_SHORT).show()
            true
        }
    }

    // funzione per collegare la BottomNavigationView al grafo di navigazione annidato.
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

        navController.addOnDestinationChangedListener { _, destination, _ ->
            bottomNav.menu.findItem(destination.id)?.isChecked = true
            // la chat ha un header proprio: nascondo la toolbar del ruolo lì
            view.findViewById<View>(R.id.volontarioToolbar).visibility =
                if (destination.id == R.id.chatFragment) View.GONE else View.VISIBLE
            ViewCompat.requestApplyInsets(view)
        }
    }

    // funzione che gestisce il tasto Indietro di sistema in modo esplicito e prevedibile.
    private fun gestisciTastoIndietro(navController: NavController) {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            val tornatoIndietro = navController.popBackStack()
            if (!tornatoIndietro) {
                isEnabled = false
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
        }
    }
}
package com.careconnect.ui.volontario

import android.os.Bundle
import android.view.View
import androidx.activity.addCallback
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
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

/**
 * Contenitore della sezione Volontario: Toolbar del ruolo + NavHost annidato
 * (nav_graph_volontario) + BottomNavigationView.
 * Stesso schema di HomeAnzianoFragment; qui la home del ruolo è
 * "Richieste disponibili".
 */
class HomeVolontarioFragment : Fragment(R.layout.fragment_home_volontario) {

    private lateinit var appBarConfiguration: AppBarConfiguration

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val navHostFragment = childFragmentManager
            .findFragmentById(R.id.volontarioNavHostFragment) as NavHostFragment
        val navController = navHostFragment.navController

        collegaToolbar(view, navController)
        collegaBottomNav(view, navController)
        gestisciTastoIndietro(navController)
        // Il volontario è appena entrato nella sua sezione: pianifico
        // il controllo periodico delle nuove richieste (KEEP: nessun doppione se già attivo).
        WorkScheduler.pianificaControlloPeriodico(requireContext())
    }

    // Collega la Toolbar del ruolo al grafo annidato.
    // Collega la Toolbar del ruolo al grafo annidato.
    private fun collegaToolbar(view: View, navController: NavController) {
        val toolbar = view.findViewById<Toolbar>(R.id.volontarioToolbar)

        // L'app disegna edge-to-edge: spingo la Toolbar sotto la barra di stato,
        // così titolo e freccia non finiscono sotto orologio/batteria.
        ViewCompat.setOnApplyWindowInsetsListener(toolbar) { v, insets ->
            val statusBar = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            v.setPadding(v.paddingLeft, statusBar.top, v.paddingRight, v.paddingBottom)
            insets
        }

        // Solo la home (Richieste disponibili) è "di primo livello": lì la freccia
        // NON compare. In tutte le altre schermate la freccia compare e torna indietro.
        appBarConfiguration = AppBarConfiguration(setOf(R.id.richiesteDisponibiliFragment))
        toolbar.setupWithNavController(navController, appBarConfiguration)

        // FASE 11 (solo per la DEMO) — Innesco nascosto: un long-press sulla Toolbar
        // fa partire SUBITO il Worker, senza aspettare l'intervallo periodico di 15 min.
        // NON è una funzione per l'utente finale: serve solo a mostrare il task all'orale.
        toolbar.setOnLongClickListener {
            WorkScheduler.eseguiOraPerDemo(requireContext())
            Toast.makeText(requireContext(), "Controllo richieste avviato…", Toast.LENGTH_SHORT).show()
            true // true = evento consumato, non propaghiamo oltre
        }
    }

    private fun collegaBottomNav(view: View, navController: NavController) {
        val bottomNav = view.findViewById<BottomNavigationView>(R.id.volontarioBottomNav)

        bottomNav.setOnItemSelectedListener { item ->
            // Se tocco il tab su cui sono già, non faccio nulla.
            if (item.itemId == navController.currentDestination?.id) {
                return@setOnItemSelectedListener true
            }
            // popUpTo(start) senza inclusive: lo stack resta [Home, tab scelto],
            // così l'Indietro da un tab secondario riporta sempre alla home.
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
            // Provo a tornare indietro nello stack del ruolo (tab secondario -> home).
            // popBackStack() restituisce false se non c'è più nulla da togliere,
            // cioè se siamo già sulla home.
            val tornatoIndietro = navController.popBackStack()
            if (!tornatoIndietro) {
                // Siamo sulla home: disabilito questo callback e lascio agire il
                // sistema. Non essendoci altro nello stack, l'app si chiude.
                isEnabled = false
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
        }
    }
}
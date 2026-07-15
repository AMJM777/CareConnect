package com.careconnect.ui.familiare

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.navOptions
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.careconnect.R
import com.careconnect.databinding.FragmentHomeFamiliareBinding
import com.careconnect.repository.AuthRepositoryImpl
import com.careconnect.repository.UserRepositoryImpl
import com.careconnect.viewmodel.familiare.HomeFamiliareViewModel
import com.careconnect.viewmodel.familiare.HomeFamiliareViewModelFactory
import com.careconnect.viewmodel.familiare.StatoHomeFamiliare
import kotlinx.coroutines.launch
import android.widget.Toast
import com.careconnect.work.WorkScheduler
import com.careconnect.ui.common.nascondiBottomNavQuandoTastieraAperta

/**
 * home del familiare. se l'utente non è ancora collegato a un anziano,
 * mostra il form per il codice invito. una volta collegato, aggancia a
 * runtime il NavHost annidato (Attività/Profilo), la toolbar e la bottomnav.
 */
class HomeFamiliareFragment : Fragment() {

    private var _binding: FragmentHomeFamiliareBinding? = null
    private val binding get() = _binding!!

    private lateinit var appBarConfiguration: AppBarConfiguration

    private val homeViewModel: HomeFamiliareViewModel by viewModels {
        HomeFamiliareViewModelFactory(UserRepositoryImpl(), AuthRepositoryImpl())
    }

    // evita di ricreare il grafo annidato ogni volta che lo stato "Collegato" viene riemesso
    private var navGraphAnnidatoAgganciato = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DataBindingUtil.inflate(inflater, R.layout.fragment_home_familiare, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.collegatiButton.setOnClickListener {
            homeViewModel.collegati(binding.codiceInvitoEditText.text.toString())
        }

        osservaStato()
        osservaErroreCollegamento()
        osservaCollegamentoInCorso()
    }

    // funzione per osservare lo stato (caricamento, non collegato, collegato) e mostrare la vista corretta
    private fun osservaStato() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                homeViewModel.stato.collect { stato ->
                    binding.caricamentoProgressBar.visibility =
                        if (stato is StatoHomeFamiliare.Caricamento) View.VISIBLE else View.GONE
                    binding.collegaGroup.visibility =
                        if (stato is StatoHomeFamiliare.NonCollegato) View.VISIBLE else View.GONE
                    binding.collegatoGroup.visibility =
                        if (stato is StatoHomeFamiliare.Collegato) View.VISIBLE else View.GONE

                    if (stato is StatoHomeFamiliare.Collegato) {
                        agganciaNavGraphAnnidato()
                    }
                }
            }
        }
    }

    // crea il NavHost annidato solo ora che si sa per certo che l'utente è
    // collegato a un anziano
    private fun agganciaNavGraphAnnidato() {
        if (navGraphAnnidatoAgganciato) return
        navGraphAnnidatoAgganciato = true
        binding.root.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.care_primary))

        val navHostFragment = NavHostFragment.create(R.navigation.nav_graph_familiare)
        childFragmentManager.beginTransaction()
            .replace(R.id.familiareNavHostContainer, navHostFragment)
            .setPrimaryNavigationFragment(navHostFragment)
            .commitNow()

        collegaToolbar(navHostFragment.navController)
        collegaBottomNav(navHostFragment.navController)
        nascondiBottomNavQuandoTastieraAperta(
            binding.root,
            binding.familiareBottomNav,
            viewLifecycleOwner
        )
        gestisciTastoIndietro(navHostFragment.navController)

        WorkScheduler.pianificaControlloConfermePeriodico(requireContext())
    }

    // Collega la Toolbar del ruolo al grafo annidato del Familiare
    private fun collegaToolbar(navController: NavController) {
        val toolbar = binding.familiareToolbar

        // Solo Attività è "di primo livello": lì la freccia NON compare.
        // Su Profilo la freccia compare e riporta ad Attività.
        appBarConfiguration = AppBarConfiguration(setOf(R.id.attivitaFamiliareFragment))
        toolbar.setupWithNavController(navController, appBarConfiguration)

        // scorciatoia solo per la presentazoine: long-press avvia subito il controllo
        // delle richieste da confermare, senza aspettare i 15 minuti
        toolbar.setOnLongClickListener {
            WorkScheduler.eseguiControlloConfermeOraPerDemo(requireContext())
            Toast.makeText(requireContext(), "Controllo conferme avviato…", Toast.LENGTH_SHORT).show()
            true
        }
    }

    // funzione per collegare la BottomNavigationView al grafo annidato
    private fun collegaBottomNav(navController: NavController) {
        val bottomNav = binding.familiareBottomNav

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
        }
    }
    // funzione che gestisce il tasto Indietro di sistema in modo esplicito
    private fun gestisciTastoIndietro(navController: NavController) {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            val tornatoIndietro = navController.popBackStack()
            if (!tornatoIndietro) {
                //  sulla home: disabilito questo callback e lascio agire il
                // sistema. Non essendoci altro nello stack, l'app si chiude.
                isEnabled = false
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
        }
    }

    // funzione per osservare eventuali errori nel collegamento tramite codice invito
    private fun osservaErroreCollegamento() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                homeViewModel.erroreCollegamento.collect { errore ->
                    binding.erroreCollegamentoText.visibility = if (errore != null) View.VISIBLE else View.GONE
                    binding.erroreCollegamentoText.text = errore ?: ""
                }
            }
        }
    }

    // funzione per osservare se il collegamento è in corso e mostrare/nascondere il caricamento
    private fun osservaCollegamentoInCorso() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                homeViewModel.collegamentoInCorso.collect { inCorso ->
                    binding.collegamentoProgressBar.visibility = if (inCorso) View.VISIBLE else View.GONE
                    binding.collegatiButton.isEnabled = !inCorso
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
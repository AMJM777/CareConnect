package com.careconnect.ui.familiare

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.navOptions
import com.careconnect.R
import com.careconnect.databinding.FragmentHomeFamiliareBinding
import com.careconnect.repository.AuthRepositoryImpl
import com.careconnect.repository.UserRepositoryImpl
import com.careconnect.viewmodel.familiare.HomeFamiliareViewModel
import com.careconnect.viewmodel.familiare.HomeFamiliareViewModelFactory
import com.careconnect.viewmodel.familiare.StatoHomeFamiliare
import kotlinx.coroutines.launch

/**
 * Home del Familiare (FASE 6).
 * Se l'utente non è ancora collegato a un anziano, mostra il form per il
 * codice invito. Una volta collegato, aggancia (a runtime, vedi
 * agganciaNavGraphAnnidato) la BottomNavigation con le 2 destinazioni
 * Attività/Profilo.
 */
class HomeFamiliareFragment : Fragment() {

    private var _binding: FragmentHomeFamiliareBinding? = null
    private val binding get() = _binding!!

    private val homeViewModel: HomeFamiliareViewModel by viewModels {
        HomeFamiliareViewModelFactory(UserRepositoryImpl(), AuthRepositoryImpl())
    }

    // Evita di ricreare il grafo annidato ogni volta che lo stato "Collegato" viene riemesso.
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

    /**
     * Crea e collega il NavHostFragment annidato SOLO ora che sappiamo per
     * certo che l'utente è collegato a un anziano. Diverso dal pattern
     * statico (app:navGraph in XML) usato per Anziano/Volontario apposta:
     * qui esiste uno stato "non collegato" che loro non hanno, e non
     * vogliamo che le schermate annidate partano a caricare dati prima
     * che il collegamento sia confermato.
     */
    private fun agganciaNavGraphAnnidato() {
        if (navGraphAnnidatoAgganciato) return
        navGraphAnnidatoAgganciato = true

        val navHostFragment = NavHostFragment.create(R.navigation.nav_graph_familiare)
        childFragmentManager.beginTransaction()
            .replace(R.id.familiareNavHostContainer, navHostFragment)
            .setPrimaryNavigationFragment(navHostFragment)
            .commitNow()

        collegaBottomNav(navHostFragment.navController)
        gestisciTastoIndietro(navHostFragment.navController)
    }

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

    /** Stesso schema manuale già usato per Anziano e Volontario. */
    private fun gestisciTastoIndietro(navController: NavController) {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            if (navController.currentDestination?.id != R.id.attivitaFamiliareFragment) {
                navController.popBackStack()
            } else {
                isEnabled = false
                requireActivity().onBackPressedDispatcher.onBackPressed()
                isEnabled = true
            }
        }
    }

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
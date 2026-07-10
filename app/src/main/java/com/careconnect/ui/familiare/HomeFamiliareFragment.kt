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
 * Home del Familiare (FASE 6).
 * Se l'utente non è ancora collegato a un anziano, mostra il form per il
 * codice invito (senza alcuna barra in alto). Una volta collegato, aggancia
 * a runtime il NavHost annidato (Attività/Profilo), la Toolbar del ruolo e
 * la BottomNavigation.
 *
 * Barre di sistema: lo spazio è riservato nel layout con fitsSystemWindows
 * (vedi fragment_home_familiare.xml). Lo sfondo della root è chiaro finché si
 * è sul form; quando ci si collega lo coloriamo di blu, così le strisce in
 * alto/basso diventano blu come nelle sezioni Anziano e Volontario.
 */
class HomeFamiliareFragment : Fragment() {

    private var _binding: FragmentHomeFamiliareBinding? = null
    private val binding get() = _binding!!

    private lateinit var appBarConfiguration: AppBarConfiguration

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
     * certo che l'utente è collegato a un anziano. Qui agganciamo anche la
     * Toolbar del ruolo, la BottomNavigation e il tasto Indietro: prima di
     * questo momento il NavHost non esiste ancora.
     */
    private fun agganciaNavGraphAnnidato() {
        if (navGraphAnnidatoAgganciato) return
        navGraphAnnidatoAgganciato = true

        // Da collegati coloriamo la root di blu: le schermate interne (opache)
        // coprono il centro, quindi si vede blu solo nelle strisce delle barre
        // di sistema, coerente con Anziano e Volontario.
        binding.root.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.care_primary))

        val navHostFragment = NavHostFragment.create(R.navigation.nav_graph_familiare)
        childFragmentManager.beginTransaction()
            .replace(R.id.familiareNavHostContainer, navHostFragment)
            .setPrimaryNavigationFragment(navHostFragment)
            .commitNow()

        collegaToolbar(navHostFragment.navController)
        collegaBottomNav(navHostFragment.navController)
        // Nasconde la bottom nav mentre la tastiera è aperta. Qui va DENTRO
        // agganciaNavGraphAnnidato perché la bottom nav esiste solo da
        // collegati. Usiamo binding.root come vista di riferimento e
        // viewLifecycleOwner per la rimozione automatica del listener.
        nascondiBottomNavQuandoTastieraAperta(
            binding.root,
            binding.familiareBottomNav,
            viewLifecycleOwner
        )
        gestisciTastoIndietro(navHostFragment.navController)

        // FASE 11b — Ora che sappiamo che il familiare è collegato a un anziano,
        // pianifichiamo il controllo periodico delle richieste da confermare.
        WorkScheduler.pianificaControlloConfermePeriodico(requireContext())
    }

    // Collega la Toolbar del ruolo al grafo annidato del Familiare.
    private fun collegaToolbar(navController: NavController) {
        val toolbar = binding.familiareToolbar

        // Solo Attività è "di primo livello": lì la freccia NON compare.
        // Su Profilo la freccia compare e riporta ad Attività.
        appBarConfiguration = AppBarConfiguration(setOf(R.id.attivitaFamiliareFragment))
        toolbar.setupWithNavController(navController, appBarConfiguration)

        // FASE 11b (solo per la DEMO) — Long-press sulla Toolbar: fa partire SUBITO
        // il controllo delle richieste da confermare, senza aspettare i 15 minuti.
        // NON è una funzione per l'utente finale: serve solo per l'orale.
        toolbar.setOnLongClickListener {
            WorkScheduler.eseguiControlloConfermeOraPerDemo(requireContext())
            Toast.makeText(requireContext(), "Controllo conferme avviato…", Toast.LENGTH_SHORT).show()
            true
        }
    }

    private fun collegaBottomNav(navController: NavController) {
        val bottomNav = binding.familiareBottomNav

        bottomNav.setOnItemSelectedListener { item ->
            // Se tocco il tab su cui sono già, non faccio nulla.
            if (item.itemId == navController.currentDestination?.id) {
                return@setOnItemSelectedListener true
            }
            // popUpTo(start) senza inclusive: lo stack resta [Attività, tab scelto],
            // così l'Indietro da Profilo riporta sempre ad Attività.
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
            // Provo a tornare indietro nello stack del ruolo (Profilo -> Attività).
            // popBackStack() restituisce false se non c'è più nulla da togliere,
            // cioè se siamo già su Attività (la home del ruolo).
            val tornatoIndietro = navController.popBackStack()
            if (!tornatoIndietro) {
                // Siamo sulla home: disabilito questo callback e lascio agire il
                // sistema. Non essendoci altro nello stack, l'app si chiude.
                isEnabled = false
                requireActivity().onBackPressedDispatcher.onBackPressed()
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
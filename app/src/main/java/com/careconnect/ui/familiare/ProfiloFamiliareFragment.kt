package com.careconnect.ui.familiare

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.navOptions
import com.careconnect.R
import com.careconnect.databinding.FragmentProfiloFamiliareBinding
import com.careconnect.repository.AuthRepositoryImpl
import com.careconnect.repository.UserRepositoryImpl
import com.careconnect.util.SessionCache
import com.careconnect.viewmodel.auth.AuthViewModel
import com.careconnect.viewmodel.auth.AuthViewModelFactory
import com.careconnect.viewmodel.familiare.ProfiloFamiliareViewModel
import com.careconnect.viewmodel.familiare.ProfiloFamiliareViewModelFactory
import kotlinx.coroutines.launch

/**
 * Profilo del Familiare (FASE 6): mostra il proprio nome e quello
 * dell'anziano seguito, più il logout (già presente dallo Step 4).
 */
class ProfiloFamiliareFragment : Fragment() {

    private var _binding: FragmentProfiloFamiliareBinding? = null
    private val binding get() = _binding!!

    private val authViewModel: AuthViewModel by activityViewModels {
        AuthViewModelFactory(AuthRepositoryImpl(), UserRepositoryImpl(), SessionCache(requireContext()))
    }

    private val profiloViewModel: ProfiloFamiliareViewModel by viewModels {
        ProfiloFamiliareViewModelFactory(UserRepositoryImpl(), AuthRepositoryImpl())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DataBindingUtil.inflate(inflater, R.layout.fragment_profilo_familiare, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.logoutButton.setOnClickListener { mostraConfermaLogout() }

        osservaInfo()
        osservaErrori()
    }

    private fun osservaInfo() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                profiloViewModel.info.collect { info ->
                    if (info != null) {
                        binding.nomeFamiliareText.text = "Sei collegato/a come: ${info.nomeFamiliare}"
                        binding.nomeAnzianoText.text = "Stai seguendo: ${info.nomeAnziano}"
                    }
                }
            }
        }
    }

    private fun osservaErrori() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                profiloViewModel.errore.collect { errore ->
                    if (errore != null) {
                        Toast.makeText(requireContext(), errore, Toast.LENGTH_SHORT).show()
                        profiloViewModel.erroreMostrato()
                    }
                }
            }
        }
    }

    private fun mostraConfermaLogout() {
        AlertDialog.Builder(requireContext())
            .setTitle("Vuoi uscire?")
            .setMessage("Dovrai effettuare di nuovo l'accesso per tornare in CareConnect.")
            .setPositiveButton("Sì, esci") { _, _ -> eseguiLogout() }
            .setNegativeButton("Annulla", null)
            .show()
    }

    private fun eseguiLogout() {
        // FIX: il logout ora passa dal condiviso AuthViewModel (resetta anche
        // sessionCache e AuthUiState).
        authViewModel.logout()

        val navHostFragmentPrincipale = requireActivity().supportFragmentManager
            .findFragmentById(R.id.navHostFragment) as NavHostFragment
        val navControllerPrincipale = navHostFragmentPrincipale.navController

        // Svuota TUTTO lo stack principale fino alla radice del grafo (inclusa)
        // e riparte dal login. In modo deterministico, non con il trucco
        // popUpTo(0): così il logout riporta sempre al login (mai fuori
        // dall'app) e da lì il tasto Indietro esce dall'app, senza residui
        // della sessione precedente.
        val opzioni = navOptions {
            popUpTo(navControllerPrincipale.graph.id) { inclusive = true }
        }
        navControllerPrincipale.navigate(R.id.nav_graph_auth, null, opzioni)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

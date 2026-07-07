package com.careconnect.ui.anziano

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import com.careconnect.databinding.FragmentProfiloAnzianoBinding
import com.careconnect.repository.AuthRepositoryImpl
import com.careconnect.repository.UserRepositoryImpl
import com.careconnect.util.SessionCache
import com.careconnect.viewmodel.anziano.ProfiloAnzianoViewModel
import com.careconnect.viewmodel.anziano.ProfiloAnzianoViewModelFactory
import com.careconnect.viewmodel.auth.AuthViewModel
import com.careconnect.viewmodel.auth.AuthViewModelFactory
import kotlinx.coroutines.launch

/**
 * Profilo dell'Anziano (FASE 8): nome/email/ruolo, codice invito e
 * indirizzo (spostati qui da Dashboard, che nello Step 2 diventa solo
 * 2 pulsanti), logout.
 */
class ProfiloAnzianoFragment : Fragment() {

    private var _binding: FragmentProfiloAnzianoBinding? = null
    private val binding get() = _binding!!

    private val authViewModel: AuthViewModel by activityViewModels {
        AuthViewModelFactory(
            AuthRepositoryImpl(),
            UserRepositoryImpl(),
            SessionCache(requireContext())
        )
    }

    private val viewModel: ProfiloAnzianoViewModel by viewModels {
        ProfiloAnzianoViewModelFactory(UserRepositoryImpl(), AuthRepositoryImpl())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DataBindingUtil.inflate(inflater, R.layout.fragment_profilo_anziano, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.emailText.text = viewModel.email ?: ""
        binding.logoutButton.setOnClickListener { mostraConfermaLogout() }
        binding.copiaCodiceButton.setOnClickListener { copiaCodiceNegliAppunti() }
        binding.salvaIndirizzoButton.setOnClickListener {
            viewModel.salvaIndirizzo(binding.indirizzoEditText.text.toString())
        }

        osservaProfilo()
        osservaCodiceInvito()
        osservaErrori()
        osservaIndirizzoSalvato()
    }

    private fun osservaProfilo() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.utente.collect { utente ->
                    if (utente != null) {
                        binding.nomeText.text = utente.nome
                        if (binding.indirizzoEditText.text.isNullOrBlank()) {
                            binding.indirizzoEditText.setText(utente.indirizzo ?: "")
                        }
                    }
                }
            }
        }
    }

    private fun osservaCodiceInvito() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.codiceInvito.collect { codice ->
                    binding.codiceInvitoText.text = codice ?: "..."
                    binding.copiaCodiceButton.isEnabled = codice != null
                }
            }
        }
    }

    private fun osservaErrori() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.errore.collect { errore ->
                    if (errore != null) {
                        Toast.makeText(requireContext(), errore, Toast.LENGTH_SHORT).show()
                        viewModel.erroreMostrato()
                    }
                }
            }
        }
    }

    private fun osservaIndirizzoSalvato() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.indirizzoSalvato.collect { salvato ->
                    if (salvato) {
                        Toast.makeText(requireContext(), "Indirizzo salvato", Toast.LENGTH_SHORT).show()
                        viewModel.indirizzoSalvatoMostrato()
                    }
                }
            }
        }
    }

    private fun copiaCodiceNegliAppunti() {
        val codice = viewModel.codiceInvito.value ?: return
        val clipboardManager =
            requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboardManager.setPrimaryClip(ClipData.newPlainText("Codice invito CareConnect", codice))
        Toast.makeText(requireContext(), "Codice copiato", Toast.LENGTH_SHORT).show()
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
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
import com.careconnect.databinding.FragmentDashboardAnzianoBinding
import com.careconnect.repository.AuthRepositoryImpl
import com.careconnect.repository.UserRepositoryImpl
import com.careconnect.util.SessionCache
import com.careconnect.viewmodel.anziano.DashboardAnzianoViewModel
import com.careconnect.viewmodel.anziano.DashboardAnzianoViewModelFactory
import com.careconnect.viewmodel.auth.AuthViewModel
import com.careconnect.viewmodel.auth.AuthViewModelFactory
import kotlinx.coroutines.launch

class DashboardAnzianoFragment : Fragment() {

    private var _binding: FragmentDashboardAnzianoBinding? = null
    private val binding get() = _binding!!

    private val authViewModel: AuthViewModel by activityViewModels {
        AuthViewModelFactory(
            AuthRepositoryImpl(),
            UserRepositoryImpl(),
            SessionCache(requireContext())
        )
    }

    private val dashboardViewModel: DashboardAnzianoViewModel by viewModels {
        DashboardAnzianoViewModelFactory(UserRepositoryImpl(), AuthRepositoryImpl())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_dashboard_anziano, container, false
        )
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.logoutButton.setOnClickListener { mostraConfermaLogout() }
        binding.copiaCodiceButton.setOnClickListener { copiaCodiceNegliAppunti() }
        binding.salvaIndirizzoButton.setOnClickListener {
            dashboardViewModel.salvaIndirizzo(binding.indirizzoEditText.text.toString())
        }

        osservaProfilo()
        osservaCodiceInvito()
        osservaErrori()
        osservaIndirizzoSalvato()
    }

    /** Precompila il campo indirizzo con quello già salvato, se esiste. */
    private fun osservaProfilo() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                dashboardViewModel.utente.collect { utente ->
                    if (utente != null && binding.indirizzoEditText.text.isNullOrBlank()) {
                        binding.indirizzoEditText.setText(utente.indirizzo ?: "")
                    }
                }
            }
        }
    }

    private fun osservaCodiceInvito() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                dashboardViewModel.codiceInvito.collect { codice ->
                    binding.codiceInvitoText.text = codice ?: "..."
                    binding.copiaCodiceButton.isEnabled = codice != null
                }
            }
        }
    }

    private fun osservaErrori() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                dashboardViewModel.errore.collect { errore ->
                    if (errore != null) {
                        Toast.makeText(requireContext(), errore, Toast.LENGTH_SHORT).show()
                        dashboardViewModel.erroreMostrato()
                    }
                }
            }
        }
    }

    private fun osservaIndirizzoSalvato() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                dashboardViewModel.indirizzoSalvato.collect { salvato ->
                    if (salvato) {
                        Toast.makeText(requireContext(), "Indirizzo salvato", Toast.LENGTH_SHORT).show()
                        dashboardViewModel.indirizzoSalvatoMostrato()
                    }
                }
            }
        }
    }

    private fun copiaCodiceNegliAppunti() {
        val codice = dashboardViewModel.codiceInvito.value ?: return
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
        authViewModel.logout()

        val navHostFragmentPrincipale = requireActivity().supportFragmentManager
            .findFragmentById(R.id.navHostFragment) as NavHostFragment
        val navControllerPrincipale = navHostFragmentPrincipale.navController

        val opzioni = navOptions {
            popUpTo(0) { inclusive = true }
        }
        navControllerPrincipale.navigate(R.id.nav_graph_auth, null, opzioni)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
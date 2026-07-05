package com.careconnect.ui.volontario

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
import com.careconnect.databinding.FragmentProfiloVolontarioBinding
import com.careconnect.repository.AuthRepositoryImpl
import com.careconnect.repository.UserRepositoryImpl
import com.careconnect.util.SessionCache
import com.careconnect.viewmodel.auth.AuthViewModel
import com.careconnect.viewmodel.auth.AuthViewModelFactory
import com.careconnect.viewmodel.volontario.ProfiloVolontarioViewModel
import com.careconnect.viewmodel.volontario.ProfiloVolontarioViewModelFactory
import kotlinx.coroutines.launch

/**
 * Schermata Profilo del Volontario: nome, email, ruolo, valutazione,
 * descrizione modificabile, e logout.
 *
 * FIX: il logout ora passa dal condiviso AuthViewModel (stesso schema di
 * Anziano/Familiare), non più da un logout() locale di
 * ProfiloVolontarioViewModel — quest'ultimo disconnetteva Firebase ma non
 * resettava lo stato di AuthViewModel, causando un rimbalzo immediato
 * indietro alla Home Volontario appena si arrivava al login.
 */
class ProfiloVolontarioFragment : Fragment() {

    private var _binding: FragmentProfiloVolontarioBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProfiloVolontarioViewModel by viewModels {
        ProfiloVolontarioViewModelFactory(UserRepositoryImpl(), AuthRepositoryImpl())
    }

    // Stesso AuthViewModel condiviso usato da Login/Registrazione e dagli
    // altri due ruoli: activityViewModels() garantisce che sia la STESSA
    // istanza, quindi il logout() qui resetta lo stato che LoginFragment
    // osserva davvero.
    private val authViewModel: AuthViewModel by activityViewModels {
        AuthViewModelFactory(
            AuthRepositoryImpl(),
            UserRepositoryImpl(),
            SessionCache(requireContext())
        )
    }

    private var bioGiaPrecompilata = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_profilo_volontario, container, false
        )
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.emailText.text = viewModel.email ?: ""
        binding.logoutButton.setOnClickListener { mostraConfermaLogout() }
        binding.salvaBioButton.setOnClickListener {
            viewModel.salvaBio(binding.bioEditText.text.toString())
        }

        osservaUtente()
        osservaErrori()
        osservaBioSalvata()
    }

    private fun osservaUtente() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.utente.collect { utente ->
                    if (utente != null) {
                        binding.nomeText.text = utente.nome
                        binding.ratingText.text = utente.ratingMedio?.let { media ->
                            "Valutazione: %.1f / 5".format(media)
                        } ?: "Valutazione: non ancora valutato"

                        if (!bioGiaPrecompilata) {
                            binding.bioEditText.setText(utente.bio ?: "")
                            bioGiaPrecompilata = true
                        }
                    }
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

    private fun osservaBioSalvata() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.bioSalvata.collect { salvata ->
                    if (salvata) {
                        Toast.makeText(requireContext(), "Descrizione salvata", Toast.LENGTH_SHORT).show()
                        viewModel.bioSalvataMostrata()
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
        // FIX: era viewModel.logout() (locale, incompleto) — ora usa il
        // logout condiviso, che resetta anche sessionCache e AuthUiState.
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
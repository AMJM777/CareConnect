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
 * DATA BINDING (lezione 9): nome/email/valutazione sono legati direttamente
 * dall'XML tramite @{viewModel.campo}. Per farlo bastano due righe dopo
 * l'inflate: binding.viewModel = viewModel e binding.lifecycleOwner =
 * viewLifecycleOwner (così il binding osserva i LiveData e aggiorna le
 * TextView da solo). Il Fragment non ha più codice per riempirle a mano.
 *
 * FIX (invariato): il logout passa dal condiviso AuthViewModel, non da un
 * logout() locale, per non lasciare lo stato di autenticazione "sporco".
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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_profilo_volontario, container, false
        )
        // Colleghiamo il ViewModel al layout: da qui le espressioni @{} nel
        // file XML possono leggere i suoi LiveData.
        binding.viewModel = viewModel
        // Diamo al binding un "proprietario del ciclo di vita": senza questo
        // le TextView legate a LiveData NON si aggiornerebbero da sole.
        binding.lifecycleOwner = viewLifecycleOwner
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.logoutButton.setOnClickListener { mostraConfermaLogout() }
        binding.salvaBioButton.setOnClickListener {
            viewModel.salvaBio(binding.bioEditText.text.toString())
        }

        preRiempiBio()
        osservaErrori()
        osservaBioSalvata()
    }

    // La bio è un campo EDITABILE: non la leghiamo in due vie, la scriviamo
    // nell'EditText una sola volta quando il profilo è caricato.
    private fun preRiempiBio() {
        viewModel.bioIniziale.observe(viewLifecycleOwner) { bio ->
            binding.bioEditText.setText(bio)
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
        // Il logout passa dal condiviso AuthViewModel (resetta anche
        // sessionCache e AuthUiState).
        authViewModel.logout()

        val navHostFragmentPrincipale = requireActivity().supportFragmentManager
            .findFragmentById(R.id.navHostFragment) as NavHostFragment
        val navControllerPrincipale = navHostFragmentPrincipale.navController

        // Svuota tutto lo stack principale fino alla radice (inclusa) e
        // riparte dal login, in modo deterministico (non con popUpTo(0)).
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
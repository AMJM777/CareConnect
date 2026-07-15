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
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding

// profilo dell'Anziano: nome/email/ruolo, codice invito, indirizzo, logout.
// nome/email/codice invito sono legati dall'XML con data binding; l'indirizzo
// è editabile e resta gestito a mano
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
        // collega il ViewModel al layout: da qui le View legate a LiveData si aggiornano da sole
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // padding in basso pari all'altezza della tastiera, così il campo
        // indirizzo può salire sopra di essa.
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val tastiera = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            v.updatePadding(bottom = tastiera)
            insets
        }

        binding.logoutButton.setOnClickListener { mostraConfermaLogout() }
        binding.copiaCodiceButton.setOnClickListener { copiaCodiceNegliAppunti() }
        binding.salvaIndirizzoButton.setOnClickListener {
            viewModel.salvaIndirizzo(binding.indirizzoEditText.text.toString())
        }

        preRiempiIndirizzo()
        osservaErrori()
        osservaIndirizzoSalvato()
    }

    // l'indirizzo è un campo editabile: viene scritto nell'EditText una sola volta, al caricamento
    private fun preRiempiIndirizzo() {
        viewModel.indirizzoIniziale.observe(viewLifecycleOwner) { indirizzo ->
            if (binding.indirizzoEditText.text.isNullOrBlank()) {
                binding.indirizzoEditText.setText(indirizzo)
            }
        }
    }

    // funzione per osservare eventuali errori esposti dal ViewModel e mostrarli con un Toast.
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

    // funzione per osservare la conferma di salvataggio dell'indirizzo e mostrare un Toast.
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

    // funzione per copiare il codice invito negli appunti del dispositivo
    private fun copiaCodiceNegliAppunti() {
        val codice = viewModel.codicePerCopia() ?: return
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
    // funzione che esegue il logout e riporta l'utente al flusso di autenticazione
    private fun eseguiLogout() {
        // passa dal condiviso AuthViewModel: resetta anche sessionCache e AuthUiState.
        authViewModel.logout()

        val navHostFragmentPrincipale = requireActivity().supportFragmentManager
            .findFragmentById(R.id.navHostFragment) as NavHostFragment
        val navControllerPrincipale = navHostFragmentPrincipale.navController
        // svuota lo stack fino alla radice e riparte dal logi
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
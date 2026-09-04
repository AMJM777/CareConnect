package com.careconnect.ui.familiare

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
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

// profilo del familiare: nome proprio e dell'anziano seguito, più logout
// entrambe le TextView sono legate dall'XML con data binding
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
        // collego il ViewModel al layout e passo il lifecycle owner, così le
        // TextView legate si aggiornano da sole
        binding.viewModel = profiloViewModel
        binding.lifecycleOwner = viewLifecycleOwner
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.logoutButton.setOnClickListener { mostraConfermaLogout() }
        binding.collegaButton.setOnClickListener {
            profiloViewModel.collegati(binding.codiceInvitoEditText.text.toString())
        }

        osservaErrori()
        osservaAnziani()
        osservaCollegamento()
    }

    // mostra la lista degli anziani seguiti; se vuota mostra l'invito a collegarne uno
    private fun osservaAnziani() {
        profiloViewModel.anzianiSeguiti.observe(viewLifecycleOwner) { anziani ->
            val container = binding.anzianiContainer
            container.removeAllViews()
            binding.nessunAnzianoText.visibility =
                if (anziani.isEmpty()) View.VISIBLE else View.GONE
            val inflater = LayoutInflater.from(requireContext())
            anziani.forEach { anziano ->
                val riga = inflater.inflate(R.layout.item_anziano_seguito, container, false)
                riga.findViewById<TextView>(R.id.anzianoNomeText).text = anziano.nome
                riga.findViewById<TextView>(R.id.anzianoIndirizzoText).text =
                    anziano.indirizzo?.takeIf { it.isNotBlank() }
                        ?: "Indirizzo non ancora indicato"
                container.addView(riga)
            }
        }
    }

    // conferma di collegamento riuscito: pulisco il campo e avviso
    private fun osservaCollegamento() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                profiloViewModel.collegamentoRiuscito.collect { ok ->
                    if (ok) {
                        Toast.makeText(requireContext(), "Anziano collegato", Toast.LENGTH_SHORT).show()
                        binding.codiceInvitoEditText.text?.clear()
                        profiloViewModel.collegamentoRiuscitoMostrato()
                    }
                }
            }
        }
    }

    // funzione per osservare eventuali errori esposti dal ViewModel e mostrarli con un toast
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

    // funzione che esegue il logout e riporta l'utente al flusso di autenticazione
    private fun eseguiLogout() {
        authViewModel.logout()

        val navHostFragmentPrincipale = requireActivity().supportFragmentManager
            .findFragmentById(R.id.navHostFragment) as NavHostFragment
        val navControllerPrincipale = navHostFragmentPrincipale.navController
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
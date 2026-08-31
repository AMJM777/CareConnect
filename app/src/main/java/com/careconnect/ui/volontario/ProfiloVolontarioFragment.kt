package com.careconnect.ui.volontario

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RatingBar
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
import com.careconnect.databinding.FragmentProfiloVolontarioBinding
import com.careconnect.repository.AuthRepositoryImpl
import com.careconnect.repository.RatingRepositoryImpl
import com.careconnect.repository.UserRepositoryImpl
import com.careconnect.util.SessionCache
import com.careconnect.viewmodel.auth.AuthViewModel
import com.careconnect.viewmodel.auth.AuthViewModelFactory
import com.careconnect.viewmodel.volontario.ProfiloVolontarioViewModel
import com.careconnect.viewmodel.volontario.ProfiloVolontarioViewModelFactory
import kotlinx.coroutines.launch
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding

// profilo del volontario: nome, email, ruolo, valutazione, descrizione
// modificabile, logout. nome/email/valutazione sono legati dall'XML con data binding.
class ProfiloVolontarioFragment : Fragment() {

    private var _binding: FragmentProfiloVolontarioBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProfiloVolontarioViewModel by viewModels {
        ProfiloVolontarioViewModelFactory(UserRepositoryImpl(), AuthRepositoryImpl(), RatingRepositoryImpl())
    }

    // stesso AuthViewModel condiviso di login/registrazione: garantisce che
    // sia la stessa istanza, così il logout resetta lo stato osservato altrove.
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
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // padding in basso pari all'altezza della tastiera, così il campo bio può salire sopra.
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val tastiera = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            v.updatePadding(bottom = tastiera)
            insets
        }
        binding.logoutButton.setOnClickListener { mostraConfermaLogout() }
        binding.salvaBioButton.setOnClickListener {
            viewModel.salvaBio(binding.bioEditText.text.toString())
        }

        preRiempiBio()
        osservaErrori()
        osservaBioSalvata()
        osservaRecensioni()
    }

    // mostra i commenti ricevuti (solo le valutazioni con testo) sotto le stelle
    private fun osservaRecensioni() {
        viewModel.recensioni.observe(viewLifecycleOwner) { recensioni ->
            val container = binding.recensioniContainer
            container.removeAllViews()
            binding.recensioniTitolo.visibility =
                if (recensioni.isEmpty()) View.GONE else View.VISIBLE
            val inflater = LayoutInflater.from(requireContext())
            recensioni.forEach { recensione ->
                val riga = inflater.inflate(R.layout.item_recensione, container, false)
                riga.findViewById<RatingBar>(R.id.recensioneRatingBar).rating = recensione.stelle.toFloat()
                riga.findViewById<TextView>(R.id.recensioneCommentoText).text = recensione.commento
                container.addView(riga)
            }
        }
    }

    // la bio è un campo editabile: viene scritta nell'EditText una sola volta, al caricamento.
    private fun preRiempiBio() {
        viewModel.bioIniziale.observe(viewLifecycleOwner) { bio ->
            binding.bioEditText.setText(bio)
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

    // funzione per osservare la conferma di salvataggio della bio e mostrare un Toast.
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

    // funzione che esegue il logout e riporta l'utente al flusso di autenticazione.
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
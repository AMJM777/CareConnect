package com.careconnect.ui.volontario

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
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
import com.careconnect.viewmodel.volontario.ProfiloVolontarioViewModel
import com.careconnect.viewmodel.volontario.ProfiloVolontarioViewModelFactory
import kotlinx.coroutines.launch

/**
 * Schermata Profilo del Volontario: nome, email, ruolo, valutazione
 * (placeholder fino alla Fase 7), descrizione modificabile, e logout.
 */
class ProfiloVolontarioFragment : Fragment() {

    private var _binding: FragmentProfiloVolontarioBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProfiloVolontarioViewModel by viewModels {
        ProfiloVolontarioViewModelFactory(UserRepositoryImpl(), AuthRepositoryImpl())
    }

    // Tiene traccia se l'EditText della bio è già stato riempito una volta
    // con il valore caricato da Firestore: senza questo flag, ogni nuova
    // emissione del Flow "utente" (es. dopo il salvataggio) sovrascriverebbe
    // quello che l'utente sta scrivendo in quel momento.
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

                        // Precompila l'EditText solo la prima volta: dopo un
                        // salvataggio riuscito, _utente si aggiorna di nuovo,
                        // ma non vogliamo "resettare" il campo se l'utente
                        // sta già continuando a scrivere qualcos'altro.
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
        viewModel.logout()

        val navHostFragmentPrincipale = requireActivity().supportFragmentManager
            .findFragmentById(R.id.navHostFragment) as NavHostFragment
        val navControllerPrincipale = navHostFragmentPrincipale.navController

        val opzioni = navOptions {
            popUpTo(R.id.homeVolontarioFragment) { inclusive = true }
        }
        navControllerPrincipale.navigate(R.id.nav_graph_auth, null, opzioni)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
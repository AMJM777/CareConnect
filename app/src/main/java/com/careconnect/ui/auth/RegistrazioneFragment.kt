package com.careconnect.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.careconnect.R
import com.careconnect.databinding.FragmentRegistrazioneBinding
import com.careconnect.model.UserRole
import com.careconnect.repository.AuthRepositoryImpl
import com.careconnect.repository.UserRepositoryImpl
import com.careconnect.viewmodel.auth.AuthUiState
import com.careconnect.viewmodel.auth.AuthViewModel
import com.careconnect.viewmodel.auth.AuthViewModelFactory
import kotlinx.coroutines.launch
import androidx.navigation.fragment.findNavController
import com.careconnect.ui.auth.navigaAllaHomePerRuolo
import com.careconnect.util.SessionCache


 //Schermata di registrazione (email/password + scelta ruolo)

class RegistrazioneFragment : Fragment() {

    private var _binding: FragmentRegistrazioneBinding? = null
    private val binding get() = _binding!!
    // stesso AuthViewModel di LoginFragment, condiviso tramite l'Activity
    private val viewModel: AuthViewModel by activityViewModels {
        AuthViewModelFactory(AuthRepositoryImpl(), UserRepositoryImpl(), SessionCache(requireContext()))
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DataBindingUtil.inflate(inflater, R.layout.fragment_registrazione, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.registratiButton.setOnClickListener { onRegistratiClick() }

        binding.goToLoginText.setOnClickListener {
            findNavController().navigate(R.id.action_registrazione_to_login)
        }

        osservaStatoAutenticazione()
    }

    // funzione che valida il form e chiama il ViewModel per la registrazione
    private fun onRegistratiClick() {
        val nome = binding.nomeInput.text.toString().trim()
        val email = binding.emailInput.text.toString().trim()
        val password = binding.passwordInput.text.toString()
        val confermaPassword = binding.confermaPasswordInput.text.toString()
        val ruolo = ruoloSelezionato()

        val erroreValidazione = when {
            nome.isEmpty() || email.isEmpty() || password.isEmpty() ->
                getString(R.string.register_error_campi_vuoti)
            password != confermaPassword ->
                getString(R.string.register_error_password_diverse)
            ruolo == null ->
                getString(R.string.register_error_ruolo_mancante)
            else -> null
        }

        if (erroreValidazione != null) {
            mostraErroreLocale(erroreValidazione)
            return
        }

        // ruolo non è null qui: già escluso dal when sopra
        viewModel.registraConEmail(nome, email, password, ruolo!!)
    }


    // funzione per leggere quale RadioButton del ruolo è selezionato
    private fun ruoloSelezionato(): UserRole? = when (binding.ruoloRadioGroup.checkedRadioButtonId) {
        R.id.ruoloAnzianoRadio -> UserRole.ANZIANO
        R.id.ruoloVolontarioRadio -> UserRole.VOLONTARIO
        R.id.ruoloFamiliareRadio -> UserRole.FAMILIARE
        else -> null
    }

    // funzione per mostrare un errore di validazione locale (non da Firebase)
    private fun mostraErroreLocale(messaggio: String) {
        binding.errorText.text = messaggio
        binding.errorText.visibility = View.VISIBLE
    }

    // funzione per osservare lo stato del ViewModel e aggiornare la UI di conseguenza
    private fun osservaStatoAutenticazione() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { stato -> aggiornaUi(stato) }
            }
        }
    }

    private fun aggiornaUi(stato: AuthUiState) {
        binding.loadingIndicator.visibility =
            if (stato is AuthUiState.Loading) View.VISIBLE else View.GONE

        if (stato is AuthUiState.Errore) {
            binding.errorText.text = stato.eccezione.message
                ?: getString(R.string.login_generic_error)
            binding.errorText.visibility = View.VISIBLE
        }

        if (stato is AuthUiState.Autenticato) {
            navigaAllaHomePerRuolo(stato.ruolo)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
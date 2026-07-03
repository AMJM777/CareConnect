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
import com.careconnect.databinding.FragmentCompletaProfiloGoogleBinding
import com.careconnect.model.UserRole
import com.careconnect.repository.AuthRepositoryImpl
import com.careconnect.repository.UserRepositoryImpl
import com.careconnect.viewmodel.auth.AuthUiState
import com.careconnect.viewmodel.auth.AuthViewModel
import com.careconnect.viewmodel.auth.AuthViewModelFactory
import kotlinx.coroutines.launch
import com.careconnect.ui.auth.navigaAllaHomePerRuolo
import com.careconnect.util.SessionCache

/**
 * Schermata mostrata solo al primo accesso con Google, quando Firebase Auth
 * ha già le credenziali ma il profilo Firestore non esiste ancora.
 * Raccoglie nome (pre-compilato da Google, se disponibile) e ruolo,
 * poi chiama AuthViewModel.completaRegistrazioneGoogle() per salvarli.
 */
class CompletaProfiloGoogleFragment : Fragment() {

    private var _binding: FragmentCompletaProfiloGoogleBinding? = null
    private val binding get() = _binding!!

    // Stesso AuthViewModel di Login/Registrazione: lo stato RichiestaRuoloGoogle
    // che ci ha portato qui vive già in questa istanza condivisa.
    private val viewModel: AuthViewModel by activityViewModels {
        AuthViewModelFactory(AuthRepositoryImpl(), UserRepositoryImpl(), SessionCache(requireContext()))
    }

    // Evita di sovrascrivere il testo digitato dall'utente ogni volta che
    // arriva un nuovo stato (es. quando passa a Loading dopo il click):
    // pre-compiliamo il nome una sola volta.
    private var nomeGiaPrecompilato = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_completa_profilo_google, container, false
        )
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.completaButton.setOnClickListener { onCompletaClick() }

        osservaStatoAutenticazione()
    }

    private fun onCompletaClick() {
        val nome = binding.nomeInput.text.toString().trim()
        val ruolo = ruoloSelezionato()

        val erroreValidazione = when {
            nome.isEmpty() -> getString(R.string.register_error_campi_vuoti)
            ruolo == null -> getString(R.string.register_error_ruolo_mancante)
            else -> null
        }

        if (erroreValidazione != null) {
            mostraErroreLocale(erroreValidazione)
            return
        }

        viewModel.completaRegistrazioneGoogle(nome, ruolo!!)
    }

    private fun ruoloSelezionato(): UserRole? = when (binding.ruoloRadioGroup.checkedRadioButtonId) {
        R.id.ruoloAnzianoRadio -> UserRole.ANZIANO
        R.id.ruoloVolontarioRadio -> UserRole.VOLONTARIO
        R.id.ruoloFamiliareRadio -> UserRole.FAMILIARE
        else -> null
    }

    private fun mostraErroreLocale(messaggio: String) {
        binding.errorText.text = messaggio
        binding.errorText.visibility = View.VISIBLE
    }

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

        // Pre-compila il nome una sola volta, quando arriviamo per la prima
        // volta in questo stato con un nome fornito da Google.
        if (stato is AuthUiState.RichiestaRuoloGoogle && !nomeGiaPrecompilato) {
            stato.utente.nome?.let { binding.nomeInput.setText(it) }
            nomeGiaPrecompilato = true
        }

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
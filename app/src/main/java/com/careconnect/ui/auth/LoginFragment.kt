package com.careconnect.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.careconnect.R
import com.careconnect.databinding.FragmentLoginBinding
import com.careconnect.repository.AuthRepositoryImpl
import com.careconnect.repository.UserRepositoryImpl
import com.careconnect.viewmodel.auth.AuthUiState
import com.careconnect.viewmodel.auth.AuthViewModel
import com.careconnect.viewmodel.auth.AuthViewModelFactory
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch
import androidx.navigation.fragment.findNavController
import com.careconnect.ui.auth.navigaAllaHomePerRuolo
import com.careconnect.util.SessionCache

/**
 * Schermata di login (email/password + Google).
 * Usa lo stesso AuthViewModel di RegistrazioneFragment: activityViewModels()
 * lo scopa alla Activity (unica nell'app), non serve un NavGraph per condividerlo.
 * TODO Fase 3: valutare passaggio a navGraphViewModels() quando esiste il grafo
 * di navigazione, per uno scoping più stretto al solo flusso di autenticazione.
 */
class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AuthViewModel by activityViewModels {
        AuthViewModelFactory(AuthRepositoryImpl(), UserRepositoryImpl(), SessionCache(requireContext()))
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DataBindingUtil.inflate(inflater, R.layout.fragment_login, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.loginButton.setOnClickListener {
            val email = binding.emailInput.text.toString().trim()
            val password = binding.passwordInput.text.toString()
            viewModel.loginConEmail(email, password)
        }

        binding.googleSignInButton.setOnClickListener { avviaLoginGoogle() }

        binding.goToRegisterText.setOnClickListener {
            findNavController().navigate(R.id.action_login_to_registrazione)
        }

        osservaStatoAutenticazione()
    }

    /**
     * Apre il selettore account di sistema (Credential Manager) e, se l'utente
     * ne sceglie uno, passa l'ID token ottenuto al ViewModel per completare
     * il login su Firebase. Vive nel Fragment (non nel ViewModel) perché
     * Credential Manager richiede il Context dell'Activity per mostrare la UI.
     */
    private fun avviaLoginGoogle() {
        val credentialManager = CredentialManager.create(requireContext())

        // Richiede specificamente credenziali Google (non password salvate o altro)
        val googleIdOption = GetGoogleIdOption.Builder()
            .setServerClientId(getString(R.string.default_web_client_id))
            // false = mostra anche account non ancora usati con questa app,
            // non solo quelli già "autorizzati" in passato
            .setFilterByAuthorizedAccounts(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val result = credentialManager.getCredential(
                    context = requireContext(),
                    request = request
                )
                val googleIdTokenCredential = GoogleIdTokenCredential
                    .createFrom(result.credential.data)

                viewModel.loginConGoogle(googleIdTokenCredential.idToken)

            } catch (e: GetCredentialCancellationException) {
                // L'utente ha chiuso il selettore senza scegliere nulla:
                // non è un errore, semplicemente non facciamo nulla e il
                // form resta nello stato in cui era (Idle).

            } catch (e: Exception) {
                // Qualunque altro problema (es. nessun account Google sul
                // dispositivo, problemi di configurazione): lo trattiamo
                // come un vero errore, mostrato tramite lo stesso AuthUiState
                // usato per gli errori di login email/password.
                binding.errorText.text = e.message ?: getString(R.string.login_generic_error)
                binding.errorText.visibility = View.VISIBLE
            }
        }
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

        binding.errorText.visibility =
            if (stato is AuthUiState.Errore) View.VISIBLE else View.GONE

        if (stato is AuthUiState.Errore) {
            binding.errorText.text = stato.eccezione.message
                ?: getString(R.string.login_generic_error)
        }

        if (stato is AuthUiState.Autenticato) {
            navigaAllaHomePerRuolo(stato.ruolo)
        }

        if (stato is AuthUiState.RichiestaRuoloGoogle) {
            findNavController().navigate(R.id.action_login_to_completaProfiloGoogle)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
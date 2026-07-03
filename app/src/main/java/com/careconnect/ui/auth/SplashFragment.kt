package com.careconnect.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import com.careconnect.R
import com.careconnect.databinding.FragmentSplashBinding
import com.careconnect.model.UserRole
import com.careconnect.repository.AuthRepositoryImpl
import com.careconnect.repository.UserRepositoryImpl
import com.careconnect.util.SessionCache
import com.careconnect.viewmodel.auth.SplashUiState
import com.careconnect.viewmodel.auth.SplashViewModel
import com.careconnect.viewmodel.auth.SplashViewModelFactory
import kotlinx.coroutines.launch

/**
 * Vero startDestination del grafo principale. Non è una schermata con cui
 * l'utente interagisce: appare solo per il tempo necessario a
 * SplashViewModel per decidere se mostrare il login o saltare direttamente
 * alla home del ruolo corretto (auto-login).
 *
 * Non usa activityViewModels(): a differenza di AuthViewModel, questo
 * ViewModel serve solo qui, una volta sola all'avvio, quindi lo scope
 * di default (legato al Fragment stesso) va bene.
 */
class SplashFragment : Fragment() {

    private var _binding: FragmentSplashBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SplashViewModel by viewModels {
        SplashViewModelFactory(
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
        _binding = DataBindingUtil.inflate(inflater, R.layout.fragment_splash, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        osservaStato()
    }

    private fun osservaStato() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { stato -> reagisciAStato(stato) }
            }
        }
    }

    private fun reagisciAStato(stato: SplashUiState) {
        when (stato) {
            is SplashUiState.Verifica -> {
                // Controllo ancora in corso (fallback su Firestore): non facciamo
                // nulla, la ProgressBar del layout resta visibile.
            }

            is SplashUiState.VaiAlLogin -> {
                // popUpTo(splashFragment, inclusive = true): la Splash sparisce
                // dal back stack, altrimenti il tasto Indietro dal login
                // riporterebbe l'utente su una schermata vuota.
                val opzioni = navOptions {
                    popUpTo(R.id.splashFragment) { inclusive = true }
                }
                findNavController().navigate(R.id.nav_graph_auth, null, opzioni)
            }

            is SplashUiState.VaiAllaHome -> {
                // Non riusiamo navigaAllaHomePerRuolo(): quella funzione fa
                // popUpTo(nav_graph_auth), corretto quando si arriva da
                // Login/Registrazione. Da qui invece dobbiamo rimuovere la
                // Splash stessa dal back stack, altrimenti il tasto Indietro
                // dalla home ci farebbe rimbalzare su una Splash che
                // ri-naviga subito di nuovo alla home.
                val destinazione = when (stato.ruolo) {
                    UserRole.ANZIANO -> R.id.homeAnzianoFragment
                    UserRole.VOLONTARIO -> R.id.homeVolontarioFragment
                    UserRole.FAMILIARE -> R.id.homeFamiliareFragment
                }
                val opzioni = navOptions {
                    popUpTo(R.id.splashFragment) { inclusive = true }
                }
                findNavController().navigate(destinazione, null, opzioni)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
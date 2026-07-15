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

// startDestination del grafo principale: decide se mostrare il login o
// saltare direttamente alla home del ruolo corretto (auto-login)
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

    // funzione per osservare lo stato del ViewModel e navigare di conseguenza
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
                // controllo ancora in corso: si aspetta, la ProgressBar resta visibile
            }

            is SplashUiState.VaiAlLogin -> {
                // rimuove la Splash dallo stack: da qui il tasto Indietro non deve tornarci
                val opzioni = navOptions {
                    popUpTo(R.id.splashFragment) { inclusive = true }
                }
                findNavController().navigate(R.id.nav_graph_auth, null, opzioni)
            }

            is SplashUiState.VaiAllaHome -> {
                // stessa logica di navigaAllaHomePerRuolo(), ma rimuove la Splash
                // (non il grafo auth) dallo stack, dato che si arriva da qui e non dal login.
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
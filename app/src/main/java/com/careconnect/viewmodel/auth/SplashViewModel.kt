package com.careconnect.viewmodel.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.careconnect.model.UserRole
import com.careconnect.repository.AuthRepository
import com.careconnect.repository.UserRepository
import com.careconnect.util.SessionCache
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// stato della schermata Splash: dice al fragment dove navigare oppure che
// il controllo è ancora in corso
sealed class SplashUiState {
    object Verifica : SplashUiState()
    object VaiAlLogin : SplashUiState()
    data class VaiAllaHome(val ruolo: UserRole) : SplashUiState()
}

// capisce il più velocemente possibile se l'utente ha già una sessione
// valida e, se sì, quale home mostrare, senza far vedere il login se non serve
class SplashViewModel(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val sessionCache: SessionCache
) : ViewModel() {

    private val _uiState = MutableStateFlow<SplashUiState>(SplashUiState.Verifica)
    val uiState: StateFlow<SplashUiState> = _uiState.asStateFlow()

    init {
        verificaSessione()
    }

    // funzione che controlla se esiste una sessione valida e decide dove navigare
    private fun verificaSessione() {
        // c'è una sessione Firebase? lettura sincrona, nessuna rete.
        val utenteCorrente = authRepository.utenteCorrente()
        if (utenteCorrente == null) {
            _uiState.value = SplashUiState.VaiAlLogin
            return
        }

        // se il ruolo è già in cache locale ho un percorso veloce (funziona anche offline)
        val ruoloInCache = sessionCache.getRuoloSalvato()
        if (ruoloInCache != null) {
            _uiState.value = SplashUiState.VaiAllaHome(ruoloInCache)
            return
        }

        // fallback raro: sessione valida ma cache vuota, si chiede il ruolo a Firestore
        viewModelScope.launch {
            userRepository.getUtente(utenteCorrente.uid).fold(
                onSuccess = { user ->
                    sessionCache.salvaRuolo(user.ruolo)
                    _uiState.value = SplashUiState.VaiAllaHome(user.ruolo)
                },
                onFailure = {
                    // sessione valida ma nessun profilo Firestore, si manda al login
                    _uiState.value = SplashUiState.VaiAlLogin
                }
            )
        }
    }
}

class SplashViewModelFactory(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val sessionCache: SessionCache
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SplashViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SplashViewModel(authRepository, userRepository, sessionCache) as T
        }
        throw IllegalArgumentException("ViewModel sconosciuto: ${modelClass.name}")
    }
}
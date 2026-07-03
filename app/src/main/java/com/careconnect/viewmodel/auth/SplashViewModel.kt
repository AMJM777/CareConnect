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

/**
 * Stato della schermata Splash: dice al Fragment dove navigare, oppure
 * che il controllo è ancora in corso (mentre aspettiamo Firestore, nel
 * caso raro in cui la cache locale non basta).
 */
sealed class SplashUiState {

    /** Controllo sessione in corso: la UI mostra solo un caricamento. */
    object Verifica : SplashUiState()

    /** Nessuna sessione valida: la UI naviga verso il login. */
    object VaiAlLogin : SplashUiState()

    /** Sessione valida e ruolo noto: la UI naviga verso la home corretta. */
    data class VaiAllaHome(val ruolo: UserRole) : SplashUiState()
}

/**
 * ViewModel della Splash. Unico scopo: capire, il più velocemente possibile,
 * se l'utente ha già una sessione valida e, in caso affermativo, quale home
 * mostrare — senza far vedere il login se non è necessario.
 */
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

    private fun verificaSessione() {
        // Passo 1: c'è una sessione Firebase? Lettura sincrona, nessuna rete.
        val utenteCorrente = authRepository.utenteCorrente()
        if (utenteCorrente == null) {
            _uiState.value = SplashUiState.VaiAlLogin
            return
        }

        // Passo 2: il ruolo è già in cache locale? Se sì, non serve
        // aspettare Firestore: percorso veloce, funziona anche offline.
        val ruoloInCache = sessionCache.getRuoloSalvato()
        if (ruoloInCache != null) {
            _uiState.value = SplashUiState.VaiAllaHome(ruoloInCache)
            return
        }

        // Passo 3 (fallback, caso raro): sessione Firebase valida ma cache
        // vuota. Chiediamo il ruolo a Firestore e lo salviamo in cache,
        // così i prossimi avvii useranno di nuovo il percorso veloce.
        viewModelScope.launch {
            userRepository.getUtente(utenteCorrente.uid).fold(
                onSuccess = { user ->
                    sessionCache.salvaRuolo(user.ruolo)
                    _uiState.value = SplashUiState.VaiAllaHome(user.ruolo)
                },
                onFailure = {
                    // Sessione Firebase valida ma nessun profilo Firestore
                    // trovato (es. registrazione Google mai completata):
                    // scelta sicura, mandiamo al login invece di tentare
                    // di ricostruire uno stato intermedio.
                    _uiState.value = SplashUiState.VaiAlLogin
                }
            )
        }
    }
}

/**
 * Factory manuale, stesso pattern già usato per AuthViewModelFactory:
 * nessun framework DI (Hilt) nel progetto.
 */
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
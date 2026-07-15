package com.careconnect.viewmodel.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.careconnect.model.AuthUser
import com.careconnect.model.User
import com.careconnect.model.UserRole
import com.careconnect.repository.AuthRepository
import com.careconnect.repository.UserRepository
import com.careconnect.util.SessionCache
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.careconnect.fcm.FcmTokenManager

// stato della schermata di autenticazione: Login e Registrazione lo condividono
sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()

    // il ruolo è incluso qui (non solo in AuthUser) perché serve subito dopo il login per scegliere la home
    data class Autenticato(val utente: AuthUser, val ruolo: UserRole) : AuthUiState()

    // primo accesso con Google: credenziali valide, ma il profilo Firestore non esiste ancora
    data class RichiestaRuoloGoogle(val utente: AuthUser) : AuthUiState()

    data class Errore(val eccezione: Throwable) : AuthUiState()
}

// ViewModel condiviso tra LoginFragment e RegistrazioneFragment. orchestra
// AuthRepository (credenziali Firebase), UserRepository (profilo Firestore)
// e SessionCache
class AuthViewModel(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val sessionCache: SessionCache
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    // funzione per il login con email e password: il ruolo va letto dal profilo Firestore già esistente
    fun loginConEmail(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            authRepository.loginConEmail(email, password).fold(
                onSuccess = { authUser -> completaConProfiloEsistente(authUser) },
                onFailure = { errore -> _uiState.value = AuthUiState.Errore(errore) }
            )
        }
    }

    // funzione per registrare un nuovo utente: crea la credenziale Firebase, poi salva il profilo su Firestore
    fun registraConEmail(nome: String, email: String, password: String, ruolo: UserRole) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            authRepository.registraConEmail(email, password).fold(
                onSuccess = { authUser -> salvaProfiloEConcludi(authUser, nome, ruolo) },
                onFailure = { errore -> _uiState.value = AuthUiState.Errore(errore) }
            )
        }
    }

    // funzione per login/registrazione con Google: Firebase le gestisce come
    // un'unica operazione, si distingue poi in base al profilo Firestore
    fun loginConGoogle(idToken: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            authRepository.loginConGoogle(idToken).fold(
                onSuccess = { authUser -> gestisciEsitoLoginGoogle(authUser) },
                onFailure = { errore -> _uiState.value = AuthUiState.Errore(errore) }
            )
        }
    }

    // secondo passo per un nuovo utente Google: nome e ruolo raccolti dalla UI, ora si salva il profilo
    fun completaRegistrazioneGoogle(nome: String, ruolo: UserRole) {
        val statoAttuale = _uiState.value
        if (statoAttuale !is AuthUiState.RichiestaRuoloGoogle) return

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            salvaProfiloEConcludi(statoAttuale.utente, nome, ruolo)
        }
    }

    // funzione per il logout: pulisce sia la sessione Firebase sia il ruolo
    // in cache locale, altrimenti un login successivo con un altro utente
    // sullo stesso dispositivo potrebbe leggere per errore il vecchio ruolo
    fun logout() {
        authRepository.logout()
        sessionCache.pulisci()
        _uiState.value = AuthUiState.Idle
    }

    private suspend fun completaConProfiloEsistente(authUser: AuthUser) {
        userRepository.getUtente(authUser.uid).fold(
            onSuccess = { user ->
                sessionCache.salvaRuolo(user.ruolo)
                salvaTokenFcm(authUser.uid)
                _uiState.value = AuthUiState.Autenticato(authUser, user.ruolo)
            },
            onFailure = { errore -> _uiState.value = AuthUiState.Errore(errore) }
        )
    }

    // funzione che, dopo un login Google, distingue utente esistente da primo accesso
    private suspend fun gestisciEsitoLoginGoogle(authUser: AuthUser) {
        userRepository.getUtente(authUser.uid).fold(
            onSuccess = { user ->
                sessionCache.salvaRuolo(user.ruolo)
                salvaTokenFcm(authUser.uid)
                _uiState.value = AuthUiState.Autenticato(authUser, user.ruolo)
            },
            onFailure = { errore ->
                // profilo non trovato = primo accesso, non un vero errore
                _uiState.value = if (errore is NoSuchElementException) {
                    AuthUiState.RichiestaRuoloGoogle(authUser)
                } else {
                    AuthUiState.Errore(errore)
                }
            }
        )
    }

    private suspend fun salvaProfiloEConcludi(authUser: AuthUser, nome: String, ruolo: UserRole) {
        val user = User(uid = authUser.uid, nome = nome, ruolo = ruolo)
        userRepository.salvaUtente(user).fold(
            onSuccess = {
                sessionCache.salvaRuolo(ruolo)
                salvaTokenFcm(authUser.uid)
                _uiState.value = AuthUiState.Autenticato(authUser, ruolo)
            },
            onFailure = { errore -> _uiState.value = AuthUiState.Errore(errore) }
        )
    }

    // funzione che salva il token FCM del dispositivo dopo l'autenticazione.
    // se fallisce non blocca il login (la push è una funzionalità extra)
    private suspend fun salvaTokenFcm(uid: String) {
        FcmTokenManager.tokenCorrente().onSuccess { token ->
            userRepository.aggiornaFcmToken(uid, token)
        }
    }
}

class AuthViewModelFactory(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val sessionCache: SessionCache
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AuthViewModel(authRepository, userRepository, sessionCache) as T
        }
        throw IllegalArgumentException("ViewModel sconosciuto: ${modelClass.name}")
    }
}
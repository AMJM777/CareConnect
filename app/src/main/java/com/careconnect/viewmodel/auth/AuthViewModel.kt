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

/**
 * Stato della schermata di autenticazione (Login e Registrazione condividono
 * questo stato tramite un unico AuthViewModel scoped all'Activity).
 */
sealed class AuthUiState {

    /** Nessuna operazione in corso, form pronto per l'input. */
    object Idle : AuthUiState()

    /** Login/registrazione/chiamata Firebase in corso: la UI mostra un loading. */
    object Loading : AuthUiState()

    /**
     * Login o registrazione completati con successo, profilo Firestore pronto.
     * Il ruolo è incluso qui (non solo in AuthUser) perché serve subito
     * dopo l'autenticazione per scegliere la home corretta da mostrare.
     */
    data class Autenticato(val utente: AuthUser, val ruolo: UserRole) : AuthUiState()

    /**
     * Caso speciale del login Google: le credenziali Firebase sono valide,
     * ma è il primo accesso e il profilo Firestore non esiste ancora.
     * La UI deve mostrare una schermata per far scegliere nome e ruolo
     * prima di considerare la registrazione conclusa.
     */
    data class RichiestaRuoloGoogle(val utente: AuthUser) : AuthUiState()

    /** Un'operazione è fallita: la UI legge il messaggio da mostrare all'utente. */
    data class Errore(val eccezione: Throwable) : AuthUiState()
}

/**
 * ViewModel condiviso tra LoginFragment e RegistrazioneFragment.
 * Orchestra AuthRepository (credenziali Firebase Auth), UserRepository
 * (profilo utente su Firestore) e SessionCache (ruolo salvato in locale,
 * usato dalla Fase 4 per l'auto-login senza dover rileggere Firestore
 * a ogni avvio dell'app): i tre restano indipendenti, è questo ViewModel
 * a sapere come combinarli.
 */
class AuthViewModel(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val sessionCache: SessionCache
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    /**
     * Login con email e password già registrate. A differenza della
     * registrazione, qui non conosciamo ancora il ruolo dell'utente:
     * va letto dal profilo Firestore già esistente prima di considerare
     * il login concluso, altrimenti non sapremmo dove navigare dopo.
     */
    fun loginConEmail(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            authRepository.loginConEmail(email, password).fold(
                onSuccess = { authUser -> completaConProfiloEsistente(authUser) },
                onFailure = { errore -> _uiState.value = AuthUiState.Errore(errore) }
            )
        }
    }

    /**
     * Registrazione con email e password: crea prima la credenziale Firebase Auth,
     * poi salva il profilo (nome + ruolo) su Firestore.
     */
    fun registraConEmail(nome: String, email: String, password: String, ruolo: UserRole) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            authRepository.registraConEmail(email, password).fold(
                onSuccess = { authUser -> salvaProfiloEConcludi(authUser, nome, ruolo) },
                onFailure = { errore -> _uiState.value = AuthUiState.Errore(errore) }
            )
        }
    }

    /**
     * Login/registrazione con Google. Firebase gestisce le due cose come
     * un'unica operazione: dopo il login controlliamo se esiste già un
     * profilo Firestore per capire se è un utente nuovo o di ritorno.
     */
    fun loginConGoogle(idToken: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            authRepository.loginConGoogle(idToken).fold(
                onSuccess = { authUser -> gestisciEsitoLoginGoogle(authUser) },
                onFailure = { errore -> _uiState.value = AuthUiState.Errore(errore) }
            )
        }
    }

    /**
     * Secondo passo per un nuovo utente Google: la UI ha raccolto nome e ruolo,
     * qui salviamo finalmente il profilo su Firestore.
     * Chiamabile solo se lo stato corrente è RichiestaRuoloGoogle.
     */
    fun completaRegistrazioneGoogle(nome: String, ruolo: UserRole) {
        val statoAttuale = _uiState.value
        if (statoAttuale !is AuthUiState.RichiestaRuoloGoogle) return

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            salvaProfiloEConcludi(statoAttuale.utente, nome, ruolo)
        }
    }

    /**
     * Logout: pulisce sia la sessione Firebase (AuthRepository) sia il ruolo
     * salvato in cache locale. Se dimenticassimo di pulire la cache, un
     * eventuale login successivo con un ALTRO utente (ruolo diverso) sul
     * medesimo dispositivo potrebbe leggere per errore il ruolo del
     * vecchio utente durante l'auto-login, prima ancora di interpellare Firestore.
     */
    fun logout() {
        authRepository.logout()
        sessionCache.pulisci()
        _uiState.value = AuthUiState.Idle
    }

    /** Legge il profilo Firestore di un utente che ha già un account (login, non registrazione). */
    private suspend fun completaConProfiloEsistente(authUser: AuthUser) {
        userRepository.getUtente(authUser.uid).fold(
            onSuccess = { user ->
                sessionCache.salvaRuolo(user.ruolo)
                _uiState.value = AuthUiState.Autenticato(authUser, user.ruolo)
            },
            onFailure = { errore -> _uiState.value = AuthUiState.Errore(errore) }
        )
    }

    private suspend fun gestisciEsitoLoginGoogle(authUser: AuthUser) {
        userRepository.getUtente(authUser.uid).fold(
            onSuccess = { user ->
                sessionCache.salvaRuolo(user.ruolo)
                _uiState.value = AuthUiState.Autenticato(authUser, user.ruolo)
            },
            onFailure = { errore ->
                // Profilo non trovato = primo accesso, non un vero errore.
                // Qualsiasi altra eccezione (es. rete) resta un errore reale.
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
                _uiState.value = AuthUiState.Autenticato(authUser, ruolo)
            },
            onFailure = { errore -> _uiState.value = AuthUiState.Errore(errore) }
        )
    }
}

/**
 * Factory manuale per creare AuthViewModel con le sue dipendenze.
 * Non usiamo un framework DI (Hilt) nel progetto: stessa scelta già
 * applicata per gli altri RepositoryImpl.
 */
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
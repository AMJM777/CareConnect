package com.careconnect.viewmodel.anziano

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.careconnect.model.User
import com.careconnect.repository.AuthRepository
import com.careconnect.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel della Dashboard Anziano.
 * Si occupa di: codice invito (FASE 6) e indirizzo (FASE 7, serve al
 * Volontario per sapere dove andare quando accetta una richiesta).
 */
class DashboardAnzianoViewModel(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _utente = MutableStateFlow<User?>(null)
    val utente: StateFlow<User?> = _utente.asStateFlow()

    private val _codiceInvito = MutableStateFlow<String?>(null)
    val codiceInvito: StateFlow<String?> = _codiceInvito.asStateFlow()

    private val _errore = MutableStateFlow<String?>(null)
    val errore: StateFlow<String?> = _errore.asStateFlow()

    private val _indirizzoSalvato = MutableStateFlow(false)
    val indirizzoSalvato: StateFlow<Boolean> = _indirizzoSalvato.asStateFlow()

    init {
        caricaProfilo()
        caricaCodiceInvito()
    }

    private fun caricaProfilo() {
        val uid = authRepository.utenteCorrente()?.uid
        if (uid == null) {
            _errore.value = "Sessione non valida"
            return
        }
        viewModelScope.launch {
            userRepository.getUtente(uid).fold(
                onSuccess = { utenteCaricato -> _utente.value = utenteCaricato },
                onFailure = { errore -> _errore.value = errore.message ?: "Impossibile caricare il profilo" }
            )
        }
    }

    private fun caricaCodiceInvito() {
        val uid = authRepository.utenteCorrente()?.uid
        if (uid == null) return
        viewModelScope.launch {
            userRepository.ottieniOCreaCodiceInvito(uid).fold(
                onSuccess = { codice -> _codiceInvito.value = codice },
                onFailure = { errore ->
                    _errore.value = errore.message ?: "Impossibile generare il codice invito"
                }
            )
        }
    }

    fun salvaIndirizzo(nuovoIndirizzo: String) {
        val utenteAttuale = _utente.value
        if (utenteAttuale == null) {
            _errore.value = "Profilo non ancora caricato"
            return
        }
        if (nuovoIndirizzo.isBlank()) {
            _errore.value = "Inserisci un indirizzo valido"
            return
        }
        val utenteAggiornato = utenteAttuale.copy(indirizzo = nuovoIndirizzo.trim())

        viewModelScope.launch {
            userRepository.salvaUtente(utenteAggiornato).fold(
                onSuccess = {
                    _utente.value = utenteAggiornato
                    _indirizzoSalvato.value = true
                },
                onFailure = { errore ->
                    _errore.value = errore.message ?: "Impossibile salvare l'indirizzo"
                }
            )
        }
    }

    fun erroreMostrato() {
        _errore.value = null
    }

    fun indirizzoSalvatoMostrato() {
        _indirizzoSalvato.value = false
    }
}

class DashboardAnzianoViewModelFactory(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DashboardAnzianoViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DashboardAnzianoViewModel(userRepository, authRepository) as T
        }
        throw IllegalArgumentException("ViewModel sconosciuto: ${modelClass.name}")
    }
}
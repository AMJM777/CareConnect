package com.careconnect.viewmodel.volontario

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
 * ViewModel del Profilo Volontario. Carica il profilo una sola volta
 * all'apertura (non è realtime) e gestisce logout + modifica della bio.
 */
class ProfiloVolontarioViewModel(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _utente = MutableStateFlow<User?>(null)
    val utente: StateFlow<User?> = _utente.asStateFlow()

    private val _errore = MutableStateFlow<String?>(null)
    val errore: StateFlow<String?> = _errore.asStateFlow()

    // Messaggio di conferma separato dall'errore: "bio salvata" non è un
    // errore, ma merita comunque un feedback visivo (Toast) all'utente.
    private val _bioSalvata = MutableStateFlow(false)
    val bioSalvata: StateFlow<Boolean> = _bioSalvata.asStateFlow()

    val email: String? = authRepository.utenteCorrente()?.email

    init {
        caricaProfilo()
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

    /**
     * Salva la nuova bio. Parte dall'utente già caricato in memoria e lo
     * copia con il nuovo testo: salvaUtente() fa un .set() completo, quindi
     * serve sempre l'oggetto User intero, non solo il campo cambiato.
     */
    fun salvaBio(nuovaBio: String) {
        val utenteAttuale = _utente.value
        if (utenteAttuale == null) {
            _errore.value = "Profilo non ancora caricato"
            return
        }
        val utenteAggiornato = utenteAttuale.copy(bio = nuovaBio.ifBlank { null })

        viewModelScope.launch {
            userRepository.salvaUtente(utenteAggiornato).fold(
                onSuccess = {
                    _utente.value = utenteAggiornato
                    _bioSalvata.value = true
                },
                onFailure = { errore ->
                    _errore.value = errore.message ?: "Impossibile salvare la descrizione"
                }
            )
        }
    }

    fun logout() {
        authRepository.logout()
    }

    fun erroreMostrato() {
        _errore.value = null
    }

    fun bioSalvataMostrata() {
        _bioSalvata.value = false
    }
}

class ProfiloVolontarioViewModelFactory(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProfiloVolontarioViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProfiloVolontarioViewModel(userRepository, authRepository) as T
        }
        throw IllegalArgumentException("ViewModel sconosciuto: ${modelClass.name}")
    }
}
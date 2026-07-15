package com.careconnect.viewmodel.volontario

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
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

// carica il profilo una sola volta (non realtime) e gestisce logout + modifica della bio
// nome e valutazione sono legati dall'xml con data binding tramite i liveData sottostanti
class ProfiloVolontarioViewModel(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    // tenuto in memoria come sorgente per salvaBio(): salvaUtente è un .set() completo, serve l'oggetto intero
    private var utenteCaricato: User? = null

    private val _nome = MutableLiveData("")
    val nome: LiveData<String> = _nome

    // testo già formattato: "valutazione: 4.5 / 5" oppure "valutazione: non ancora valutato"
    private val _valutazione = MutableLiveData("")
    val valutazione: LiveData<String> = _valutazione

    private val _bioIniziale = MutableLiveData<String>()
    val bioIniziale: LiveData<String> = _bioIniziale

    val email: String? = authRepository.utenteCorrente()?.email

    private val _errore = MutableStateFlow<String?>(null)
    val errore: StateFlow<String?> = _errore.asStateFlow()

    private val _bioSalvata = MutableStateFlow(false)
    val bioSalvata: StateFlow<Boolean> = _bioSalvata.asStateFlow()

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
                onSuccess = { utente -> mostraUtente(utente) },
                onFailure = { errore -> _errore.value = errore.message ?: "Impossibile caricare il profilo" }
            )
        }
    }

    private fun mostraUtente(utente: User) {
        utenteCaricato = utente
        _nome.value = utente.nome
        _valutazione.value = utente.ratingMedio
            ?.let { media -> "Valutazione: %.1f / 5".format(media) }
            ?: "Valutazione: non ancora valutato"
        _bioIniziale.value = utente.bio ?: ""
    }

    // funzione per salvare la nuova bio, a partire dall'utente già in memoria
    fun salvaBio(nuovaBio: String) {
        val utenteAttuale = utenteCaricato
        if (utenteAttuale == null) {
            _errore.value = "Profilo non ancora caricato"
            return
        }
        val utenteAggiornato = utenteAttuale.copy(bio = nuovaBio.ifBlank { null })

        viewModelScope.launch {
            userRepository.salvaUtente(utenteAggiornato).fold(
                onSuccess = {
                    utenteCaricato = utenteAggiornato
                    _bioSalvata.value = true
                },
                onFailure = { errore ->
                    _errore.value = errore.message ?: "Impossibile salvare la descrizione"
                }
            )
        }
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
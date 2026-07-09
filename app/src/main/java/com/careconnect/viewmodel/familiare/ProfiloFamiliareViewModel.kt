package com.careconnect.viewmodel.familiare

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.careconnect.repository.AuthRepository
import com.careconnect.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel del Profilo Familiare (FASE 6). Carica una sola volta (non
 * realtime: questi dati non cambiano mentre la schermata è aperta) il
 * proprio nome e quello dell'anziano collegato.
 *
 * DATA BINDING (lezione 9): essendo tutto in SOLA LETTURA, le due TextView
 * sono legate interamente dall'XML con @{}. I testi già pronti ("Sei
 * collegato/a come: ...", "Stai seguendo: ...") vengono composti qui col
 * solito schema MutableLiveData privato + LiveData pubblico, così il
 * Fragment non ha più codice per riempire le View.
 */
class ProfiloFamiliareViewModel(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    // Testi già pronti, legati dall'XML.
    private val _nomeFamiliareTesto = MutableLiveData("")
    val nomeFamiliareTesto: LiveData<String> = _nomeFamiliareTesto

    private val _nomeAnzianoTesto = MutableLiveData("")
    val nomeAnzianoTesto: LiveData<String> = _nomeAnzianoTesto

    // Evento "una tantum" (Toast): resta StateFlow, è un segnale momentaneo.
    private val _errore = MutableStateFlow<String?>(null)
    val errore: StateFlow<String?> = _errore.asStateFlow()

    init {
        caricaInfo()
    }

    private fun caricaInfo() {
        val uid = authRepository.utenteCorrente()?.uid
        if (uid == null) {
            _errore.value = "Sessione non valida"
            return
        }
        viewModelScope.launch {
            val familiare = userRepository.getUtente(uid).getOrElse {
                _errore.value = it.message ?: "Impossibile caricare il profilo"
                return@launch
            }
            val anzianoId = familiare.anzianoCollegatoId
            if (anzianoId == null) {
                _errore.value = "Nessun anziano collegato"
                return@launch
            }
            val anziano = userRepository.getUtente(anzianoId).getOrElse {
                _errore.value = it.message ?: "Impossibile caricare i dati dell'anziano"
                return@launch
            }
            // Componiamo i testi qui: da questo momento le TextView legate
            // nell'XML si aggiornano da sole.
            _nomeFamiliareTesto.value = "Sei collegato/a come: ${familiare.nome}"
            _nomeAnzianoTesto.value = "Stai seguendo: ${anziano.nome}"
        }
    }

    fun erroreMostrato() {
        _errore.value = null
    }
}

class ProfiloFamiliareViewModelFactory(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProfiloFamiliareViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProfiloFamiliareViewModel(userRepository, authRepository) as T
        }
        throw IllegalArgumentException("ViewModel sconosciuto: ${modelClass.name}")
    }
}
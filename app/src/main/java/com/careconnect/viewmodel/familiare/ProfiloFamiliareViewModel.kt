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

// carica una sola volta (non realtime) il proprio nome e quello dell'anziano collegato
// i due testi sono già pronti qui e legati dall'xml con data binding
class ProfiloFamiliareViewModel(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _nomeFamiliareTesto = MutableLiveData("")
    val nomeFamiliareTesto: LiveData<String> = _nomeFamiliareTesto

    private val _nomeAnzianoTesto = MutableLiveData("")
    val nomeAnzianoTesto: LiveData<String> = _nomeAnzianoTesto

    private val _errore = MutableStateFlow<String?>(null)
    val errore: StateFlow<String?> = _errore.asStateFlow()

    init {
        caricaInfo()
    }

    // funzione per caricare i dati del familiare e dell'anziano collegato, e comporre i testi da mostrare
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
            _nomeFamiliareTesto.value = "Sei collegato/a come: ${familiare.nome}"
            _nomeAnzianoTesto.value = "Stai seguendo: ${anziano.nome}"
        }
    }

    // funzione per segnalare che l'errore è stato mostrato e va nascosto
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
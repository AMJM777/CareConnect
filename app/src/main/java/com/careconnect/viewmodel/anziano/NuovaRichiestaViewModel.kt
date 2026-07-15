package com.careconnect.viewmodel.anziano

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.careconnect.model.Request
import com.careconnect.repository.RequestRepository
import com.careconnect.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class NuovaRichiestaUiState {
    object Idle : NuovaRichiestaUiState()
    object Loading : NuovaRichiestaUiState()
    data class Successo(val requestId: String) : NuovaRichiestaUiState()
    data class Errore(val eccezione: Throwable) : NuovaRichiestaUiState()
}

class NuovaRichiestaViewModel(
    private val requestRepository: RequestRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<NuovaRichiestaUiState>(NuovaRichiestaUiState.Idle)
    val uiState: StateFlow<NuovaRichiestaUiState> = _uiState.asStateFlow()

    // funzione per creare una richiesta: legge anche il profilo dell'anziano
    // per copiare nome e indirizzo sulla Request, che serviranno al volontario
    fun creaRichiesta(autoreId: String, tipo: String, descrizione: String) {
        viewModelScope.launch {
            _uiState.value = NuovaRichiestaUiState.Loading

            val autore = userRepository.getUtente(autoreId).getOrElse {
                _uiState.value = NuovaRichiestaUiState.Errore(it)
                return@launch
            }

            // senza indirizzo il volontario non saprebbe dove andare: meglio
            // bloccare qui che creare una richiesta incompleta
            val indirizzo = autore.indirizzo
            if (indirizzo.isNullOrBlank()) {
                _uiState.value = NuovaRichiestaUiState.Errore(
                    IllegalStateException(
                        "Imposta prima il tuo indirizzo dalla Home: serve al volontario per sapere dove venire"
                    )
                )
                return@launch
            }

            val nuovaRichiesta = Request(
                autoreId = autoreId,
                autoreNome = autore.nome,
                autoreIndirizzo = indirizzo,
                tipo = tipo,
                descrizione = descrizione
            )

            requestRepository.creaRichiesta(nuovaRichiesta).fold(
                onSuccess = { id -> _uiState.value = NuovaRichiestaUiState.Successo(id) },
                onFailure = { errore -> _uiState.value = NuovaRichiestaUiState.Errore(errore) }
            )
        }
    }

    // funzione per modificare tipo e descrizione di una richiesta esistente
    fun modificaRichiesta(requestId: String, tipo: String, descrizione: String) {
        viewModelScope.launch {
            _uiState.value = NuovaRichiestaUiState.Loading

            requestRepository.modificaRichiesta(requestId, tipo, descrizione).fold(
                onSuccess = { _uiState.value = NuovaRichiestaUiState.Successo(requestId) },
                onFailure = { errore -> _uiState.value = NuovaRichiestaUiState.Errore(errore) }
            )
        }
    }
}

class NuovaRichiestaViewModelFactory(
    private val requestRepository: RequestRepository,
    private val userRepository: UserRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NuovaRichiestaViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return NuovaRichiestaViewModel(requestRepository, userRepository) as T
        }
        throw IllegalArgumentException("ViewModel sconosciuto: ${modelClass.name}")
    }
}
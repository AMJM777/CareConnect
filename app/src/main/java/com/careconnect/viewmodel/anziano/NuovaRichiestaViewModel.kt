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

/**
 * ViewModel della schermata "Nuova richiesta" / "Modifica richiesta".
 * FASE 7: creaRichiesta() ora legge anche il profilo dell'Anziano per
 * denormalizzare nome e indirizzo sulla Request — il Volontario ne avrà
 * bisogno per sapere chi cercare e dove andare quando accetta.
 */
class NuovaRichiestaViewModel(
    private val requestRepository: RequestRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<NuovaRichiestaUiState>(NuovaRichiestaUiState.Idle)
    val uiState: StateFlow<NuovaRichiestaUiState> = _uiState.asStateFlow()

    fun creaRichiesta(autoreId: String, tipo: String, descrizione: String) {
        viewModelScope.launch {
            _uiState.value = NuovaRichiestaUiState.Loading

            val autore = userRepository.getUtente(autoreId).getOrElse {
                _uiState.value = NuovaRichiestaUiState.Errore(it)
                return@launch
            }

            // Blocco qui, non dopo: senza indirizzo il volontario che accetta
            // non saprebbe dove andare. Meglio impedire la richiesta che
            // crearla incompleta.
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

    /** Modalità modifica: tipo/descrizione possono cambiare, autore e indirizzo no. */
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
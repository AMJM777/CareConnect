package com.careconnect.viewmodel.anziano

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.careconnect.model.Request
import com.careconnect.repository.RequestRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Stato condiviso sia per "creazione" sia per "modifica" di una richiesta:
 * sono la stessa interazione con dati diversi, quindi un unico stato basta.
 */
sealed class NuovaRichiestaUiState {
    object Idle : NuovaRichiestaUiState()
    object Loading : NuovaRichiestaUiState()
    data class Successo(val requestId: String) : NuovaRichiestaUiState()
    data class Errore(val eccezione: Throwable) : NuovaRichiestaUiState()
}

/**
 * ViewModel della schermata "Nuova richiesta" / "Modifica richiesta"
 * (stesso Fragment, stesso ViewModel: creaRichiesta() per il primo caso,
 * modificaRichiesta() per il secondo, entrambe scrivono su RequestRepository).
 */
class NuovaRichiestaViewModel(
    private val requestRepository: RequestRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<NuovaRichiestaUiState>(NuovaRichiestaUiState.Idle)
    val uiState: StateFlow<NuovaRichiestaUiState> = _uiState.asStateFlow()

    fun creaRichiesta(autoreId: String, tipo: String, descrizione: String) {
        viewModelScope.launch {
            _uiState.value = NuovaRichiestaUiState.Loading

            val nuovaRichiesta = Request(
                autoreId = autoreId,
                tipo = tipo,
                descrizione = descrizione
            )

            requestRepository.creaRichiesta(nuovaRichiesta).fold(
                onSuccess = { id -> _uiState.value = NuovaRichiestaUiState.Successo(id) },
                onFailure = { errore -> _uiState.value = NuovaRichiestaUiState.Errore(errore) }
            )
        }
    }

    /** Modalità modifica: aggiorna una richiesta esistente invece di crearne una nuova. */
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
    private val requestRepository: RequestRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NuovaRichiestaViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return NuovaRichiestaViewModel(requestRepository) as T
        }
        throw IllegalArgumentException("ViewModel sconosciuto: ${modelClass.name}")
    }
}
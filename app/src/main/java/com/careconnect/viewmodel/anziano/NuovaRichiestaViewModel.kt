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

// ViewModel della schermata di MODIFICA di una richiesta esistente
class NuovaRichiestaViewModel (
    private val requestRepository: RequestRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<NuovaRichiestaUiState>(NuovaRichiestaUiState.Idle)
    val uiState: StateFlow<NuovaRichiestaUiState> = _uiState.asStateFlow()

    // modifica tipo e descrizione di una richiesta esistente (permesso solo se APERTA)
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
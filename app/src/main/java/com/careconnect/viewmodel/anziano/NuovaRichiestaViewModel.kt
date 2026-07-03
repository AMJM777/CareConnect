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
 * Stato della schermata "Nuova richiesta": dice al Fragment cosa mostrare
 * (form pronto, caricamento, successo con id creato, o errore).
 */
sealed class NuovaRichiestaUiState {
    object Idle : NuovaRichiestaUiState()
    object Loading : NuovaRichiestaUiState()
    data class Successo(val requestId: String) : NuovaRichiestaUiState()
    data class Errore(val eccezione: Throwable) : NuovaRichiestaUiState()
}

/**
 * ViewModel della schermata "Nuova richiesta". Riceve dal Fragment i dati
 * già validati (tipo e descrizione non vuoti) e li scrive su Firestore
 * tramite RequestRepository, che si occupa già di generare id e stato
 * iniziale (RequestStatus.APERTA di default, vedi Request.kt).
 */
class NuovaRichiestaViewModel(
    private val requestRepository: RequestRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<NuovaRichiestaUiState>(NuovaRichiestaUiState.Idle)
    val uiState: StateFlow<NuovaRichiestaUiState> = _uiState.asStateFlow()

    /**
     * Crea la richiesta. autoreId è l'uid dell'anziano loggato (passato dal
     * Fragment, che lo legge da AuthRepository: questo ViewModel non deve
     * sapere nulla di autenticazione, solo di richieste).
     */
    fun creaRichiesta(autoreId: String, tipo: String, descrizione: String) {
        viewModelScope.launch {
            _uiState.value = NuovaRichiestaUiState.Loading

            // stato e timestampCreazione usano i default già definiti in
            // Request.kt (RequestStatus.APERTA, Timestamp.now()): non li
            // impostiamo qui per non duplicare quella logica.
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
}

/**
 * Factory manuale, stesso pattern già usato per AuthViewModelFactory
 * e SplashViewModelFactory: nessun framework DI (Hilt) nel progetto.
 */
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
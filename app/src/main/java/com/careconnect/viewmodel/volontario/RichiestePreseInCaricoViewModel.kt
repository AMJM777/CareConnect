package com.careconnect.viewmodel.volontario

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.careconnect.model.Request
import com.careconnect.model.RequestStatus
import com.careconnect.repository.AuthRepository
import com.careconnect.repository.RequestRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel della schermata "Le mie richieste prese in carico": mostra in
 * tempo reale le richieste PRESA_IN_CARICO/COMPLETATA_DAL_VOLONTARIO del
 * volontario loggato, e gestisce "Segna come completata" e "Rilascia".
 */
class RichiestePreseInCaricoViewModel(
    private val requestRepository: RequestRepository,
    authRepository: AuthRepository
) : ViewModel() {

    val richieste: StateFlow<List<Request>> = run {
        val volontarioId = authRepository.utenteCorrente()?.uid

        val flowRichieste = if (volontarioId != null) {
            requestRepository.osservaRichiestePerVolontario(volontarioId)
        } else {
            emptyFlow()
        }

        flowRichieste.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    // Messaggio di errore da mostrare come Toast, se una delle due azioni fallisce.
    private val _errore = MutableStateFlow<String?>(null)
    val errore: StateFlow<String?> = _errore.asStateFlow()

    fun segnaCompletata(requestId: String) {
        viewModelScope.launch {
            // nuovoVolontarioId non passato: resta quello già presente sul
            // documento, qui cambia solo lo stato.
            requestRepository.aggiornaStato(requestId, RequestStatus.COMPLETATA_DAL_VOLONTARIO).fold(
                onSuccess = { /* la UI si aggiorna da sola: Flow realtime */ },
                onFailure = { errore ->
                    _errore.value = errore.message ?: "Impossibile segnare la richiesta come completata"
                }
            )
        }
    }

    fun rilasciaRichiesta(requestId: String) {
        viewModelScope.launch {
            // Tornare ad APERTA con volontarioId=null: il repository si
            // occupa già di azzerare il campo (vedi RequestRepositoryImpl).
            requestRepository.aggiornaStato(requestId, RequestStatus.APERTA).fold(
                onSuccess = { /* la UI si aggiorna da sola: Flow realtime */ },
                onFailure = { errore ->
                    _errore.value = errore.message ?: "Impossibile rilasciare la richiesta"
                }
            )
        }
    }

    fun erroreMostrato() {
        _errore.value = null
    }
}

class RichiestePreseInCaricoViewModelFactory(
    private val requestRepository: RequestRepository,
    private val authRepository: AuthRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RichiestePreseInCaricoViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RichiestePreseInCaricoViewModel(requestRepository, authRepository) as T
        }
        throw IllegalArgumentException("ViewModel sconosciuto: ${modelClass.name}")
    }
}
package com.careconnect.viewmodel.anziano

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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// mostra in tempo reale le richieste dell'anziano e gestisce l'azione "annulla"

class MieRichiesteViewModel(
    private val requestRepository: RequestRepository,
    authRepository: AuthRepository
) : ViewModel() {

    val richieste: StateFlow<List<Request>> = run {
        val anzianoId = authRepository.utenteCorrente()?.uid

        val flowRichieste = if (anzianoId != null) {
            requestRepository.osservaRichiestePerAnziano(anzianoId)
        } else {
            emptyFlow()
        }

        // ordinate per data di creazione decrescente: vedi commento in
        // RichiesteDisponibiliViewModel, stesso motivo (query senza orderBy).
        flowRichieste
            .map { lista -> lista.sortedByDescending { it.timestampCreazione.seconds } }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )
    }

    // null = nessun errore da mostrare. il Fragment lo resetta dopo averlo mostrato
    private val _erroreAnnullamento = MutableStateFlow<String?>(null)
    val erroreAnnullamento: StateFlow<String?> = _erroreAnnullamento.asStateFlow()

    // funzione per annullare una richiesta già presa in carico: transizione di
    // stato (Update), NON una Delete. Si conserva lo storico perché un
    // volontario è già coinvolto.
    fun annullaRichiesta(requestId: String) {
        viewModelScope.launch {
            requestRepository.aggiornaStato(requestId, RequestStatus.ANNULLATA).fold(
                onSuccess = { /* la UI si aggiorna da sola: osservaRichiestePerAnziano è realtime */ },
                onFailure = { errore ->
                    _erroreAnnullamento.value = errore.message ?: "Impossibile annullare la richiesta"
                }
            )
        }
    }

    // funzione per eliminare definitivamente una richiesta ancora APERTA (vera
    // Delete): nessun volontario l'ha mai vista/accettata, quindi non c'è
    // storico da preservare.
    fun eliminaRichiesta(requestId: String) {
        viewModelScope.launch {
            requestRepository.eliminaRichiesta(requestId).fold(
                onSuccess = { /* la UI si aggiorna da sola: osservaRichiestePerAnziano è realtime */ },
                onFailure = { errore ->
                    _erroreAnnullamento.value = errore.message ?: "Impossibile eliminare la richiesta"
                }
            )
        }
    }

    // funzione per segnalare che l'errore è stato mostrato e va nascosto
    fun erroreMostrato() {
        _erroreAnnullamento.value = null
    }
}

class MieRichiesteViewModelFactory(
    private val requestRepository: RequestRepository,
    private val authRepository: AuthRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MieRichiesteViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MieRichiesteViewModel(requestRepository, authRepository) as T
        }
        throw IllegalArgumentException("ViewModel sconosciuto: ${modelClass.name}")
    }
}
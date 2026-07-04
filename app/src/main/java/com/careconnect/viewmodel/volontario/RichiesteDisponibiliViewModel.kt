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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel della schermata "Richieste disponibili": mostra in tempo reale
 * tutte le richieste APERTA e gestisce l'azione "Prendi in carico".
 */
class RichiesteDisponibiliViewModel(
    private val requestRepository: RequestRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    val richieste: StateFlow<List<Request>> = requestRepository.osservaRichiesteAperte()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Messaggio di errore da mostrare come Toast (es. "richiesta già presa in
    // carico da un altro volontario", generato dalla Transaction del
    // repository se due volontari sono stati troppo rapidi). Stesso schema
    // già usato in MieRichiesteViewModel per l'annullamento.
    private val _errorePresaInCarico = MutableStateFlow<String?>(null)
    val errorePresaInCarico: StateFlow<String?> = _errorePresaInCarico.asStateFlow()

    fun prendiInCarico(requestId: String) {
        val volontarioId = authRepository.utenteCorrente()?.uid
        if (volontarioId == null) {
            _errorePresaInCarico.value = "Sessione non valida, effettua di nuovo l'accesso"
            return
        }

        viewModelScope.launch {
            requestRepository.aggiornaStato(
                requestId = requestId,
                nuovoStato = RequestStatus.PRESA_IN_CARICO,
                nuovoVolontarioId = volontarioId
            ).fold(
                onSuccess = { /* la UI si aggiorna da sola: osservaRichiesteAperte è realtime */ },
                onFailure = { errore ->
                    _errorePresaInCarico.value = errore.message ?: "Impossibile prendere in carico la richiesta"
                }
            )
        }
    }

    fun erroreMostrato() {
        _errorePresaInCarico.value = null
    }
}

class RichiesteDisponibiliViewModelFactory(
    private val requestRepository: RequestRepository,
    private val authRepository: AuthRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RichiesteDisponibiliViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RichiesteDisponibiliViewModel(requestRepository, authRepository) as T
        }
        throw IllegalArgumentException("ViewModel sconosciuto: ${modelClass.name}")
    }
}
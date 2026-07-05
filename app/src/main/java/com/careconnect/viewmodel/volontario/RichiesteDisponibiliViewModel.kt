package com.careconnect.viewmodel.volontario

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.careconnect.model.Request
import com.careconnect.model.RequestStatus
import com.careconnect.repository.AuthRepository
import com.careconnect.repository.RequestRepository
import com.careconnect.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel della schermata "Richieste disponibili": mostra in tempo reale
 * tutte le richieste APERTA e gestisce l'azione "Prendi in carico".
 *
 * FASE 7: prendiInCarico() ora legge anche il proprio nome da UserRepository
 * prima di chiamare aggiornaStato(), per scrivere volontarioNome sulla
 * Request (Opzione B: il repository resta "puro", legge/scrive solo
 * "requests", il ViewModel orchestra la lettura da "users").
 */
class RichiesteDisponibiliViewModel(
    private val requestRepository: RequestRepository,
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    val richieste: StateFlow<List<Request>> = requestRepository.osservaRichiesteAperte()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _errorePresaInCarico = MutableStateFlow<String?>(null)
    val errorePresaInCarico: StateFlow<String?> = _errorePresaInCarico.asStateFlow()

    fun prendiInCarico(requestId: String) {
        val volontarioId = authRepository.utenteCorrente()?.uid
        if (volontarioId == null) {
            _errorePresaInCarico.value = "Sessione non valida, effettua di nuovo l'accesso"
            return
        }

        viewModelScope.launch {
            // Una sola lettura in più rispetto a prima: il nome del volontario
            // stesso, che ci serve solo per denormalizzarlo sulla Request.
            val volontario = userRepository.getUtente(volontarioId).getOrElse {
                _errorePresaInCarico.value = it.message ?: "Impossibile leggere il tuo profilo"
                return@launch
            }

            requestRepository.aggiornaStato(
                requestId = requestId,
                nuovoStato = RequestStatus.PRESA_IN_CARICO,
                nuovoVolontarioId = volontarioId,
                nuovoVolontarioNome = volontario.nome
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
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RichiesteDisponibiliViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RichiesteDisponibiliViewModel(requestRepository, userRepository, authRepository) as T
        }
        throw IllegalArgumentException("ViewModel sconosciuto: ${modelClass.name}")
    }
}
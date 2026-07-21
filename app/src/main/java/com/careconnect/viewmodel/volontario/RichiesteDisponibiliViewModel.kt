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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// mostra in tempo reale tutte le richieste aperte e gestisce "prendi in carico"
// legge anche il proprio nome prima di aggiornare lo stato, per scrivere volontarioNome sulla richiesta
class RichiesteDisponibiliViewModel(
    private val requestRepository: RequestRepository,
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    // ordinate per data di creazione decrescente (più recente in cima): la query
    // Firestore non ha orderBy (per non richiedere un indice composito), quindi
    // l'ordinamento va fatto qui lato app, altrimenti l'ordine non è garantito
    // e può cambiare a ogni snapshot.
    //
    // Il filtro su tipo.isNotBlank() scarta eventuali snapshot transitori/
    // incompleti: questa query osserva TUTTE le richieste aperte di TUTTI gli
    // anziani (a differenza, es., di quella del familiare che ne guarda uno
    // solo), quindi è la più esposta a un raro snapshot intermedio del
    // listener realtime durante una risincronizzazione. Una richiesta creata
    // dal form ha sempre tipo non vuoto (validato prima dell'invio): se
    // arriva un elemento con tipo vuoto non è mai una richiesta vera, va
    // ignorato finché non arriva la versione completa.
    val richieste: StateFlow<List<Request>> = requestRepository.osservaRichiesteAperte()
        .map { lista ->
            lista.filter { it.tipo.isNotBlank() }
                .sortedByDescending { it.timestampCreazione.seconds }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _errorePresaInCarico = MutableStateFlow<String?>(null)
    val errorePresaInCarico: StateFlow<String?> = _errorePresaInCarico.asStateFlow()

    // funzione per prendere in carico una richiesta: legge il proprio nome e poi aggiorna lo stato
    fun prendiInCarico(requestId: String) {
        val volontarioId = authRepository.utenteCorrente()?.uid
        if (volontarioId == null) {
            _errorePresaInCarico.value = "Sessione non valida, effettua di nuovo l'accesso"
            return
        }

        viewModelScope.launch {
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
                onSuccess = { /* la ui si aggiorna da sola: flow realtime */ },
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
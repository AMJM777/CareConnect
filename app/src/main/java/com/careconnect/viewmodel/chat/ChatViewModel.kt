package com.careconnect.viewmodel.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.careconnect.model.Message
import com.careconnect.repository.AuthRepository
import com.careconnect.repository.MessageRepository
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// gestisce la chat di una singola richiesta: osserva i messaggi in tempo reale e invia i nuovi messaggi
class ChatViewModel(
    private val messageRepository: MessageRepository,
    authRepository: AuthRepository,
    private val requestId: String,
    private val anzianoId: String,
    private val volontarioId: String
) : ViewModel() {

    // uid dell'utente corrente, sarà l'anzianoId o il volontarioId:
    // serve a marcare i messaggi come "miei" e a firmare quelli inviati
    val uidCorrente: String = authRepository.utenteCorrente()?.uid ?: ""

    private val _errore = MutableStateFlow<String?>(null)
    val errore: StateFlow<String?> = _errore.asStateFlow()

    // chi sta guardando decide il filtro che soddisfa le security rules:
    // - volontario: filtro volontarioId == proprio uid
    // - anziano : filtro anzianoId == proprio uid (anzianoId == uidCorrente)
    // - garante : filtro anzianoId == anziano della  richiesta
    private val campoUtente = if (uidCorrente == volontarioId) "volontarioId" else "anzianoId"
    private val valoreUtente = if (uidCorrente == volontarioId) volontarioId else anzianoId

    // lista messaggi ordinata cronologicamente
    val messaggi: StateFlow<List<Message>> =
        messageRepository.osservaMessaggiPerRichiesta(requestId, campoUtente, valoreUtente)
            .map { lista -> lista.sortedBy { it.timestamp.seconds } }
            // se il listener fallisce (es. permessi negati) non far crashare
            // la schermata: segnala l'errore e mostra una lista vuota
            .catch { e ->
                _errore.value = e.message ?: "Impossibile caricare i messaggi"
                emit(emptyList())
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    // invia un messaggio: ignora testo vuoto, firma con l'uid corrente
    fun inviaMessaggio(testo: String) {
        val testoPulito = testo.trim()
        if (testoPulito.isEmpty()) return

        val messaggio = Message(
            requestId = requestId,
            anzianoId = anzianoId,
            volontarioId = volontarioId,
            mittenteId = uidCorrente,
            testo = testoPulito,
            timestamp = Timestamp.now()
        )
        viewModelScope.launch {
            messageRepository.inviaMessaggio(messaggio).fold(
                onSuccess = { /* la ui si aggiorna da sola: flow realtime */ },
                onFailure = { e ->
                    _errore.value = e.message ?: "Impossibile inviare il messaggio"
                }
            )
        }
    }

    fun erroreMostrato() {
        _errore.value = null
    }
}

class ChatViewModelFactory(
    private val messageRepository: MessageRepository,
    private val authRepository: AuthRepository,
    private val requestId: String,
    private val anzianoId: String,
    private val volontarioId: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ChatViewModel(
                messageRepository, authRepository, requestId, anzianoId, volontarioId
            ) as T
        }
        throw IllegalArgumentException("ViewModel sconosciuto: ${modelClass.name}")
    }
}
package com.careconnect.viewmodel.familiare

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.careconnect.model.Rating
import com.careconnect.model.Request
import com.careconnect.model.SosAlert
import com.careconnect.model.SosStatus
import com.careconnect.repository.AuthRepository
import com.careconnect.repository.RatingRepository
import com.careconnect.repository.RequestRepository
import com.careconnect.repository.SosRepository
import com.careconnect.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// mostra in tempo reale tutte le richieste dell'anziano collegato e gestisce
// la conferma finale con valutazione, oltre agli alert sos di questo familiare
class AttivitaFamiliareViewModel(
    private val requestRepository: RequestRepository,
    private val ratingRepository: RatingRepository,
    private val sosRepository: SosRepository,
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _richieste = MutableStateFlow<List<Request>>(emptyList())
    val richieste: StateFlow<List<Request>> = _richieste.asStateFlow()

    // solo l'alert attivo più recente: se ce ne fosse più di uno si mostra un banner alla volta
    private val _sosAttivo = MutableStateFlow<SosAlert?>(null)
    val sosAttivo: StateFlow<SosAlert?> = _sosAttivo.asStateFlow()

    private val _errore = MutableStateFlow<String?>(null)
    val errore: StateFlow<String?> = _errore.asStateFlow()

    private val familiareId: String? = authRepository.utenteCorrente()?.uid

    init {
        osservaRichiesteAnziano()
        osservaSos()
    }

    // funzione per osservare in tempo reale le richieste dell'anziano collegato a questo familiare
    private fun osservaRichiesteAnziano() {
        val uid = familiareId
        if (uid == null) {
            _errore.value = "Sessione non valida"
            return
        }
        viewModelScope.launch {
            val familiare = userRepository.getUtente(uid).getOrNull()
            val anzianoId = familiare?.anzianoCollegatoId
            if (anzianoId == null) {
                _errore.value = "Nessun anziano collegato"
                return@launch
            }
            requestRepository.osservaRichiestePerAnziano(anzianoId).collect { lista ->
                _richieste.value = lista
            }
        }
    }

    // funzione per osservare in tempo reale gli alert sos di questo familiare e tenere solo il più recente attivo
    private fun osservaSos() {
        val uid = familiareId ?: return
        viewModelScope.launch {
            sosRepository.osservaAlertPerFamiliare(uid).collect { alerts ->
                _sosAttivo.value = alerts
                    .filter { it.stato == SosStatus.ATTIVO }
                    .maxByOrNull { it.timestampCreazione.seconds }
            }
        }
    }

    // funzione per chiudere l'alert sos: l'anziano è stato preso in carico dal familiare
    fun chiudiSos(alertId: String) {
        viewModelScope.launch {
            sosRepository.aggiornaStato(alertId, SosStatus.CHIUSO).onFailure { errore ->
                _errore.value = errore.message ?: "Impossibile chiudere l'allarme"
            }
        }
    }

    // funzione per confermare il completamento di una richiesta e salvare la valutazione
    fun confermaEValuta(richiesta: Request, stelle: Int, commento: String?) {
        val valutatoreId = familiareId ?: return
        val volontarioId = richiesta.volontarioId ?: return

        viewModelScope.launch {
            val rating = Rating(
                requestId = richiesta.id,
                volontarioId = volontarioId,
                stelle = stelle,
                commento = commento?.takeIf { it.isNotBlank() },
                valutatoreId = valutatoreId
            )
            ratingRepository.creaRatingEConfermaRichiesta(rating).fold(
                onSuccess = {
                    // il rating è già salvato: se questo secondo passo fallisse
                    // non si mostra un errore, per non confondere l'utente
                    userRepository.aggiornaRatingMedio(volontarioId)
                },
                onFailure = { errore ->
                    _errore.value = errore.message ?: "Impossibile confermare la richiesta"
                }
            )
        }
    }

    // funzione per segnalare che l'errore è stato mostrato e va nascosto
    fun erroreMostrato() {
        _errore.value = null
    }
}

class AttivitaFamiliareViewModelFactory(
    private val requestRepository: RequestRepository,
    private val ratingRepository: RatingRepository,
    private val sosRepository: SosRepository,
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AttivitaFamiliareViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AttivitaFamiliareViewModel(
                requestRepository, ratingRepository, sosRepository, userRepository, authRepository
            ) as T
        }
        throw IllegalArgumentException("ViewModel sconosciuto: ${modelClass.name}")
    }
}
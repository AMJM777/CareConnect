package com.careconnect.viewmodel.familiare

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.careconnect.model.Rating
import com.careconnect.model.Request
import com.careconnect.repository.AuthRepository
import com.careconnect.repository.RatingRepository
import com.careconnect.repository.RequestRepository
import com.careconnect.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel della schermata Attività del Familiare: mostra in tempo reale
 * tutte le richieste dell'anziano collegato (stato attuale + storico in
 * un'unica lista) e gestisce la conferma finale con valutazione.
 */
class AttivitaFamiliareViewModel(
    private val requestRepository: RequestRepository,
    private val ratingRepository: RatingRepository,
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _richieste = MutableStateFlow<List<Request>>(emptyList())
    val richieste: StateFlow<List<Request>> = _richieste.asStateFlow()

    private val _errore = MutableStateFlow<String?>(null)
    val errore: StateFlow<String?> = _errore.asStateFlow()

    private val familiareId: String? = authRepository.utenteCorrente()?.uid

    init {
        osservaRichiesteAnziano()
    }

    private fun osservaRichiesteAnziano() {
        val uid = familiareId
        if (uid == null) {
            _errore.value = "Sessione non valida"
            return
        }
        viewModelScope.launch {
            // Un'unica lettura per sapere QUALE anziano osservare, poi il
            // Flow realtime prende il sopravvento: non serve rileggere
            // questo dato più e più volte, il familiare non cambia
            // assistito mentre questa schermata è aperta.
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

    /** Conferma il completamento e salva la valutazione (stelle 1..5 + commento facoltativo). */
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
                onSuccess = { /* la UI si aggiorna da sola: Flow realtime */ },
                onFailure = { errore ->
                    _errore.value = errore.message ?: "Impossibile confermare la richiesta"
                }
            )
        }
    }

    fun erroreMostrato() {
        _errore.value = null
    }
}

class AttivitaFamiliareViewModelFactory(
    private val requestRepository: RequestRepository,
    private val ratingRepository: RatingRepository,
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AttivitaFamiliareViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AttivitaFamiliareViewModel(requestRepository, ratingRepository, userRepository, authRepository) as T
        }
        throw IllegalArgumentException("ViewModel sconosciuto: ${modelClass.name}")
    }
}
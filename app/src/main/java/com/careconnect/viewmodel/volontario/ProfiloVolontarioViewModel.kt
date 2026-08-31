package com.careconnect.viewmodel.volontario

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.careconnect.model.Rating
import com.careconnect.model.User
import com.careconnect.repository.AuthRepository
import com.careconnect.repository.RatingRepository
import com.careconnect.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// carica il profilo una sola volta (non realtime) e gestisce logout + modifica della bio
// nome e valutazione sono legati dall'xml con data binding tramite i liveData sottostanti
class ProfiloVolontarioViewModel(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
    private val ratingRepository: RatingRepository
) : ViewModel() {

    // tenuto in memoria come sorgente per salvaBio(): salvaUtente è un .set() completo, serve l'oggetto intero
    private var utenteCaricato: User? = null

    private val _nome = MutableLiveData("")
    val nome: LiveData<String> = _nome

    // descrizione vocale delle stelle per lo screen reader (es. "Valutazione 4,5 su 5")
    private val _valutazione = MutableLiveData("")
    val valutazione: LiveData<String> = _valutazione

    // valore numerico per la RatingBar (0 se non ancora valutato)
    private val _ratingStelle = MutableLiveData(0f)
    val ratingStelle: LiveData<Float> = _ratingStelle

    // true solo se il volontario ha già ricevuto almeno una valutazione
    private val _haValutazione = MutableLiveData(false)
    val haValutazione: LiveData<Boolean> = _haValutazione

    private val _bioIniziale = MutableLiveData<String>()
    val bioIniziale: LiveData<String> = _bioIniziale

    // commenti ricevuti (solo le valutazioni con testo), mostrati sotto le stelle
    private val _recensioni = MutableLiveData<List<Rating>>(emptyList())
    val recensioni: LiveData<List<Rating>> = _recensioni

    val email: String? = authRepository.utenteCorrente()?.email

    private val _errore = MutableStateFlow<String?>(null)
    val errore: StateFlow<String?> = _errore.asStateFlow()

    private val _bioSalvata = MutableStateFlow(false)
    val bioSalvata: StateFlow<Boolean> = _bioSalvata.asStateFlow()

    init {
        caricaProfilo()
    }

    private fun caricaProfilo() {
        val uid = authRepository.utenteCorrente()?.uid
        if (uid == null) {
            _errore.value = "Sessione non valida"
            return
        }
        viewModelScope.launch {
            userRepository.getUtente(uid).fold(
                onSuccess = { utente -> mostraUtente(utente) },
                onFailure = { errore -> _errore.value = errore.message ?: "Impossibile caricare il profilo" }
            )
        }
    }

        private fun mostraUtente(utente: User) {
        utenteCaricato = utente
        _nome.value = utente.nome

        val media = utente.ratingMedio
        _haValutazione.value = media != null
        _ratingStelle.value = media?.toFloat() ?: 0f
        _valutazione.value = media
            ?.let { "Valutazione %.1f su 5".format(it).replace('.', ',') }
            ?: ""

        _bioIniziale.value = utente.bio ?: ""

        caricaRecensioni(utente.uid)
    }

    // carica i commenti ricevuti dal volontario (solo le valutazioni con testo)
    private fun caricaRecensioni(volontarioId: String) {
        viewModelScope.launch {
            _recensioni.value = ratingRepository.getRatingsPerVolontario(volontarioId).getOrNull()
                ?.filter { !it.commento.isNullOrBlank() }
                ?: emptyList()
        }
    }

    // funzione per salvare la nuova bio, a partire dall'utente già in memoria
    fun salvaBio(nuovaBio: String) {
        val utenteAttuale = utenteCaricato
        if (utenteAttuale == null) {
            _errore.value = "Profilo non ancora caricato"
            return
        }
        val utenteAggiornato = utenteAttuale.copy(bio = nuovaBio.ifBlank { null })

        viewModelScope.launch {
            userRepository.salvaUtente(utenteAggiornato).fold(
                onSuccess = {
                    utenteCaricato = utenteAggiornato
                    _bioSalvata.value = true
                },
                onFailure = { errore ->
                    _errore.value = errore.message ?: "Impossibile salvare la descrizione"
                }
            )
        }
    }

    fun erroreMostrato() {
        _errore.value = null
    }

    fun bioSalvataMostrata() {
        _bioSalvata.value = false
    }
}

class ProfiloVolontarioViewModelFactory(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
    private val ratingRepository: RatingRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProfiloVolontarioViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProfiloVolontarioViewModel(userRepository, authRepository, ratingRepository) as T
        }
        throw IllegalArgumentException("ViewModel sconosciuto: ${modelClass.name}")
    }
}
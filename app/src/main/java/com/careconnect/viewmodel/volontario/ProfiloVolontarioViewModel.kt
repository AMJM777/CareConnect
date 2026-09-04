package com.careconnect.viewmodel.volontario

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.careconnect.model.User
import com.careconnect.repository.AuthRepository
import com.careconnect.repository.RatingRepository
import com.careconnect.repository.RequestRepository
import com.careconnect.repository.UserRepository
import com.careconnect.util.RecensioneFormat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// modello di presentazione di un commento ricevuto: testo + etichetta "Nome R. · Tipo"
data class RecensioneUi(
    val commento: String,
    val etichetta: String
)

// carica il profilo una sola volta (non realtime) e gestisce logout + modifica della bio
// nome e valutazione sono legati dall'xml con data binding tramite i liveData sottostanti
class ProfiloVolontarioViewModel(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
    private val ratingRepository: RatingRepository,
    private val requestRepository: RequestRepository
) : ViewModel() {

    // tenuto in memoria come sorgente per salvaBio(): salvaUtente è un .set() completo, serve l'oggetto intero
    private var utenteCaricato: User? = null

    private val _nome = MutableLiveData("")
    val nome: LiveData<String> = _nome

    // descrizione vocale delle stelle per lo screen reader
    private val _valutazione = MutableLiveData("")
    val valutazione: LiveData<String> = _valutazione

    // valore numerico per la RatingBar (0 se non ancora valutato)
    private val _ratingStelle = MutableLiveData(0f)
    val ratingStelle: LiveData<Float> = _ratingStelle

    // media come numero accanto alle stelle
    private val _ratingNumero = MutableLiveData("")
    val ratingNumero: LiveData<String> = _ratingNumero

    // conteggio del numero delle valutazioni
    private val _numeroValutazioni = MutableLiveData("")
    val numeroValutazioni: LiveData<String> = _numeroValutazioni

    // true solo se il volontario ha già ricevuto almeno una valutazione
    private val _haValutazione = MutableLiveData(false)
    val haValutazione: LiveData<Boolean> = _haValutazione

    private val _bioIniziale = MutableLiveData<String>()
    val bioIniziale: LiveData<String> = _bioIniziale

    // commenti ricevuti (solo le valutazioni con testo), arricchiti con nome e tipo
    private val _recensioni = MutableLiveData<List<RecensioneUi>>(emptyList())
    val recensioni: LiveData<List<RecensioneUi>> = _recensioni

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
        _ratingNumero.value = media?.let { "%.1f".format(it).replace('.', ',') } ?: ""
        _valutazione.value = media
            ?.let { "Valutazione %.1f su 5".format(it).replace('.', ',') }
            ?: ""

        _bioIniziale.value = utente.bio ?: ""

        caricaRecensioni(utente.uid)
    }

    // carica i commenti ricevuti dal volontario (solo le valutazioni con testo),
    // arricchendoli con il nome abbreviato dell'autore e il tipo di richiesta
    private fun caricaRecensioni(volontarioId: String) {
        viewModelScope.launch {
            val tutte = ratingRepository.getRatingsPerVolontario(volontarioId).getOrNull() ?: emptyList()
            _numeroValutazioni.value = when (tutte.size) {
                0 -> ""
                1 -> "su 1 valutazione"
                else -> "su ${tutte.size} valutazioni"
            }

            val conCommento = tutte.filter { !it.commento.isNullOrBlank() }
            _recensioni.value = conCommento.map { rating ->
                // nome e tipo dalla RICHIESTA (autoreNome è denormalizzato ed è leggibile
                // dal volontario che l'ha servita)
                val richiesta = requestRepository.getRichiesta(rating.requestId).getOrNull()
                RecensioneUi(
                    commento = rating.commento ?: "",
                    etichetta = RecensioneFormat.etichetta(
                        richiesta?.autoreNome ?: "", richiesta?.tipo ?: ""
                    )
                )
            }
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
    private val ratingRepository: RatingRepository,
    private val requestRepository: RequestRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProfiloVolontarioViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProfiloVolontarioViewModel(userRepository, authRepository, ratingRepository, requestRepository) as T
        }
        throw IllegalArgumentException("ViewModel sconosciuto: ${modelClass.name}")
    }
}

package com.careconnect.viewmodel.volontario

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.careconnect.model.User
import com.careconnect.repository.AuthRepository
import com.careconnect.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel del Profilo Volontario. Carica il profilo una sola volta
 * all'apertura (non è realtime) e gestisce logout + modifica della bio.
 *
 * DATA BINDING (lezione 9): i dati di sola lettura mostrati a schermo
 * (nome, valutazione) sono esposti come LiveData e legati direttamente
 * dall'XML con espressioni @{}. Schema visto a lezione: un MutableLiveData
 * privato "di appoggio" (_campo) e un LiveData pubblico di sola lettura
 * (campo). Così il Fragment NON aggiorna più a mano le TextView: ci pensa
 * il DataBinding, che osserva il LiveData tramite il lifecycleOwner.
 */
class ProfiloVolontarioViewModel(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    // Utente caricato, tenuto in memoria come sorgente per salvaBio():
    // salvaUtente() fa un .set() completo, quindi serve sempre l'oggetto
    // User intero (lo copiamo con .copy()), non solo il campo cambiato.
    private var utenteCaricato: User? = null

    // ----- Campi di SOLA LETTURA, legati dall'XML tramite DataBinding -----

    // Nome del volontario. Parte da stringa vuota finché il profilo carica.
    private val _nome = MutableLiveData("")
    val nome: LiveData<String> = _nome

    // Testo della valutazione, GIÀ formattato qui (logica fuori dal Fragment):
    // "Valutazione: 4.5 / 5" oppure "Valutazione: non ancora valutato".
    private val _valutazione = MutableLiveData("")
    val valutazione: LiveData<String> = _valutazione

    // Bio con cui pre-riempire il campo di testo UNA volta. Non ha valore
    // iniziale: così il Fragment lo osserva e scrive nell'EditText solo
    // quando il profilo è davvero caricato (una singola emissione).
    private val _bioIniziale = MutableLiveData<String>()
    val bioIniziale: LiveData<String> = _bioIniziale

    // Email: non cambia durante la sessione, la leggiamo una volta come
    // semplice proprietà (il DataBinding può legare anche una String fissa).
    val email: String? = authRepository.utenteCorrente()?.email

    // ----- Eventi "una tantum" (Toast): restano StateFlow, il Fragment li
    //       raccoglie. Non sono stato da disegnare, ma segnali momentanei. -----
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

    // Riempie i LiveData a partire dall'utente caricato: da qui in poi le
    // TextView legate nell'XML si aggiornano da sole.
    private fun mostraUtente(utente: User) {
        utenteCaricato = utente
        _nome.value = utente.nome
        _valutazione.value = utente.ratingMedio
            ?.let { media -> "Valutazione: %.1f / 5".format(media) }
            ?: "Valutazione: non ancora valutato"
        _bioIniziale.value = utente.bio ?: ""
    }

    /**
     * Salva la nuova bio. Parte dall'utente già in memoria e lo copia con il
     * nuovo testo (salvaUtente = .set() completo, serve l'oggetto intero).
     */
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
    private val authRepository: AuthRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProfiloVolontarioViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProfiloVolontarioViewModel(userRepository, authRepository) as T
        }
        throw IllegalArgumentException("ViewModel sconosciuto: ${modelClass.name}")
    }
}
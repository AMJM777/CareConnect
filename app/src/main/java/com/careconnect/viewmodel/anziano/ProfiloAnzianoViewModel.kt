package com.careconnect.viewmodel.anziano

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
 * ViewModel del Profilo Anziano (FASE 8): nome, email, ruolo, codice invito
 * e indirizzo, più logout.
 *
 * DATA BINDING (lezione 9): i dati di sola lettura (nome, codice invito)
 * sono esposti come LiveData e legati dall'XML con @{}. Schema visto a
 * lezione: un MutableLiveData privato di appoggio (_campo) + un LiveData
 * pubblico di sola lettura (campo). L'indirizzo è EDITABILE, quindi resta
 * gestito a mano dal Fragment.
 */
class ProfiloAnzianoViewModel(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    // Utente caricato: sorgente per salvaIndirizzo() (salvaUtente = .set()
    // completo, serve l'oggetto User intero, copiato con .copy()).
    private var utenteCaricato: User? = null

    // Codice invito "grezzo", tenuto per la copia negli appunti.
    private var codiceCorrente: String? = null

    // Email: fissa durante la sessione, semplice proprietà (bindabile).
    val email: String? = authRepository.utenteCorrente()?.email

    // ----- Campi di SOLA LETTURA legati dall'XML tramite DataBinding -----
    private val _nome = MutableLiveData("")
    val nome: LiveData<String> = _nome

    // Testo del codice invito: il codice vero, oppure "..." mentre carica.
    private val _codiceInvitoTesto = MutableLiveData("...")
    val codiceInvitoTesto: LiveData<String> = _codiceInvitoTesto

    // Abilita il bottone "Copia" solo quando il codice è davvero pronto.
    private val _copiaAbilitato = MutableLiveData(false)
    val copiaAbilitato: LiveData<Boolean> = _copiaAbilitato

    // Indirizzo con cui pre-riempire il campo UNA volta. Nessun valore
    // iniziale: il Fragment lo scrive nell'EditText solo quando arriva.
    private val _indirizzoIniziale = MutableLiveData<String>()
    val indirizzoIniziale: LiveData<String> = _indirizzoIniziale

    // ----- Eventi "una tantum" (Toast): restano StateFlow, sono segnali,
    //       non stato da disegnare. -----
    private val _errore = MutableStateFlow<String?>(null)
    val errore: StateFlow<String?> = _errore.asStateFlow()

    private val _indirizzoSalvato = MutableStateFlow(false)
    val indirizzoSalvato: StateFlow<Boolean> = _indirizzoSalvato.asStateFlow()

    init {
        caricaProfilo()
        caricaCodiceInvito()
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

    // Riempie i LiveData del profilo: da qui le View legate si aggiornano da sole.
    private fun mostraUtente(utente: User) {
        utenteCaricato = utente
        _nome.value = utente.nome
        _indirizzoIniziale.value = utente.indirizzo ?: ""
    }

    private fun caricaCodiceInvito() {
        val uid = authRepository.utenteCorrente()?.uid ?: return
        viewModelScope.launch {
            userRepository.ottieniOCreaCodiceInvito(uid).fold(
                onSuccess = { codice -> mostraCodiceInvito(codice) },
                onFailure = { errore ->
                    _errore.value = errore.message ?: "Impossibile generare il codice invito"
                }
            )
        }
    }

    private fun mostraCodiceInvito(codice: String) {
        codiceCorrente = codice
        _codiceInvitoTesto.value = codice
        _copiaAbilitato.value = true
    }

    /** Codice grezzo da copiare negli appunti (null se non ancora pronto). */
    fun codicePerCopia(): String? = codiceCorrente

    /** Parte dall'utente già in memoria (.copy()): salvaUtente() è un .set() completo. */
    fun salvaIndirizzo(nuovoIndirizzo: String) {
        val utenteAttuale = utenteCaricato
        if (utenteAttuale == null) {
            _errore.value = "Profilo non ancora caricato"
            return
        }
        if (nuovoIndirizzo.isBlank()) {
            _errore.value = "Inserisci un indirizzo valido"
            return
        }
        val utenteAggiornato = utenteAttuale.copy(indirizzo = nuovoIndirizzo.trim())

        viewModelScope.launch {
            userRepository.salvaUtente(utenteAggiornato).fold(
                onSuccess = {
                    utenteCaricato = utenteAggiornato
                    _indirizzoSalvato.value = true
                },
                onFailure = { errore ->
                    _errore.value = errore.message ?: "Impossibile salvare l'indirizzo"
                }
            )
        }
    }

    fun erroreMostrato() {
        _errore.value = null
    }

    fun indirizzoSalvatoMostrato() {
        _indirizzoSalvato.value = false
    }
}

class ProfiloAnzianoViewModelFactory(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProfiloAnzianoViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProfiloAnzianoViewModel(userRepository, authRepository) as T
        }
        throw IllegalArgumentException("ViewModel sconosciuto: ${modelClass.name}")
    }
}
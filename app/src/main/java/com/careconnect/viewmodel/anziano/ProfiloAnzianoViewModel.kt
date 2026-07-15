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

// dati di sola lettura esposti come LiveData e legati dall'XML con data
// binding; l'indirizzo è editabile e resta gestito dal Fragment
class ProfiloAnzianoViewModel(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    // utente caricato: serve a salvaIndirizzo() per fare .copy() (salvaUtente è un .set() completo)
    private var utenteCaricato: User? = null

    // codice invito tenuto per la copia negli appunti
    private var codiceCorrente: String? = null

    val email: String? = authRepository.utenteCorrente()?.email

    private val _nome = MutableLiveData("")
    val nome: LiveData<String> = _nome

    private val _codiceInvitoTesto = MutableLiveData("...")
    val codiceInvitoTesto: LiveData<String> = _codiceInvitoTesto

    private val _copiaAbilitato = MutableLiveData(false)
    val copiaAbilitato: LiveData<Boolean> = _copiaAbilitato

    private val _indirizzoIniziale = MutableLiveData<String>()
    val indirizzoIniziale: LiveData<String> = _indirizzoIniziale

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

    private fun mostraUtente(utente: User) {
        utenteCaricato = utente
        _nome.value = utente.nome
        _indirizzoIniziale.value = utente.indirizzo ?: ""
    }

    // funzione per ottenere (o creare, se non esiste) il codice invito dell'anziano
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

    fun codicePerCopia(): String? = codiceCorrente

    // funzione per salvare un nuovo indirizzo a partire dall'utente già in memoria
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
package com.careconnect.viewmodel.familiare

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

// carica il nome del familiare e la lista degli anziani seguiti,
// e permette di collegare un nuovo anziano tramite codice invito
class ProfiloFamiliareViewModel(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _nomeFamiliareTesto = MutableLiveData("")
    val nomeFamiliareTesto: LiveData<String> = _nomeFamiliareTesto

    // anziani seguiti dal familiare (molti-a-molti)
    private val _anzianiSeguiti = MutableLiveData<List<User>>(emptyList())
    val anzianiSeguiti: LiveData<List<User>> = _anzianiSeguiti

    // email del familiare, mostrata sotto il nome
    val email: String? = authRepository.utenteCorrente()?.email

    private val _errore = MutableStateFlow<String?>(null)
    val errore: StateFlow<String?> = _errore.asStateFlow()

    private val _collegamentoInCorso = MutableStateFlow(false)
    val collegamentoInCorso: StateFlow<Boolean> = _collegamentoInCorso.asStateFlow()

    private val _collegamentoRiuscito = MutableStateFlow(false)
    val collegamentoRiuscito: StateFlow<Boolean> = _collegamentoRiuscito.asStateFlow()

    init {
        caricaInfo()
    }

    // carica il nome del familiare e i dati di ogni anziano seguito
    private fun caricaInfo() {
        val uid = authRepository.utenteCorrente()?.uid
        if (uid == null) {
            _errore.value = "Sessione non valida"
            return
        }
        viewModelScope.launch {
            val familiare = userRepository.getUtente(uid).getOrElse {
                _errore.value = it.message ?: "Impossibile caricare il profilo"
                return@launch
            }
            _nomeFamiliareTesto.value = familiare.nome
            _anzianiSeguiti.value = familiare.anzianiCollegatiIds.mapNotNull { id ->
                userRepository.getUtente(id).getOrNull()
            }
        }
    }

    // collega un nuovo anziano a partire dal codice invito inserito
    fun collegati(codiceInserito: String) {
        val uid = authRepository.utenteCorrente()?.uid
        if (uid == null) {
            _errore.value = "Sessione non valida"
            return
        }
        val codice = codiceInserito.trim().uppercase()
        if (codice.isEmpty()) {
            _errore.value = "Inserisci un codice"
            return
        }

        _collegamentoInCorso.value = true
        viewModelScope.launch {
            userRepository.trovaAnzianoPerCodiceInvito(codice).fold(
                onSuccess = { anziano ->
                    userRepository.collegaFamiliareAdAnziano(anziano.uid, uid).fold(
                        onSuccess = {
                            _collegamentoRiuscito.value = true
                            caricaInfo()
                        },
                        onFailure = { errore ->
                            _errore.value = errore.message ?: "Impossibile completare il collegamento"
                        }
                    )
                },
                onFailure = {
                    _errore.value = "Codice non valido"
                }
            )
            _collegamentoInCorso.value = false
        }
    }

    fun erroreMostrato() {
        _errore.value = null
    }

    fun collegamentoRiuscitoMostrato() {
        _collegamentoRiuscito.value = false
    }
}

class ProfiloFamiliareViewModelFactory(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProfiloFamiliareViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProfiloFamiliareViewModel(userRepository, authRepository) as T
        }
        throw IllegalArgumentException("ViewModel sconosciuto: ${modelClass.name}")
    }
}

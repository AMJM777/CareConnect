package com.careconnect.viewmodel.familiare

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.careconnect.repository.AuthRepository
import com.careconnect.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// stato della home familiare: si sa solo dopo una chiamata a Firestore se l'utente è già collegato a un anziano
sealed class StatoHomeFamiliare {
    object Caricamento : StatoHomeFamiliare()
    object NonCollegato : StatoHomeFamiliare()
    object Collegato : StatoHomeFamiliare()
}

// capisce se l'utente è già collegato a un anziano e gestisce il collegamento tramite codice invito
class HomeFamiliareViewModel(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _stato = MutableStateFlow<StatoHomeFamiliare>(StatoHomeFamiliare.Caricamento)
    val stato: StateFlow<StatoHomeFamiliare> = _stato.asStateFlow()

    // separato da _stato: un errore non deve far sparire il form, l'utente deve poter riprovare
    private val _erroreCollegamento = MutableStateFlow<String?>(null)
    val erroreCollegamento: StateFlow<String?> = _erroreCollegamento.asStateFlow()

    private val _collegamentoInCorso = MutableStateFlow(false)
    val collegamentoInCorso: StateFlow<Boolean> = _collegamentoInCorso.asStateFlow()

    init {
        caricaStatoCollegamento()
    }

    // funzione per verificare se l'utente è già collegato a un anziano e caricarne il nome
    private fun caricaStatoCollegamento() {
        val uid = authRepository.utenteCorrente()?.uid
        if (uid == null) {
            _stato.value = StatoHomeFamiliare.NonCollegato
            return
        }
        viewModelScope.launch {
            userRepository.getUtente(uid).fold(
                onSuccess = { familiare ->
                    // collegato se segue almeno un anziano (lista molti-a-molti)
                    _stato.value = if (familiare.anzianiCollegatiIds.isEmpty()) {
                        StatoHomeFamiliare.NonCollegato
                    } else {
                        StatoHomeFamiliare.Collegato
                    }
                },
                onFailure = { _stato.value = StatoHomeFamiliare.NonCollegato }
            )
        }
    }

    // funzione chiamata quando l'utente preme "collegati" con un codice inserito
    fun collegati(codiceInserito: String) {
        val uid = authRepository.utenteCorrente()?.uid
        if (uid == null) {
            _erroreCollegamento.value = "Sessione non valida"
            return
        }
        val codice = codiceInserito.trim().uppercase()
        if (codice.isEmpty()) {
            _erroreCollegamento.value = "Inserisci un codice"
            return
        }

        _collegamentoInCorso.value = true
        viewModelScope.launch {
            userRepository.trovaAnzianoPerCodiceInvito(codice).fold(
                onSuccess = { anziano ->
                    userRepository.collegaFamiliareAdAnziano(anziano.uid, uid).fold(
                        onSuccess = {
                            _stato.value = StatoHomeFamiliare.Collegato
                        },
                        onFailure = { errore ->
                            _erroreCollegamento.value = errore.message ?: "Impossibile completare il collegamento"
                        }
                    )
                },
                onFailure = {
                    _erroreCollegamento.value = "Codice non valido"
                }
            )
            _collegamentoInCorso.value = false
        }
    }

    // funzione per segnalare che l'errore è stato mostrato e va nascosto
    fun erroreMostrato() {
        _erroreCollegamento.value = null
    }
}

class HomeFamiliareViewModelFactory(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeFamiliareViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HomeFamiliareViewModel(userRepository, authRepository) as T
        }
        throw IllegalArgumentException("ViewModel sconosciuto: ${modelClass.name}")
    }
}
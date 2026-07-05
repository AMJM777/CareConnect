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

/** Stato della Home Familiare: sappiamo solo DOPO una chiamata a Firestore se l'utente è già collegato a un anziano. */
sealed class StatoHomeFamiliare {
    object Caricamento : StatoHomeFamiliare()
    object NonCollegato : StatoHomeFamiliare()
    data class Collegato(val nomeAnziano: String) : StatoHomeFamiliare()
}

/**
 * ViewModel della Home Familiare (FASE 6).
 * Si occupa di due cose: capire se l'utente è già collegato a un anziano,
 * e gestire il collegamento tramite codice invito quando non lo è ancora.
 */
class HomeFamiliareViewModel(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _stato = MutableStateFlow<StatoHomeFamiliare>(StatoHomeFamiliare.Caricamento)
    val stato: StateFlow<StatoHomeFamiliare> = _stato.asStateFlow()

    // Errore del TENTATIVO di collegamento (codice sbagliato, già collegato altrove, ecc.)
    // Separato da _stato perché un errore non deve far sparire il form: l'utente deve poter riprovare.
    private val _erroreCollegamento = MutableStateFlow<String?>(null)
    val erroreCollegamento: StateFlow<String?> = _erroreCollegamento.asStateFlow()

    private val _collegamentoInCorso = MutableStateFlow(false)
    val collegamentoInCorso: StateFlow<Boolean> = _collegamentoInCorso.asStateFlow()

    init {
        caricaStatoCollegamento()
    }

    private fun caricaStatoCollegamento() {
        val uid = authRepository.utenteCorrente()?.uid
        if (uid == null) {
            _stato.value = StatoHomeFamiliare.NonCollegato
            return
        }
        viewModelScope.launch {
            userRepository.getUtente(uid).fold(
                onSuccess = { familiare ->
                    val anzianoId = familiare.anzianoCollegatoId
                    if (anzianoId == null) {
                        _stato.value = StatoHomeFamiliare.NonCollegato
                    } else {
                        // Servono anche i dati dell'anziano per mostrarne il nome.
                        userRepository.getUtente(anzianoId).fold(
                            onSuccess = { anziano -> _stato.value = StatoHomeFamiliare.Collegato(anziano.nome) },
                            onFailure = { _stato.value = StatoHomeFamiliare.NonCollegato }
                        )
                    }
                },
                onFailure = { _stato.value = StatoHomeFamiliare.NonCollegato }
            )
        }
    }

    /** Chiamato quando l'utente preme "Collegati" con un codice inserito. */
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
                            _stato.value = StatoHomeFamiliare.Collegato(anziano.nome)
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
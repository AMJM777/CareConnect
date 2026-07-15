package com.careconnect.viewmodel.anziano

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.careconnect.model.SosAlert
import com.careconnect.repository.AuthRepository
import com.careconnect.repository.SosRepository
import com.careconnect.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// gestisce solo l'invio dell'SOS ("Nuova richiesta" naviga direttamente al Fragment esistente)
class DashboardAnzianoViewModel(
    private val sosRepository: SosRepository,
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _sosInviato = MutableStateFlow(false)
    val sosInviato: StateFlow<Boolean> = _sosInviato.asStateFlow()

    private val _errore = MutableStateFlow<String?>(null)
    val errore: StateFlow<String?> = _errore.asStateFlow()

    // funzione per inviare l'SOS: scrive un alert per ogni familiare
    // collegato (un anziano può averne più di uno)
    fun inviaSos() {
        val anzianoId = authRepository.utenteCorrente()?.uid
        if (anzianoId == null) {
            _errore.value = "Sessione non valida"
            return
        }

        viewModelScope.launch {
            val anziano = userRepository.getUtente(anzianoId).getOrElse {
                _errore.value = it.message ?: "Impossibile inviare l'allarme"
                return@launch
            }

            if (anziano.familiariCollegatiIds.isEmpty()) {
                _errore.value = "Nessun familiare collegato: condividi il tuo codice invito prima di usare SOS"
                return@launch
            }

            var almenoUnoRiuscito = false
            for (familiareId in anziano.familiariCollegatiIds) {
                val alert = SosAlert(anzianoId = anzianoId, familiareId = familiareId)
                sosRepository.creaAlert(alert).onSuccess { almenoUnoRiuscito = true }
            }

            if (almenoUnoRiuscito) {
                _sosInviato.value = true
            } else {
                _errore.value = "Impossibile avvisare i familiari, riprova"
            }
        }
    }

    fun erroreMostrato() {
        _errore.value = null
    }

    fun sosInviatoMostrato() {
        _sosInviato.value = false
    }
}

class DashboardAnzianoViewModelFactory(
    private val sosRepository: SosRepository,
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DashboardAnzianoViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DashboardAnzianoViewModel(sosRepository, userRepository, authRepository) as T
        }
        throw IllegalArgumentException("ViewModel sconosciuto: ${modelClass.name}")
    }
}
package com.careconnect.viewmodel.anziano

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.careconnect.model.Request
import com.careconnect.model.RequestStatus
import com.careconnect.model.SosAlert
import com.careconnect.repository.AuthRepository
import com.careconnect.repository.RequestRepository
import com.careconnect.repository.SosRepository
import com.careconnect.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// ViewModel della Home dell'Anziano, riunisce la creazione di una richiesta,
// il banner "richiesta in corso" e l'SOS
// (la modifica di una richiesta si trova in NuovaRichiestaViewModel)
class NuovaRichiestaHomeViewModel(
    private val requestRepository: RequestRepository,
    private val userRepository: UserRepository,
    private val sosRepository: SosRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    // 1) CREAZIONE RICHIESTA
    // uso lo stato UI del form (idle, loading, successo, errore)
    private val _uiState = MutableStateFlow<NuovaRichiestaUiState>(NuovaRichiestaUiState.Idle)
    val uiState: StateFlow<NuovaRichiestaUiState> = _uiState.asStateFlow()

    // crea una richiesta
    fun creaRichiesta(tipo: String, descrizione: String) {
        val autoreId = authRepository.utenteCorrente()?.uid
        if (autoreId == null) {
            _uiState.value = NuovaRichiestaUiState.Errore(
                IllegalStateException("Sessione scaduta, effettua di nuovo il login")
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = NuovaRichiestaUiState.Loading

            val autore = userRepository.getUtente(autoreId).getOrElse {
                _uiState.value = NuovaRichiestaUiState.Errore(it)
                return@launch
            }

            // senza indirizzo il volontario non saprebbe dove andare
            val indirizzo = autore.indirizzo
            if (indirizzo.isNullOrBlank()) {
                _uiState.value = NuovaRichiestaUiState.Errore(
                    IllegalStateException(
                        "Imposta prima il tuo indirizzo dal Profilo: serve al volontario per sapere dove venire"
                    )
                )
                return@launch
            }

            val nuovaRichiesta = Request(
                autoreId = autoreId,
                autoreNome = autore.nome,
                autoreIndirizzo = indirizzo,
                tipo = tipo,
                descrizione = descrizione
            )

            requestRepository.creaRichiesta(nuovaRichiesta).fold(
                onSuccess = { id -> _uiState.value = NuovaRichiestaUiState.Successo(id) },
                onFailure = { errore -> _uiState.value = NuovaRichiestaUiState.Errore(errore) }
            )
        }
    }

    // il fragment lo chiama dopo aver gestito l'esito, per riportare lo stato a Idle
    fun statoConsumato() {
        _uiState.value = NuovaRichiestaUiState.Idle
    }

    // 2) BANNER "RICHIESTA IN CORSO"
    // stream realtime delle sole richieste ATTIVE dell'anziano: quelle non ancora
    // chiuse (aperta / presa in carico / completata in attesa di conferma)
    val richiesteAttive: StateFlow<List<Request>> = run {
        val anzianoId = authRepository.utenteCorrente()?.uid

        val flow = if (anzianoId != null) {
            requestRepository.osservaRichiestePerAnziano(anzianoId)
        } else {
            emptyFlow()
        }

        flow
            .map { lista -> lista.filter { it.stato in STATI_ATTIVI } }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )
    }

    // 3) SOS
    // c'è un alert per ogni familiare collegato, l'anziano può averne più di uno
    private val _sosInviato = MutableStateFlow(false)
    val sosInviato: StateFlow<Boolean> = _sosInviato.asStateFlow()

    private val _erroreSos = MutableStateFlow<String?>(null)
    val erroreSos: StateFlow<String?> = _erroreSos.asStateFlow()

    fun inviaSos() {
        val anzianoId = authRepository.utenteCorrente()?.uid
        if (anzianoId == null) {
            _erroreSos.value = "Sessione non valida"
            return
        }

        viewModelScope.launch {
            val anziano = userRepository.getUtente(anzianoId).getOrElse {
                _erroreSos.value = it.message ?: "Impossibile inviare l'allarme"
                return@launch
            }

            if (anziano.familiariCollegatiIds.isEmpty()) {
                _erroreSos.value = "Nessun familiare collegato: condividi il tuo codice invito prima di usare SOS"
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
                _erroreSos.value = "Impossibile avvisare i familiari, riprova"
            }
        }
    }

    fun erroreSosMostrato() {
        _erroreSos.value = null
    }

    fun sosInviatoMostrato() {
        _sosInviato.value = false
    }

    companion object {
        // stati considerati "in corso" per il banner: tutto ciò che non è
        // ancora terminale (CONFERMATA / ANNULLATA)
        private val STATI_ATTIVI = setOf(
            RequestStatus.APERTA,
            RequestStatus.PRESA_IN_CARICO,
            RequestStatus.COMPLETATA_DAL_VOLONTARIO
        )
    }
}

// factory: crea il ViewModel iniettando i repository
class NuovaRichiestaHomeViewModelFactory(
    private val requestRepository: RequestRepository,
    private val userRepository: UserRepository,
    private val sosRepository: SosRepository,
    private val authRepository: AuthRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NuovaRichiestaHomeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return NuovaRichiestaHomeViewModel(
                requestRepository, userRepository, sosRepository, authRepository
            ) as T
        }
        throw IllegalArgumentException("ViewModel sconosciuto: ${modelClass.name}")
    }
}
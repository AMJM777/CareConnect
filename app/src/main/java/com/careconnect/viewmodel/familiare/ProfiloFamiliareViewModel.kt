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

/** Dati mostrati nel Profilo Familiare: nome proprio e nome dell'anziano seguito. */
data class ProfiloFamiliareInfo(
    val nomeFamiliare: String,
    val nomeAnziano: String
)

/**
 * ViewModel del Profilo Familiare (FASE 6). Carica una sola volta (non
 * realtime: questi dati non cambiano mentre la schermata è aperta) il
 * proprio nome e quello dell'anziano collegato.
 */
class ProfiloFamiliareViewModel(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _info = MutableStateFlow<ProfiloFamiliareInfo?>(null)
    val info: StateFlow<ProfiloFamiliareInfo?> = _info.asStateFlow()

    private val _errore = MutableStateFlow<String?>(null)
    val errore: StateFlow<String?> = _errore.asStateFlow()

    init {
        caricaInfo()
    }

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
            val anzianoId = familiare.anzianoCollegatoId
            if (anzianoId == null) {
                _errore.value = "Nessun anziano collegato"
                return@launch
            }
            val anziano = userRepository.getUtente(anzianoId).getOrElse {
                _errore.value = it.message ?: "Impossibile caricare i dati dell'anziano"
                return@launch
            }
            _info.value = ProfiloFamiliareInfo(
                nomeFamiliare = familiare.nome,
                nomeAnziano = anziano.nome
            )
        }
    }

    fun erroreMostrato() {
        _errore.value = null
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
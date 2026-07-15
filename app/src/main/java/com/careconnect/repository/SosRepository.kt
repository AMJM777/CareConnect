package com.careconnect.repository

import com.careconnect.model.SosAlert
import com.careconnect.model.SosStatus
import kotlinx.coroutines.flow.Flow

// interfaccia per la gestione delle segnalazioni di emergenza (SOS)
interface SosRepository {

    //crea un nuovo alert SOS. familiareId deve essere già risolto dal chiamante
    suspend fun creaAlert(alert: SosAlert): Result<String>

    //aggiorna lo stato di un alert (es. VISTO dal familiare, poi CHIUSO)
    suspend fun aggiornaStato(alertId: String, nuovoStato: SosStatus): Result<Unit>

    // Stream in tempo reale degli alert per un familiare (per la notifica immediata)
    fun osservaAlertPerFamiliare(familiareId: String): Flow<List<SosAlert>>
}
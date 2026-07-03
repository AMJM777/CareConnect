package com.careconnect.repository

import com.careconnect.model.Request
import com.careconnect.model.RequestStatus
import kotlinx.coroutines.flow.Flow

interface RequestRepository {

    /** Crea una nuova richiesta su Firestore. Ritorna l'id generato in caso di successo. */
    suspend fun creaRichiesta(request: Request): Result<String>

    /** Legge una singola richiesta per id. */
    suspend fun getRichiesta(requestId: String): Result<Request>

    /**
     * Aggiorna lo stato di una richiesta, validando che la transizione
     * da stato attuale al nuovo stato sia ammessa da "RequestStatus.canTransitionTo"
     * Se "Nuovo volontario Id" è fornito, aggiorna anche quel campo nella stessa scrittura
     * (usato per presa in carico e rilascio).
     */
    suspend fun aggiornaStato(
        requestId: String,
        nuovoStato: RequestStatus,
        nuovoVolontarioId: String? = null
    ): Result<Unit>

    /** Stream in tempo reale delle richieste con stato APERTA (per il volontario). */
    fun osservaRichiesteAperte(): Flow<List<Request>>

    /** Stream in tempo reale delle richieste create da un anziano (storico). */
    fun osservaRichiestePerAnziano(anzianoId: String): Flow<List<Request>>
}
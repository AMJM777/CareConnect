package com.careconnect.repository

import com.careconnect.model.Request
import com.careconnect.model.RequestStatus
import kotlinx.coroutines.flow.Flow

//interfaccia per la gestione di richieste d'aiuto
interface RequestRepository {

    // funzione per creare una nuova richiesta su firestore. ritorna l'id generato in caso di successo
    suspend fun creaRichiesta(request: Request): Result<String>

    // funzione per leggere una singola richiesta per id
    suspend fun getRichiesta(requestId: String): Result<Request>

    /**
     * funzione per aggiornare lo stato di una richiesta (purchè valida)
     */
    suspend fun aggiornaStato(
        requestId: String,
        nuovoStato: RequestStatus,
        nuovoVolontarioId: String? = null, //se nuovoVolontarioId è fornito,aggiorna campo nella stessa scrittura
        nuovoVolontarioNome: String? = null // viene scritto solo se nuovoVolontarioId è fornito
    ): Result<Unit>

    /**
     * Modifica tipo e descrizione di una richiesta esistente.
     * Permesso solo se la richiesta è ancora APERTA
     */
    suspend fun modificaRichiesta(
        requestId: String,
        nuovoTipo: String,
        nuovaDescrizione: String
    ): Result<Unit>

    /** Stream in tempo reale delle richieste con stato APERTA, per il volontario */
    fun osservaRichiesteAperte(): Flow<List<Request>>

    /**
     * funzione per leggere una volta sola le richieste aperte (query singola,non realtime).
     * serve al task periodico in background
     */
    suspend fun getRichiesteAperte(): Result<List<Request>>

    /** Stream in tempo reale delle richieste create da un anziano */
    fun osservaRichiestePerAnziano(anzianoId: String): Flow<List<Request>>

    /**
     * funzione per leggere una volta sola tutte le richieste di un anziano (query singola, non realtime). serve al worker del familiare, che poi
     * e filtra quelle in attesa di conferma
     */
    suspend fun getRichiestePerAnziano(anzianoId: String): Result<List<Request>>

    /**
     * stream in tempo reale delle richieste "attive" di un volontario: PRESA_IN_CARICO o
     * COMPLETATA_DAL_VOLONTARIO (in attesa di conferma del garante)
     */
    fun osservaRichiestePerVolontario(volontarioId: String): Flow<List<Request>>
}
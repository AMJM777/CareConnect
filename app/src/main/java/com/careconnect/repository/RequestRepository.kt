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
     * Se "nuovoVolontarioId" è fornito, aggiorna anche quel campo nella stessa scrittura
     * (usato per presa in carico e rilascio).
     *
     * FASE 7: "nuovoVolontarioNome" viene scritto solo se "nuovoVolontarioId"
     * è fornito (caso "prendi in carico"): il chiamante (ViewModel) lo
     * recupera con UserRepository.getUtente() PRIMA di questa chiamata,
     * così il repository resta "puro" e legge/scrive solo la collezione
     * "requests", senza sconfinare su "users".
     */
    suspend fun aggiornaStato(
        requestId: String,
        nuovoStato: RequestStatus,
        nuovoVolontarioId: String? = null,
        nuovoVolontarioNome: String? = null
    ): Result<Unit>

    /**
     * Modifica tipo e descrizione di una richiesta esistente.
     * Permesso SOLO se la richiesta è ancora APERTA: se un volontario l'ha
     * già presa in carico, cambiarne il contenuto lo lascerebbe con
     * informazioni superate senza saperlo.
     */
    suspend fun modificaRichiesta(
        requestId: String,
        nuovoTipo: String,
        nuovaDescrizione: String
    ): Result<Unit>

    /** Stream in tempo reale delle richieste con stato APERTA (per il volontario). */
    fun osservaRichiesteAperte(): Flow<List<Request>>

    /**
     * Legge UNA VOLTA SOLA le richieste aperte (query singola, non realtime).
     * Serve al Worker in background (FASE 11): un task periodico compie un'unità
     * di lavoro discreta e poi termina, quindi non deve registrare un listener
     * realtime come osservaRichiesteAperte(), che invece serve alla UI del volontario.
     * Coerente con la scelta di progetto "suspend fun per operazioni singole,
     * Flow per gli ascolti realtime".
     */
    suspend fun getRichiesteAperte(): Result<List<Request>>

    /** Stream in tempo reale delle richieste create da un anziano (storico). */
    fun osservaRichiestePerAnziano(anzianoId: String): Flow<List<Request>>

    /**
     * Legge UNA VOLTA SOLA tutte le richieste di un anziano (query singola,
     * non realtime). Serve al Worker del Familiare (FASE 11b), che poi filtra
     * quelle in attesa di conferma. Stessa logica di getRichiesteAperte():
     * il task fa un'operazione discreta e termina, non resta in ascolto.
     */
    suspend fun getRichiestePerAnziano(anzianoId: String): Result<List<Request>>
    /**
     * Stream in tempo reale delle richieste "attive" di un volontario, cioè
     * quelle che ha preso in carico e non sono ancora arrivate a uno stato
     * terminale dal suo punto di vista: PRESA_IN_CARICO (sta lavorandoci) o
     * COMPLETATA_DAL_VOLONTARIO (ha finito, in attesa di conferma del garante).
     * Non include CONFERMATA/ANNULLATA: quelle non richiedono più nessuna
     * azione da parte del volontario, quindi non hanno senso in questa lista.
     */
    fun osservaRichiestePerVolontario(volontarioId: String): Flow<List<Request>>
}
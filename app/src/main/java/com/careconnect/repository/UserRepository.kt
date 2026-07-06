package com.careconnect.repository

import com.careconnect.model.User

interface UserRepository {

    /** Salva (crea o sovrascrive) il profilo utente su Firestore, alla registrazione. */
    suspend fun salvaUtente(user: User): Result<Unit>

    /** Legge il profilo utente per uid. */
    suspend fun getUtente(uid: String): Result<User>

    /**
     * FASE 6 — restituisce il codice invito dell'anziano indicato.
     * Se non esiste ancora lo genera, verificando che sia univoco, e lo
     * salva. Chiamate successive per lo stesso anziano restituiscono
     * sempre lo STESSO codice: è pensato per essere condiviso con più
     * familiari nel tempo, non "a consumo singolo".
     */
    suspend fun ottieniOCreaCodiceInvito(anzianoId: String): Result<String>

    /**
     * FASE 6 — cerca l'anziano proprietario di un dato codice invito.
     * Usato dalla schermata "Collegati al tuo assistito" del Familiare.
     */
    suspend fun trovaAnzianoPerCodiceInvito(codice: String): Result<User>

    /**
     * FASE 6 — collega un familiare a un anziano in un'unica scrittura
     * atomica: aggiunge familiareId alla lista dell'anziano E imposta
     * anzianoCollegatoId sul familiare. Fallisce se il familiare risulta
     * già collegato a un altro anziano (un familiare segue un solo assistito).
     */
    suspend fun collegaFamiliareAdAnziano(anzianoId: String, familiareId: String): Result<Unit>

    /**
     * FASE 9 — ricalcola la media aritmetica delle stelle di TUTTE le
     * valutazioni ricevute dal volontario indicato, e aggiorna il campo
     * ratingMedio sul suo profilo.
     *
     * Va chiamato DOPO che un nuovo Rating è stato creato con successo
     * (vedi RatingRepository.creaRatingEConfermaRichiesta). Non è incluso
     * in quella Transaction: qui serve una query su tutta la collezione
     * "ratings", e le Transaction di Firestore supportano solo letture
     * puntuali di documenti, non query — per questo è un passo separato.
     */
    suspend fun aggiornaRatingMedio(volontarioId: String): Result<Unit>
}
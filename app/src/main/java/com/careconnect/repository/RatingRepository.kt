package com.careconnect.repository

import com.careconnect.model.Rating

interface RatingRepository {

    /**
     * Crea il rating per una richiesta completata e, nella stessa transazione,
     * porta lo stato della richiesta a CONFERMATA. Fallisce se la richiesta
     * non si trova nello stato COMPLETATA_DAL_VOLONTARIO (transizione non valida).
     */
    suspend fun creaRatingEConfermaRichiesta(rating: Rating): Result<String>
}
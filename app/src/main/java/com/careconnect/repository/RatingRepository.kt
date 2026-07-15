package com.careconnect.repository

import com.careconnect.model.Rating

//interfaccia per la gesione delle valutazioni a stelle
interface RatingRepository {

    /**
     * funzione per creare la valutazione di una richiesta completata e,
     * nella stessa transazione, portare lo stato della richiesta a CONFERMATA.
     * fallisce se la richiesta non si trova nello stato COMPLETATA_DAL_VOLONTARIO
     * (transizione non valida)
     */
    suspend fun creaRatingEConfermaRichiesta(rating: Rating): Result<String>
}
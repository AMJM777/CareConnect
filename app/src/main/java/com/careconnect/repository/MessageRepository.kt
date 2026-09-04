package com.careconnect.repository

import com.careconnect.model.Message
import kotlinx.coroutines.flow.Flow


interface MessageRepository {

    // invia un nuovo messaggio e ritorna l'id del documento creato
    suspend fun inviaMessaggio(messaggio: Message): Result<String>

    // osserva in tempo reale i messaggi di una richiesta
    // campoUtente ("anzianoId" o "volontarioId") + uidUtente identificano chi sta guardando:
    fun osservaMessaggiPerRichiesta(
        requestId: String,
        campoUtente: String,
        uidUtente: String
    ): Flow<List<Message>>
}
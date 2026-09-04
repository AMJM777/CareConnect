package com.careconnect.model

import com.google.firebase.Timestamp

/**
 * un messaggio della chat tra anziano e volontario, questo è legato a una richiesta;
 * anzianoId e volontarioId sono copiati dalla richiesta al momento dell'invio
 */
data class Message(
    val id: String = "",
    val requestId: String = "",      // a quale richiesta appartiene la chat
    val anzianoId: String = "",      // autore della richiesta (denormalizzato)
    val volontarioId: String = "",   // volontario assegnato (denormalizzato)
    val mittenteId: String = "",     // uid di chi ha scritto questo messaggio
    val testo: String = "",
    val timestamp: Timestamp = Timestamp.now()
)
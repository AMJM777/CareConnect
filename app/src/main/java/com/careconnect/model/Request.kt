package com.careconnect.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint

/**
 * rappresenta una richiesta di aiuto creata dall'anziano
 * tre campi sono duplicati dal profilo utente per rendere
 * la richiesta leggibile a chi non è il volontario, senza query aggiuntive:
 * - autoreNome/autoreIndirizzo: chi è l'anziano e dove si trova
 * - volontarioNome: chi ha accettato la richiesta
 * NOTA: se utente cambia nome o indirizzo dopo aver creato le richiese queste non si aggiornano
 */
data class Request(
    val id: String = "",
    val autoreId: String = "",                  // uid dell'anziano
    val autoreNome: String = "",                 // nome dell'anziano, per volontario/familiare
    val autoreIndirizzo: String = "",             // dove andare, visibile al volontario SOLO dopo l'accettazione
    val tipo: String = "",                       // "spesa" | "bolletta" | "assistenza_digitale" | "altro"
    val descrizione: String = "",
    val stato: RequestStatus = RequestStatus.APERTA,
    val volontarioId: String? = null,
    val volontarioNome: String? = null,           // nome del volontario, per anziano/familiare
    val timestampCreazione: Timestamp = Timestamp.now(),
    val posizione: GeoPoint? = null
)
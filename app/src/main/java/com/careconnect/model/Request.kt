package com.careconnect.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint

/**
 * FASE 7 — tre campi denormalizzati aggiunti per rendere leggibile la
 * richiesta a chi non è il volontario, senza dover fare query aggiuntive:
 * - autoreNome/autoreIndirizzo: chi è l'anziano e dove si trova, letti dal
 *   suo profilo al momento della creazione (lui stesso, nessuna lettura extra)
 * - volontarioNome: chi ha accettato, scritto quando cambia volontarioId
 */
data class Request(
    val id: String = "",
    val autoreId: String = "",                  // uid dell'anziano
    val autoreNome: String = "",                 // nome dell'anziano, per Volontario/Familiare
    val autoreIndirizzo: String = "",             // dove andare, visibile al Volontario SOLO dopo l'accettazione
    val tipo: String = "",                       // "spesa" | "bolletta" | "assistenza_digitale" | "altro"
    val descrizione: String = "",
    val stato: RequestStatus = RequestStatus.APERTA,
    val volontarioId: String? = null,
    val volontarioNome: String? = null,           // nome del volontario, per Anziano/Familiare
    val timestampCreazione: Timestamp = Timestamp.now(),
    val posizione: GeoPoint? = null
)
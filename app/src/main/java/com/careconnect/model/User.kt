package com.careconnect.model

/**
 * Rappresenta un utente dell'app, con campi diversi valorizzati a seconda del ruolo.
 *
 * FASE 6 — collegamento Anziano <-> Familiare 1:N: un anziano può avere
 * PIÙ familiari/garanti collegati, ma ogni familiare segue UN SOLO anziano.
 * FASE 7 — aggiunto "indirizzo": serve al Volontario per sapere dove andare
 * quando accetta una richiesta (letto e denormalizzato su Request alla
 * creazione, vedi RequestRepository).
 */
data class User(
    val uid: String = "",
    val nome: String = "",
    val ruolo: UserRole = UserRole.ANZIANO,
    val familiariCollegatiIds: List<String> = emptyList(), // solo ANZIANO: uid di tutti i familiari collegati a lui
    val codiceInvito: String? = null,                      // solo ANZIANO: codice che i familiari usano per collegarsi
    val indirizzo: String? = null,                         // solo ANZIANO: dove il volontario deve andare
    val anzianoCollegatoId: String? = null,                // solo FAMILIARE: uid dell'unico anziano che segue
    val ratingMedio: Double? = null,                       // solo VOLONTARIO
    val bio: String? = null                                // facoltativo, solo VOLONTARIO
)
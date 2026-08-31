package com.careconnect.model

/**
 * rappresenta un utente dell'app, con campi diversi a seconda del ruolo.
 * un anziano può avere più familiari/garanti collegati, e un familiare può
 * seguire più anziani (relazione molti-a-molti).
 */
data class User(
    val uid: String = "",
    val nome: String = "",
    val ruolo: UserRole = UserRole.ANZIANO,
    val familiariCollegatiIds: List<String> = emptyList(), // solo ANZIANO: uid di tutti i familiari collegati a lui
    val codiceInvito: String? = null,                      // solo ANZIANO: codice che i familiari usano per collegarsi
    val indirizzo: String? = null,                         // solo ANZIANO: dove il volontario deve andare
    val anzianoCollegatoId: String? = null,                // deprecato: tenuto per compatibilità con i dati esistenti
    val anzianiCollegatiIds: List<String> = emptyList(),   // solo FAMILIARE: uid di tutti gli anziani seguiti
    val ratingMedio: Double? = null,                       // solo VOLONTARIO
    val bio: String? = null                                // facoltativo, solo VOLONTARIO
)
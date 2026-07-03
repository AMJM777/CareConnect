package com.careconnect.model

data class User(
    val uid: String = "",
    val nome: String = "",
    val ruolo: UserRole = UserRole.ANZIANO,
    val familiareCollegatoId: String? = null,   // valorizzato solo per ruolo ANZIANO
    val anzianoCollegatoId: String? = null,      // valorizzato solo per ruolo FAMILIARE
    val ratingMedio: Double? = null              // valorizzato solo per ruolo VOLONTARIO
)
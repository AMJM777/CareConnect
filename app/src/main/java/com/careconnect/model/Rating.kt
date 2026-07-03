package com.careconnect.model

data class Rating(
    val id: String = "",
    val requestId: String = "",
    val volontarioId: String = "",
    val stelle: Int = 0,                          // atteso 1..5, validazione lato UI/Repository
    val commento: String? = null,
    val valutatoreId: String = ""                 // uid del familiare o dell'anziano
)
package com.careconnect.util

// formattazione condivisa delle recensioni: nome abbreviato più tipo breve.
// usata dal profilo del volontario e dal dialog di profilo pubblico
object RecensioneFormat {

    // "Maria Rossi" diventa "Maria R.", nome più iniziale del cognome. i nomi
    // senza spazio, come gli account di test, restano invariati
    fun etichettaAutore(nome: String): String {
        val parti = nome.trim().split(" ").filter { it.isNotBlank() }
        return when {
            parti.isEmpty() -> "Anonimo"
            parti.size == 1 -> parti[0]
            else -> "${parti[0]} ${parti[1].first().uppercaseChar()}."
        }
    }

    // categoria in forma breve, per "altro" e i testi liberi mostro sempre "Altro"
    fun tipoBreve(tipo: String): String = when (tipo) {
        "spesa" -> "Spesa"
        "bolletta" -> "Bolletta"
        "assistenza_digitale" -> "Assistenza digitale"
        else -> "Altro"
    }

    // unisce nome e tipo come "Maria R. · Spesa"
    fun etichetta(nome: String, tipo: String): String =
        listOf(etichettaAutore(nome), tipoBreve(tipo))
            .filter { it.isNotBlank() }
            .joinToString(" · ")
}

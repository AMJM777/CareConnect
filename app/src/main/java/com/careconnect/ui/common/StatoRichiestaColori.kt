package com.careconnect.ui.common

import com.careconnect.R
import com.careconnect.model.RequestStatus

/**
 * Colori della "pillola" di stato di una richiesta, centralizzati qui così
 * tutte le liste (Anziano, Volontario, Familiare) mostrano lo stesso stato
 * con lo stesso colore. Ogni stato ha uno sfondo tenue e un testo leggibile.
 */
object StatoRichiestaColori {

    // Colore di SFONDO della pillola per ogni stato.
    fun sfondo(stato: RequestStatus): Int = when (stato) {
        RequestStatus.APERTA -> R.color.stato_aperta_bg
        RequestStatus.PRESA_IN_CARICO -> R.color.stato_incarico_bg
        RequestStatus.COMPLETATA_DAL_VOLONTARIO -> R.color.stato_attesa_bg
        RequestStatus.CONFERMATA -> R.color.stato_confermata_bg
        RequestStatus.ANNULLATA -> R.color.stato_annullata_bg
    }

    // Colore del TESTO della pillola per ogni stato.
    fun testo(stato: RequestStatus): Int = when (stato) {
        RequestStatus.APERTA -> R.color.stato_aperta_fg
        RequestStatus.PRESA_IN_CARICO -> R.color.stato_incarico_fg
        RequestStatus.COMPLETATA_DAL_VOLONTARIO -> R.color.stato_attesa_fg
        RequestStatus.CONFERMATA -> R.color.stato_confermata_fg
        RequestStatus.ANNULLATA -> R.color.stato_annullata_fg
    }
}
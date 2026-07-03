package com.careconnect.model

/**
 * Stato del ciclo di vita di una REQUEST.
 * Il valore "FIRESTOREVALUE" è quello effettivamente salvato su Firestore.
 */
enum class RequestStatus(val firestoreValue: String) {
    APERTA("aperta"),
    PRESA_IN_CARICO("presa_in_carico"),
    COMPLETATA_DAL_VOLONTARIO("completata_dal_volontario"),
    CONFERMATA("confermata"),
    ANNULLATA("annullata");

    /**
     * Verifica se la transizione da questo stato a "TARGET" è ammessa dal workflow.
     * Regole:
     * - DA APERTA A: presa in carico da un volontario, oppure annullata dall'anziano
     * - DA PRESA_IN_CARICO A: completata dal volontario, rilasciata (torna aperta), o annullata dall'anziano
     * - DA COMPLETATA_DAL_VOLONTARIO A: confermata (solo tramite rating di familiare/anziano)
     * - CONFERMATA / ANNULLATA: stati terminali, nessuna transizione possibile in più
     */
    fun canTransitionTo(target: RequestStatus): Boolean = when (this) {
        APERTA -> target == PRESA_IN_CARICO || target == ANNULLATA
        PRESA_IN_CARICO -> target == COMPLETATA_DAL_VOLONTARIO || target == APERTA || target == ANNULLATA
        COMPLETATA_DAL_VOLONTARIO -> target == CONFERMATA
        CONFERMATA -> false
        ANNULLATA -> false
    }

    companion object {
        fun fromFirestoreValue(value: String): RequestStatus =
            entries.firstOrNull { it.firestoreValue == value }
                ?: throw IllegalArgumentException("Stato richiesta sconosciuto: $value")
    }
}
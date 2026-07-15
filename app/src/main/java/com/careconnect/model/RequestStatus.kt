package com.careconnect.model

/**
 * rappresenta lo stato del ciclo di vita di una richiesta di aiuto.
 * il valore firestoreValue è quello effettivamente salvato su Firestore
 */
enum class RequestStatus(val firestoreValue: String) {
    APERTA("aperta"),
    PRESA_IN_CARICO("presa_in_carico"),
    COMPLETATA_DAL_VOLONTARIO("completata_dal_volontario"),
    CONFERMATA("confermata"),
    ANNULLATA("annullata");

    /**
     * funzione per verificare se la transizione da questo stato a uno stato target è
     * ammessa dal workflow della richiesta. le regole sono:
     * - da aperta: si può passare a presa in carico (volontario) o annullata (anziano)
     * - da presa in carico: si può passare a completata dal volontario, tornare
     *   aperta (il volontario rinuncia), oppure annullata
     * - da completata dal volontario: si può passare solo a confermata,
     *   tramite il rating lasciato da familiare o anziano
     * - confermata e annullata sono stati terminali: nessuna transizione possibile
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
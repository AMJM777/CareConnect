package com.careconnect.repository

import com.careconnect.model.User

//interfaccia per la gestione dei profili utente
interface UserRepository {

    // funzione per salvare (creare o sovrascrivere) il profilo utente su Firestore, alla registrazione
    suspend fun salvaUtente(user: User): Result<Unit>

    // funzione per leggere il profilo utente per uid
    suspend fun getUtente(uid: String): Result<User>


    //funzione che restituisce il codice invito dell'anziano indicato.
    //se non esiste ancora lo genera, verificando che sia univoco, e lo salva

    suspend fun ottieniOCreaCodiceInvito(anzianoId: String): Result<String>

    // funzione per cercare l'anziano proprietario di un dato codice invito,
    // usata dalla schermata "collegati al tuo assistito" del familiare
    suspend fun trovaAnzianoPerCodiceInvito(codice: String): Result<User>


     //funzione per collegare un familiare a un anziano:
     //aggiunge familiareId alla lista dell'anziano e imposta
     //anzianoCollegatoId sul familiare

    suspend fun collegaFamiliareAdAnziano(anzianoId: String, familiareId: String): Result<Unit>

    //funzione che ricalcola la media aritmetica delle stelle di tutte le valutazioni
    //ricevute dal volontario indicato, e aggiorna il campo ratingMedio sul suo profilo

    suspend fun aggiornaRatingMedio(volontarioId: String): Result<Unit>



    //funzione per salvare il token FCM del dispositivo sul profilo utente
    suspend fun aggiornaFcmToken(uid: String, token: String): Result<Unit>
}
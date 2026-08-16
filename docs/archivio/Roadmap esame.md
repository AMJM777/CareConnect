# Roadmap di Sviluppo — CareConnect
**Esame di Programmazione Mobile — Scadenza consegna: 17 luglio 2026**
**Periodo di lavoro: 1 → 16 luglio (16 giorni, tempo pieno) + 1 giorno di margine**
**Aggiornamento al 4 luglio: Fasi 0-5 completate. Fase 5 estesa oltre il piano originale (Profilo Volontario con bio, race condition su `aggiornaStato()` risolta in anticipo). Inserita una nuova Fase 7 dedicata ("Visibilità del volontario per Anziano e Familiare"), su richiesta esplicita, subito dopo la Fase 6: tutte le fasi successive sono state rinumerate e spostate di un giorno. Questo azzera completamente il giorno di margine originariamente previsto.**

**Aggiornamento al 5 luglio: Fase 7 completata (incluso un bug reale trovato e corretto in produzione: il logout del Volontario non resettava lo stato condiviso di autenticazione, causando un rimbalzo alla home invece del logout — vedi Project_State §7). Inserita una nuova Fase 8 ("Riconfigurazione Home Anziano + SOS parziale"), su richiesta esplicita — promuove a fase vera un'idea già segnata nel backlog. Le fasi successive sono state rinumerate e spostate di un giorno, ESATTAMENTE come già avvenuto per la Fase 7. Per non sforare la scadenza del 17 luglio (il giorno aggiuntivo avrebbe fatto slittare la Rifinitura DOPO la Consegna), la Fase Geolocalizzazione è stata ritirata ufficialmente dal piano d'esame — non più "sacrificabile se manca tempo", ma tolta ora, di proposito, spostata nel backlog tesi (Project_State §9). Il margine torna a zero (non negativo), stessa situazione di rischio già presente da dopo l'inserimento della Fase 7.**

**Aggiornamento al 5 luglio (sera): Fase 8 completata — Home Anziano ridisegnata (2 pulsanti + tab Profilo), bottone SOS funzionante con fix multi-familiare, banner realtime lato Familiare. Prossimo step: Fase 9 (Rating), consigliata una nuova chat — vedi Project_State §8.**

**Aggiornamento al 7 luglio: Fasi 9 e 10 completate. Fase 9 (Rating): `ratingMedio` calcolato con media da query (Opzione A). Fase 10 (Security Rules): regole vere basate su ruolo/uid pubblicate, semplificate su richiesta esplicita per allinearsi al livello del corso, validate a livello logico (8/11 test nel Playground; i 3 restanti falliscono per un limite noto del Playground con `get()` cross-document, non per un difetto delle regole). Preparata una scheda orale di giustificazione riga-per-riga. IMPORTANTE: durante il test su dispositivo fisico (Samsung SM-A546B, Android 13) sono emersi diversi bug reali — soprattutto un cluster grave di navigazione/tasto Indietro — che vanno risolti SUBITO in una chat dedicata, PRIMA della rifinitura estetica e delle Fasi 11-12. Vedi la nuova sezione "Bug dispositivo fisico — da svolgere subito" più sotto e Project_State §10. È stato preparato un documento di handoff (`HANDOFF_Bug_Dispositivo_Fisico.md`) da incollare in una chat nuova.**

**Aggiornamento all'8 luglio: sessione "Bugfix navigazione + Restyling grafico". ✅ RISOLTI i bug del dispositivo fisico A1 (navigazione/tasto Indietro — cluster grave), A2 (contrasto testo) e A6 (nuovo: logout Volontario che usciva dall'app). Comportamento Indietro ora identico e prevedibile su tutti i ruoli (una Toolbar per ruolo "strada B", callback basato su `popBackStack()`, logout con `popUpTo(graph.id)`). AVVIATA e in gran parte completata la revisione grafica (Fase 13): palette agganciata al tema (blu-indaco #4A5AD9 + arancione #F26522), bottom bar viola, card richieste con colori di stato, dashboard Anziano ridisegnata, restyling dei 3 profili, bottoni azione arancioni. RESTANO nel backlog di rifinitura: il "blocco insets" (barra in alto Anziano più corta su Home + bottom bar A4 quadrata, stessa radice edge-to-edge — 3 tentativi falliti, prossima strada: togliere `enableEdgeToEdge()`), il colore delle card (da rivedere), la freccia su "Accedi", il wording, lo Step F dell'arancione. Documento di handoff aggiornato: `HANDOFF_2_Stato_e_Prossimi_Passi.md`. Prossimo lavoro grosso: Fasi 11 (Background Task) e 12 (FCM), consigliata una chat nuova.**

> Legenda stato: `[ ]` da fare · `[~]` in corso · `[x]` completato

---

## Fase 0 — Setup progetto ✅ COMPLETATA (1 luglio)
- [x] Creazione progetto Android Studio (Kotlin, XML/Views, minSdk 26)
- [x] Creazione progetto Firebase e collegamento (google-services.json)
- [x] Configurazione Gradle (version catalog, plugin Google Services)
- [x] Risoluzione conflitto Kotlin built-in (AGP 9.2.1)
- [x] Dipendenze Firebase Auth + Firestore aggiunte, build funzionante

---

## Fase 1 — Fondamenta architetturali ✅ COMPLETATA (1-2 luglio)
*Copre: Architettura UI, base per DataBinding/ViewModel*
- [x] Struttura pacchetti MVVM (`model`, `repository`, `viewmodel`, `ui`, `util`)
- [x] Attivazione ViewBinding + DataBinding in `build.gradle.kts`
- [x] Definizione data model: `User`, `Request`, `Rating`, `SosAlert` (data class) + `UserRole`, `RequestStatus`, `SosStatus` (enum con mapping Firestore e, per `RequestStatus`, validazione transizioni)
- [x] Progettazione struttura Firestore (collezioni e relazioni) — confermata, vedi `Project_State.md` §5
- [x] Repository base per accesso a Firestore: un repository per collezione (`RequestRepository`, `UserRepository`, `RatingRepository`, `SosRepository`), pattern `suspend fun` + `Flow`, `Result<T>` per errori, mapping manuale Firestore↔dominio
- [x] Dipendenze coroutine (`kotlinx-coroutines-core`, `kotlinx-coroutines-play-services`) aggiunte al version catalog

**Decisioni prese in questa fase (dettaglio in Project_State.md §2):**
- Annullamento richiesta: solo l'anziano autore, solo da `APERTA`/`PRESA_IN_CARICO`; il volontario può "rilasciare" una richiesta presa in carico (torna `APERTA`)
- Rating + conferma richiesta: operazione atomica via Firestore Transaction, non due scritture separate
- `UserRepository` tenuto a scope minimale (solo `salvaUtente`/`getUtente`); crescerà organicamente nelle fasi che lo richiedono (2, 5, 7, 9)
- `SosRepository` include già il `Flow` realtime per il familiare, anticipando le Fasi 8 e 12

---

## Fase 2 — Autenticazione ✅ COMPLETATA (3 luglio)
*Copre: Autenticazione e Registrazione (5pt)*
- [x] Layout XML: Login, Registrazione (con scelta ruolo tramite RadioGroup)
- [x] `AuthViewModel` + `AuthRepository` (Firebase Auth email/password)
- [x] Integrazione Google Sign-In (Credential Manager)
- [x] Schermata dedicata al primo accesso Google (`CompletaProfiloGoogleFragment`, nome pre-compilato + scelta ruolo)
- [x] Logout (`AuthRepository.logout()`, non ancora richiamato da alcuna UI — vedi Fase 4)
- [x] Salvataggio profilo utente su Firestore alla registrazione — `UserRepository.salvaUtente()`
- [x] Testato end-to-end su emulatore (email/password e Google, utente nuovo ed esistente)
- [ ] Gestione sessione/auto-login all'avvio — **rimandata a Fase 4** (vedi nota sotto)

**Decisioni prese in questa fase (dettaglio in Project_State.md §2):**
- SDK Firebase Auth diretto invece di FirebaseUI (mostrata a lezione): incompatibile con Single-Activity + Navigation Component, e non supporta il campo custom "ruolo"
- Google Sign-In via `CredentialManager`, non `GoogleSignInClient` (deprecata)
- `AuthRepository` gestisce solo le credenziali (uid/email/nome); ruolo e nome profilo restano a `UserRepository`, orchestrati da `AuthViewModel`
- Palette colori (`colors.xml`) e dimensioni accessibili (`dimens.xml`) definite in questa fase, riutilizzate in tutta l'app

**Nota:** il check di sessione (se l'utente ha già una sessione Firebase valida, saltare il login) richiede `MainActivity`/Navigation Component, non ancora pronti al momento della Fase 2 — spostato come primo task della Fase 4.

---

## Fase 3 — Navigazione e shell dell'app ✅ COMPLETATA (3 luglio)
*Copre: Navigation Component e Toolbar (2pt), parte di Architettura UI (4pt)*
- [x] `MainActivity` come host di Navigation Component (`NavHostFragment`)
- [x] Navigation Graph con destinazioni condizionate al ruolo utente (`nav_graph_main` include `nav_graph_auth` + 3 home per ruolo)
- [x] Toolbar collegata a Navigation Component (`AppBarConfiguration` + `setupWithNavController`), titolo dinamico per schermata
- [x] Gestione back stack corretta (`popUpTo`/`popUpToInclusive` da Registrazione verso Login, e dal flusso auth verso le home)
- [x] Home placeholder per i 3 ruoli (`HomeAnzianoFragment`, `HomeVolontarioFragment`, `HomeFamiliareFragment`), per avere destinazioni valide da testare
- [x] Testato end-to-end: login (email ed esistente via Google) → routing automatico alla home del ruolo corretto
- [ ] Menu/BottomNavigation differenziato per ruolo — **rimandata a Fase 4** (vedi nota sotto)

**Decisioni prese in questa fase (dettaglio in Project_State.md §2):**
- Architettura Single-Activity confermata: un solo `NavHostFragment`, nessuna Activity secondaria
- `AuthUiState.Autenticato` esteso per portare anche `ruolo: UserRole` (prima mancava per il login email di un utente già esistente — bug potenziale corretto in questa fase)
- Funzione condivisa `Fragment.navigaAllaHomePerRuolo()` per evitare di duplicare la logica di routing in 3 Fragment diversi
- Toolbar con `minHeight` invece di `layout_height` fisso, per gestire correttamente il padding degli inset di sistema con `enableEdgeToEdge()`

**Nota:** la BottomNavigation richiede che le home per ruolo abbiano più di una destinazione reale (es. "Nuova richiesta" + "Le mie richieste" per l'Anziano) — con solo placeholder attuali sarebbe stata banale e da riscrivere. Spostata come task esplicito della Fase 4.

---

## Fase 4 — Modulo Anziano: richieste di aiuto ✅ COMPLETATA (3-4 luglio)
*Copre: Firebase CRUD, Complessità, Layout, completamento Navigation Component/Toolbar*
- [x] **Check di sessione/auto-login** (rimandato da Fase 2/3): `SplashFragment` come vero `startDestination`, legge `AuthRepository.utenteCorrente()` + `SessionCache` (cache locale ruolo), fallback su Firestore solo se necessario
- [x] **BottomNavigation per la home Anziano** (rimandata da Fase 3): grafo annidato per ruolo (`nav_graph_anziano.xml`), non bottom nav globale — vedi decisione in Project_State §2
- [x] Fragment "Nuova richiesta" (form: tipo aiuto con set chiuso di opzioni, descrizione)
- [x] `NuovaRichiestaViewModel` + scrittura su Firestore (Create) — usa `RequestRepository.creaRichiesta()`
- [x] Fragment "Le mie richieste" con RecyclerView (Read, stato in tempo reale) — usa `osservaRichiestePerAnziano()`
- [x] Modifica/eliminazione richiesta (Update/Delete) — eliminazione = annullamento (soft-delete via `aggiornaStato`), modifica = nuovo metodo `RequestRepository.modificaRichiesta()`, permessa solo se `APERTA`
- [x] Layout semplificato: testi grandi, icone chiare, poche azioni per schermata (coerente con palette/dimensioni accessibili già definite in Fase 2)
- [x] Azione di logout — bottone in Dashboard Anziano, con conferma, pulisce anche `SessionCache`
- [x] Chiarito `tipo` richiesta: set chiuso di opzioni (RadioGroup) con "Altro" configurabile via testo libero facoltativo

**Decisioni prese in questa fase (dettaglio in Project_State.md §2):**
- Grafo annidato per ruolo (non bottom nav globale) per isolare completamente la sotto-navigazione di ogni ruolo
- Collegamento bottom nav ↔ NavController fatto a mano (non `NavigationUI.setupWithNavController()`), per evitare un back stack inconsistente causato da `saveState`/`restoreState`
- `SessionCache` (SharedPreferences) iniettata in `AuthViewModel`, per evitare una query Firestore a ogni avvio solo per conoscere il ruolo
- Eliminazione richiesta trattata come annullamento (stato → `ANNULLATA`), non cancellazione fisica: zero modifiche allo schema, storico preservato
- RecyclerView con `Adapter` classico + `notifyDataSetChanged()`, non `ListAdapter`/`DiffUtil`, per coerenza con quanto visto a lezione
- Campo "data" della Roadmap chiarito come riferimento a `timestampCreazione` automatico, nessun campo nuovo nel modello `Request`

**Problema di ambiente incontrato (non un bug di codice):** il tasto Indietro disegnato sullo schermo dell'AVD in uso non genera un vero evento di sistema (confermato con test isolato su `MainActivity`); con ESC da tastiera funziona correttamente. Vedi nota in Project_State §7 — **da risolvere/cambiare emulatore prima della demo d'esame.**

**Nota (Fase 7, con effetto retroattivo qui):** `MieRichiesteFragment`/`RichiesteAdapter` di questa fase verranno riaperti in Fase 7 per mostrare il nome del volontario che ha preso in carico la richiesta — non un difetto di questa fase, solo un'estensione pianificata più avanti.

---

## Fase 5 — Modulo Volontario: gestione richieste ✅ COMPLETATA (Giorno 8-9 · 8-9 luglio)
*Copre: Firebase CRUD, Complessità*
- [x] BottomNavigation per la home Volontario — 3 tab (`nav_graph_volontario.xml`): **Richieste disponibili** (startDestination) → **Le mie richieste** → **Profilo**
- [x] Fragment "Richieste disponibili" — RecyclerView su `osservaRichiesteAperte()` (già pronto da Fase 1), adapter dedicato con singola azione
- [x] Azione "Prendi in carico" — `RequestRepository.aggiornaStato()` con `nuovoVolontarioId`, ora reso atomico con `Transaction` (vedi decisioni sotto)
- [x] Fragment "Le mie richieste prese in carico" — nuovo metodo repository `osservaRichiestePerVolontario()`, filtra solo stati "attivi" (`PRESA_IN_CARICO`/`COMPLETATA_DAL_VOLONTARIO`)
- [x] Azione "Segna come completata" (stato → `completata_dal_volontario`, in attesa di conferma del garante)
- [x] Azione "Rilascia richiesta" (torna `aperta`, reset `volontarioId`) — **con dialog di conferma**
- [x] **Profilo Volontario** (aggiunto oltre al piano originale, su richiesta esplicita): nome, email, ruolo, valutazione (placeholder fino a Fase 9), **descrizione di sé (bio) modificabile**, logout
- [x] Testato end-to-end: prendi in carico → rilascia → torna disponibile; prendi in carico → segna completata → resta in attesa conferma; bio salvata e verificata persistente su Firestore

**Decisioni prese in questa fase (dettaglio in Project_State.md §2):**
- **Home screen Volontario senza Dashboard separata** (a differenza dell'Anziano): ragionamento UX, il Volontario è un utente *task-oriented* (stesso schema delle app "a incarichi", es. Uber driver/Glovo) — si apre direttamente su "Richieste disponibili" invece che su una schermata di solo benvenuto. Il logout, di conseguenza, vive nel tab **Profilo**, non nella home
- **Race condition su `aggiornaStato()` risolta qui, in anticipo rispetto al piano originale** (era prevista come rischio accettato, da rivalutare solo in Fase 13): convertita in `firestore.runTransaction { }` (stesso pattern già usato in `RatingRepositoryImpl`), per evitare che due volontari possano "vincere" la stessa richiesta premendo "Prendi in carico" nello stesso istante
- Due Adapter distinti per Anziano e Volontario (`RichiesteAdapter` vs `RichiesteDisponibiliAdapter`/`RichiestePreseInCaricoAdapter`), non uno generico parametrizzato: azioni diverse per ruolo, più semplice da spiegare singolarmente all'orale
- Aggiunto campo `bio: String?` al modello `User` (opzionale, valorizzato solo dal Volontario per ora): riusa `UserRepository.salvaUtente()` esistente, nessun nuovo metodo — attenzione documentata nel codice: è un `.set()` completo, non un update parziale, va sempre passato l'oggetto `User` intero
- **Immagine profilo**: valutata (Firebase Storage vs avatar semplice vs rimandare) e **rimandata di proposito** come possibile estensione per la tesi — vedi Project_State §9 (backlog)

---

## Fase 6 — Modulo Familiare/Garante ✅ COMPLETATA (Giorno 10 · 10 luglio)
*Copre: Complessità, relazioni tra utenti*
- [x] Modello dati esteso: relazione Anziano↔Familiare **1:N** (un anziano può avere più familiari/garanti collegati), non 1:1 come inizialmente pianificato — `User.familiariCollegatiIds: List<String>` (solo Anziano) / `User.anzianoCollegatoId: String?` (solo Familiare, resta singolare: un familiare segue un solo assistito)
- [x] Meccanismo di collegamento: codice invito breve generato (6 caratteri, alfabeto senza caratteri ambigui), univoco per anziano, riutilizzabile per più familiari nel tempo — il Familiare inserisce il codice dell'Anziano
- [x] `UserRepository` esteso: `ottieniOCreaCodiceInvito()`, `trovaAnzianoPerCodiceInvito()`, `collegaFamiliareAdAnziano()` (scrittura atomica con `WriteBatch` + `FieldValue.arrayUnion`, guardia contro doppio collegamento)
- [x] BottomNavigation per la home Familiare — 2 tab (`nav_graph_familiare.xml`): **Attività** (startDestination) + **Profilo**, agganciata a runtime solo dopo conferma del collegamento (non staticamente in XML, a differenza di Anziano/Volontario)
- [x] Tab "Attività": lista realtime delle richieste dell'anziano collegato (stato attuale + storico in un'unica vista) + azione di **conferma finale** e valutazione (dialog con `RatingBar` + commento facoltativo) — usa `RatingRepository.creaRatingEConfermaRichiesta()` già pronto
- [x] Tab "Profilo": nome proprio, nome dell'anziano seguito, logout
- [x] Testato end-to-end: anziano genera codice → familiare si collega → volontario completa richiesta → familiare conferma e valuta

**Decisioni prese in questa fase (dettaglio in Project_State.md §2):**
- Relazione 1:N (non 1:1): un anziano può avere più familiari, ogni familiare segue un solo anziano — cambio deciso esplicitamente rispetto al piano originale, dopo aver verificato che estenderla in un secondo momento non avrebbe richiesto una riscrittura (query Firestore `whereIn` invece di `whereEqualTo`)
- Codice invito generato (non l'UID Firebase mostrato direttamente): più leggibile per un utente anziano, con controllo di unicità lato repository
- `HomeFamiliareFragment` aggancia il grafo annidato a runtime, non staticamente in XML: a differenza di Anziano/Volontario esiste uno stato "non ancora collegato" che non deve far partire prematuramente le schermate che presuppongono un anziano già collegato

---

## Fase 7 — Visibilità del volontario per Anziano e Familiare ✅ COMPLETATA (Giorno 11 · 11 luglio)
*Nuova fase, aggiunta su richiesta esplicita dopo la Fase 5 — non copre una voce di punteggio autonoma, ma rifinisce la Complessità/Layout già coperte da Fase 4/5/6, rendendo lo stato delle richieste davvero leggibile per chi non è il volontario. Estesa oltre il piano originale: anche indirizzo dell'Anziano e profilo pubblico del Volontario.*

- [x] Campo `volontarioNome: String?` aggiunto a `Request` (denormalizzato) — **Opzione B scelta**: il ViewModel recupera il nome con `UserRepository.getUtente()` prima di chiamare `aggiornaStato()`, che resta "puro" (legge/scrive solo `requests`)
- [x] **Estensione oltre il piano originale**: campo `indirizzo: String?` aggiunto a `User` (solo Anziano, fisso sul profilo, non per singola richiesta) — necessario perché senza indirizzo un volontario che accetta una richiesta non saprebbe dove andare; `NuovaRichiestaFragment` blocca la creazione se l'indirizzo non è ancora impostato
- [x] Campi `autoreNome`/`autoreIndirizzo` aggiunti a `Request` (denormalizzati dal profilo dell'Anziano alla creazione)
- [x] Descrizione richiesta resa più guidata (hint dinamico per tipo, con esempio concreto) invece di una vera chat anziano↔volontario (valutata e rimandata, vedi Project_State §9)
- [x] `RichiesteAdapter`/`item_richiesta.xml` (Anziano) e `AttivitaFamiliareAdapter`/`item_richiesta_familiare.xml` (Familiare) aggiornati: "Volontario: [nome]", cliccabile
- [x] **Estensione oltre il piano originale**: profilo di sola lettura del Volontario (nome, bio, valutazione), raggiungibile toccando il nome — dialog condiviso (`ui/common/ProfiloVolontarioDialog.kt`) tra Anziano e Familiare
- [x] `RichiestePreseInCaricoAdapter`/`item_richiesta_incarico.xml` (Volontario): nome e indirizzo dell'Anziano visibili **solo dopo** "Prendi in carico", non prima — privacy: `RichiesteDisponibiliAdapter` (richieste ancora aperte, visibili a tutti i volontari) resta invariato
- [x] Testato end-to-end: volontario prende in carico → Anziano e Familiare vedono subito nome/profilo, in tempo reale (nessun refresh manuale)
- [x] **Bug reale trovato e corretto in produzione**: `ProfiloVolontarioViewModel` aveva un proprio `logout()` che disconnetteva Firebase ma non resettava lo stato condiviso di `AuthViewModel`, causando un rimbalzo immediato alla Home Volontario invece del logout — allineato allo stesso schema già usato da Anziano/Familiare (dettaglio in Project_State §7)

**Decisioni prese in questa fase (dettaglio in Project_State.md §2):**
- Opzione B confermata per `volontarioNome` (coerenza con "un repository per collezione")
- Indirizzo sul profilo dell'Anziano (fisso), non per singola richiesta: più realistico, evita di doverlo reinserire ogni volta
- Descrizione guidata invece di chat vera: stesso beneficio pratico (l'anziano è più preciso), zero nuova infrastruttura realtime
- Privacy: nome/indirizzo dell'Anziano visibili al Volontario solo dopo l'accettazione, mai nella lista "Richieste disponibili"

---

## Fase 8 — Riconfigurazione Home Anziano + SOS parziale ✅ COMPLETATA (Giorno 12 · 12 luglio)
*Nuova fase, aggiunta su richiesta esplicita dopo la Fase 7 — promuove a fase vera un'idea già segnata nel backlog (Project_State §9, "Home Anziano ridisegnata..."). Copre: rifinitura Layout/Complessità del modulo Anziano (Fase 4), anticipa la parte "bottone SOS locale" della Fase 12 (ex Fase 11)*

- [x] Ristrutturato `nav_graph_anziano.xml` / `bottom_nav_anziano.xml`: da 3 tab (Dashboard / Nuova richiesta / Le mie richieste) a 3 tab (**Home**, con 2 pulsanti / **Le mie richieste**, invariata / **Profilo**, nuova)
- [x] `DashboardAnzianoFragment` semplificato a 2 soli pulsanti: "Nuova richiesta" (naviga al Fragment esistente, invariato) e "SOS"
- [x] Nuovo `ProfiloAnzianoFragment` + ViewModel: nome, email, ruolo, indirizzo (modificabile, spostato da Dashboard), codice invito (spostato da Dashboard), logout (spostato da Dashboard)
- [x] Bottone SOS: `Intent.ACTION_DIAL` verso il 112 + scrittura `SosAlert` su Firestore, con dialog di conferma prima dell'invio
- [x] **Fix multi-familiare applicato**: un documento `SosAlert` per ciascun familiare in `familiariCollegatiIds` (loop nel ViewModel), non un cambio di schema
- [x] Vista lato Familiare: banner in cima al tab "Attività" quando esiste un `SosAlert` con stato `ATTIVO`, con azione "Ho visto, chiudi" (→ `CHIUSO`) — realtime via `osservaAlertPerFamiliare()`
- [x] Testato end-to-end: Anziano preme SOS → dialog di conferma → compositore verso il 112 + familiari avvisati → banner visibile in tempo reale, chiusura funzionante

**Decisioni prese in questa fase (dettaglio in Project_State.md §2):**
- Dialog di conferma prima dell'invio SOS (non invio diretto al tap): un falso allarme costa di più di un secondo perso in un'emergenza vera, stesso principio già usato per logout/annulla/rilascia
- `try/catch` attorno a `ACTION_DIAL` per gestire il caso limite (raro) di un dispositivo/emulatore senza app telefono, senza far crashare l'app
- Banner SOS mostra solo l'alert `ATTIVO` più recente (non una lista): più chiaro da leggere in un momento di emergenza
- **Bug di compilazione trovato e corretto durante il testing**: nel riscrivere `AttivitaFamiliareFragment` per aggiungere il banner SOS, il parametro `onVolontarioClick` (aggiunto in Fase 7) era stato perso per errore — segnalato dal compilatore stesso ("No value passed for parameter"), corretto subito. Nota utile per l'orale: più modifiche successive sullo stesso file aumentano il rischio di questo tipo di regressione, motivo in più per rileggere sempre il file attuale prima di riscriverlo per intero

---

## Fase 9 — Sistema di valutazione a stelle ✅ COMPLETATA (Giorno 13 · 13 luglio)
*Copre: Complessità, Firebase CRUD*
- [x] Componente UI rating a stelle — **RatingBar standard di Android** (già presente in `dialog_conferma_valutazione.xml` da fasi precedenti; nessuna libreria esterna). Rifinitura estetica delle stelle rimandata (vedi Fase 13 e nota sotto)
- [x] Scrittura rating su Firestore al completamento richiesta — già collegata (Transaction `creaRatingEConfermaRichiesta`)
- [x] Calcolo/visualizzazione rating medio volontario — **scelta: Opzione A (media da query)**. Aggiunto `UserRepository.aggiornaRatingMedio(volontarioId)`: interroga tutti i `Rating` del volontario, calcola la media aritmetica, aggiorna `ratingMedio` con `update()` parziale. Chiamato da `AttivitaFamiliareViewModel.confermaEValuta()` dopo il successo del rating (non nella stessa Transaction: una query su collezione non è ammessa dentro le Transaction Firestore — rischio di inconsistenza accettato e documentato, si autocorregge al rating successivo)

**Nota — grafica stelle rating (richiesta esplicita, 6 luglio):** l'utente vuole in futuro un aspetto più curato del componente a stelle. Primo tentativo previsto in Fase 13 con personalizzazione XML nativa del RatingBar (style + drawable custom, basso costo); se il livello desiderato richiede di più, spostare nel backlog tesi. Decidere quando si arriva a quella fase.

## Fase 10 — Firestore Security Rules ✅ COMPLETATA (validata a livello logico) (Giorno 14 · 14 luglio)
*Copre: Integrazione Firebase (5pt) — parte security rules*
- [x] Sostituite le regole di test con regole vere basate su ruolo/uid, **pubblicate**
- [x] Regole per `users`: lettura del proprietario + profilo volontario pubblico + anziano leggibile dal familiare; scrittura del proprio profilo senza cambiare ruolo; aggiornamento `ratingMedio` da parte del garante
- [x] Regole per `requests`: creazione solo da anziano; macchina a stati completa (aperta→presa_in_carico→completata→confermata) con controllo di CHI può fare ogni transizione
- [x] Regole per `ratings`: creazione solo dal garante, stelle intere 1-5, no update/delete
- [x] Regole per `sosAlerts`: creazione solo da anziano verso il proprio familiare, update stato dal familiare destinatario
- [x] Test regole (Rules Playground): 8/11 casi passano direttamente; i 3 basati su `get()` cross-document falliscono per un **limite noto del Playground nella sessione**, NON per un difetto delle regole (dimostrato: lettura diretta OK, `get()` interno KO sullo stesso documento). Diagnosi confermata anche dalla documentazione ufficiale Firebase (il Playground non copre tutti i casi avanzati)
- [x] Semplificazione su richiesta esplicita: tolti quasi tutti i `diff().hasOnly()`, ridotti gli helper a `isSignedIn`/`getRuolo`/`getAnzianoCollegato` (quest'ultima usa `.data.get('campo', null)` per robustezza), per allinearsi al livello dei pattern del corso mantenendo però le regole necessarie alla logica dell'app
- [x] Preparata **scheda orale di giustificazione riga-per-riga** (`Scheda_Orale_Security_Rules.md`)
- [ ] **RESIDUO:** completare il **percorso felice sull'app** per la validazione sul campo (checklist nel documento di handoff, sezione B) — da fare dopo aver sistemato i bug di navigazione del dispositivo fisico

## BUG DISPOSITIVO FISICO — STATO AGGIORNATO (8 luglio)
*Emersi il 7 luglio su Samsung Galaxy A54 (SM-A546B), Android 13, API 33. Sessione dell'8 luglio: risolti A1/A2/A6. Restano A3/A4/A5 + barra corta + colore card, spostati nel backlog di rifinitura (Fase 13). Dettaglio in `HANDOFF_2_Stato_e_Prossimi_Passi.md` e Project_State §10.*
- [x] **A1 🔴 Navigazione / tasto Indietro** — RISOLTO: callback basato su `popBackStack()`, una Toolbar per ruolo (strada B) con `AppBarConfiguration`, Toolbar Activity nascosta sulle home. Comportamento identico e prevedibile su tutti i ruoli.
- [x] **A2 🟠 Contrasto testo** — RISOLTO forzando il tema chiaro.
- [x] **A6 🟠 Logout Volontario usciva dall'app** (nuovo) — RISOLTO: `popUpTo(0)` → `popUpTo(graph.id)` sui 3 profili.
- [ ] **[insets] Barra Anziano più corta su Home** — confermato reale (limiti layout), 3 tentativi falliti; prossima strada: togliere `enableEdgeToEdge()` in `MainActivity`. Stessa radice di A4.
- [ ] **A4 🟡 Bottom bar quadrata** — non copre tutta la parte bassa (profilo Volontario) su Android 13. Blocco insets/edge-to-edge, da chiudere insieme alla barra corta.
- [ ] **[NUOVO] Colore delle card** — l'utente non gradisce il colore attuale; ritoccare lo stile `CareConnect.Card`.
- [ ] **A3 🟠 Tastiera copre l'input (Anziano)** — `windowSoftInputMode`/`adjustResize` + scroll (i profili sono già in `NestedScrollView`).
- [ ] **A5 🟡 Colori** — gran parte fatta; resta eventuale desaturazione lieve, freccia su "Accedi", wording, Step F arancione.
- [ ] **Percorso felice Security Rules sull'app** — checklist B dell'handoff (ora che la navigazione funziona).

## Fase 11 — Background Task (Giorno 15 · 15 luglio)
*Copre: Background Task / Service (2pt)*
- [ ] Identificazione task adatto (es. WorkManager per controllo periodico richieste scadute, o notifica promemoria)
- [ ] Implementazione con Coroutine/WorkManager
- [ ] Verifica comportamento con app in background

## Fase 12 — Notifiche push (Giorno 16 · 16 luglio)
*Copre: Funzionalità Extra (1pt). Bottone SOS e scrittura `sosAlerts` anticipati in Fase 8: qui resta solo la parte FCM.*
- [ ] Setup Firebase Cloud Messaging
- [ ] Notifica push al familiare quando l'anziano lancia SOS (il bottone e la scrittura Firestore sono già pronti dalla Fase 8 — qui resta solo FCM, il banner in-app esiste già)
- [ ] Notifica locale al volontario per nuove richieste in zona (opzionale, se tempo)

**RITIRATA — Geolocalizzazione:** era prevista come Fase 12 (Extra, 1pt, cumulabile/alternativa alle notifiche): permessi posizione, associazione posizione a richiesta, filtro/ordinamento per vicinanza. Ritirata ufficialmente il 5 luglio per fare spazio alla Fase 8 senza sforare la scadenza — vedi nota di aggiornamento in cima al documento. Spostata nel backlog tesi, Project_State §9.

## Fase 13 — Rifinitura e Testing (Giorno 16-17 · 16-17 luglio) 🔧 IN CORSO
*Copre: riduzione malus, qualità complessiva*
- [~] **Revisione grafica — AVVIATA e in gran parte fatta (8 luglio):** palette agganciata al tema (blu-indaco + arancione), bottom bar viola, card richieste con colori di stato, dashboard Anziano ridisegnata, restyling dei 3 profili (avatar + card + scroll), bottoni azione arancioni. **Resta:** blocco insets (barra corta Home + bottom bar A4), colore card da rivedere, freccia su "Accedi", wording, Step F arancione. Vedi `HANDOFF_2_Stato_e_Prossimi_Passi.md` §B.
- [ ] Test manuale di tutti i flussi (3 ruoli, edge case: rotazione schermo, no rete, campi vuoti)
- [ ] **Test: Familiare con secondo account non collegato** (vista stato "non collegato")
- [ ] Verifica gestione lifecycle di Activity/Fragment (niente leak, niente crash su rotazione)
- [ ] **Coerenza grafica finale — richiesta esplicita di revisione (5 luglio)**: l'utente vuole rivedere l'aspetto visivo dell'app (palette/layout/stile), non solo un controllo di contrasto/leggibilità. Da trattare come task vero e proprio quando si arriva a questa fase, non come rifinitura automatica — chiedere in quel momento cosa esattamente si vuole cambiare, prima di modificare colori/layout già validati nelle fasi precedenti
- [ ] Controllo che non ci siano credenziali/API key esposte in modo scorretto
- [ ] Rivalutare la race condition residua su `SosRepository.aggiornaStato()` (quella di `RequestRepository` è già stata risolta in Fase 5): decidere se vale la pena convertirla in Transaction anche lì, tempo permettendo

## Fase 14 — Consegna (entro 17 luglio — **ancora l'ultimo giorno utile, zero margine residuo**)
- [ ] Export progetto in ZIP da Android Studio
- [ ] Caricamento sul form + mail a progmobile@ai-lab.it
- [ ] Iscrizione appello su EasyAcademy
- [ ] Preparazione breve demo/discorso per l'orale (10-15 min, saper commentare ogni scelta, incluse le decisioni architetturali di Fase 1-3 e la denormalizzazione di Fase 7)

---

## Note di rischio
- **Geolocalizzazione ritirata ufficialmente** (5 luglio, per fare spazio alla Fase 8): non è più nel piano d'esame, non "se avanza tempo" — è stata tolta di proposito. Se dovesse restare tempo reale a fine progetto, può essere ripresa come estensione, ma non va data per scontata.
- Se si accumula ulteriore ritardo, la **Fase 12 (notifiche push FCM) resta la più sacrificabile**: il bottone SOS locale e il banner in-app per il familiare (Fase 8) restano comunque funzionanti anche senza push — degradano l'esperienza ma non lasciano una funzionalità a metà.
- Le **Security Rules (Fase 10)** vanno fatte con calma: un errore qui può bloccare tutta l'app o lasciarla insicura (0 punti sulla voce Firebase se assenti). Le regole di test attuali scadono comunque il 2 agosto 2026, quindi c'è un margine reale ma non infinito.
- **Il giorno di margine originario è stato azzerato** dall'inserimento della Fase 7, e sarebbe diventato negativo con l'inserimento della Fase 8 se non avessimo ritirato la Geolocalizzazione per compensare. Da qui in avanti, qualunque ulteriore fase aggiuntiva o ritardo richiede un altro taglio esplicito, non solo uno spostamento di date: monitorare con attenzione.
- Testare su dispositivo/emulatore reale ogni 2-3 giorni, non solo a fine progetto.
- Fasi 0-7 completate al 5 luglio, nessun ritardo accumulato nonostante le estensioni oltre il piano originale (Fase 5, Fase 7, e ora Fase 8).
- Diversi problemi di ambiente (emulatore, Gradle, Firestore/Realtime Database, SHA-1) hanno rallentato la Fase 2, ma sono stati tutti risolti e documentati — non si sono ripresentati nelle fasi successive.
- **Nota tecnica — Ambiente di test (emersa in Fase 4):** il tasto Indietro disegnato sullo schermo dell'emulatore attualmente in uso (immagine di sistema Android recente/beta, targetSdk 36) non genera un vero evento di sistema "Indietro"; il tasto ESC da tastiera fisica sì. Confermato non essere un bug del codice tramite test isolato. **Azione da fare:** individuare o configurare un emulatore senza questo problema prima della demo d'esame.
- **Nota tecnica — DataBinding (emersa più volte in Fase 5):** ogni volta che si crea o si sostituisce interamente un layout XML con `<layout>` come tag radice, Android Studio deve rigenerare la classe `Binding` corrispondente. Se l'IDE segnala "Unresolved reference" su una classe `FragmentXxxBinding`/`ItemXxxBinding` subito dopo aver incollato un file, **il primo sospetto non deve essere un errore nel codice**: fare prima `Sync Project with Gradle Files` → `Build → Rebuild Project` (non "Make") → se persiste, `File → Invalidate Caches... → Invalidate and Restart`. Solo se dopo questi passaggi l'errore persiste, e solo guardando il pannello **Build** (non "Problems", che riflette l'indicizzazione IDE e può restare non aggiornato anche dopo un Rebuild riuscito) per righe `e:` reali, si tratta di un errore di codice genuino.

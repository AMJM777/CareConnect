# Project State — CareConnect

**Ultimo aggiornamento:** 17 agosto 2026 (TESI — **T2 "SOS ripensato" completato**: doppio trigger (bottone rosso + scuotimento accelerometro) sullo stesso percorso; overlay di conferma translucido con countdown 5→0, voce TTS "Sto per chiamare aiuto" + conteggio, e ANNULLA grande; a fine countdown `inviaSos()` (→ push FCM esistente) + `ACTION_DIAL` 112; scuotimento solo ad app aperta. Nuovi `TtsHelper`, `ShakeDetector`, `ConfermaSosDialogFragment`. Testato su device. Dettaglio in §0bis e in `Roadmap_Tesi.md`.)
> **Precedente:** 16 agosto 2026 (TESI — **T1 "Home Anziano" completato**: Home ridisegnata come form "Nuova richiesta" diretto + banner "richiesta in corso" realtime + SOS ricollocato; split creazione/modifica; nuovi `NuovaRichiestaHomeFragment`/`NuovaRichiestaHomeViewModel`; rimossi `DashboardAnziano*`. Testato su device. Dettaglio in §0bis e in `Roadmap_Tesi.md`.)
> **Precedente:** 12 luglio 2026 (sessione "Rifinitura Layout + DataBinding dichiarativo": **Fase 13 completata**. Consolidate le due voci di punteggio a rischio — Layout (3/3) e DataBinding+ViewModel (3/3). Verificate nel codice tutte e 9 le voci della griglia d'esame. Chiusi tutti i bug §10 ancora aperti (barra corta Home, A3 tastiera, A4 bottom bar, freccia Accedi, tema chiaro). Segnate come completate anche Fase 11 (Background Task) e Fase 12 (FCM), verificate nel codice. Vedi `HANDOFF_4_Fase13_Rifinitura_Completata.md`. §2/§4/§9/§10 aggiornati)
> **Precedente:** 8 luglio 2026 (sessione "Bugfix navigazione + Restyling grafico": risolti A1 navigazione/tasto Indietro, A2 contrasto, A6 logout; revisione grafica — palette agganciata al tema, bottom bar viola, card richieste, dashboard Anziano, restyling dei 3 profili. Vedi `HANDOFF_2_Stato_e_Prossimi_Passi.md`)
> **Precedente:** 7 luglio 2026 (Fasi 9 e 10 completate — Sistema rating + Security Rules; aggiunta §10 bug dispositivo fisico)
**Scadenza consegna progetto:** 17 luglio 2026

> Questo file va aggiornato da Claude solo su richiesta esplicita dell'utente ("aggiorna il Project State").
> Dopo ogni aggiornamento, l'utente deve ricaricare manualmente il file nel Project di Claude per renderlo persistente.

---

## 0bis. TESI — Aggiornamenti post-esame

> Sezione che raccoglie le modifiche fatte nella finestra tesi (dal 16 agosto 2026),
> distinte dal lavoro d'esame (Fasi 0–14, baseline consegnata). Piano attivo in `Roadmap_Tesi.md`.

### T1 — Home Anziano (16 agosto 2026) ✅
- **Home = form "Nuova richiesta" diretto.** Nuova schermata `NuovaRichiestaHomeFragment`
  + `NuovaRichiestaHomeViewModel` (un ViewModel di schermata che riunisce tre compiti:
  creazione richiesta, banner "richiesta in corso", SOS). Sostituisce la vecchia Home a
  2 pulsanti (`DashboardAnzianoFragment`).
- **Banner "richiesta in corso":** `StateFlow` realtime da `osservaRichiestePerAnziano`,
  filtrato agli stati non terminali (APERTA / PRESA_IN_CARICO / COMPLETATA_DAL_VOLONTARIO);
  visibile solo se non vuoto; tap → tab "Le mie richieste".
- **Split creazione/modifica:** `NuovaRichiestaFragment` + `NuovaRichiestaViewModel` restano
  SOLO per la modifica (rimossa `creaRichiesta` e la dipendenza `userRepository` non più usata;
  factory semplificata a `RequestRepository`).
- **SOS** ricollocato in fondo alla Home (logica invariata: un alert per ogni familiare
  collegato + `ACTION_DIAL` verso il 112). Interni (scuotimento / TTS / countdown) previsti in T2.
- **Navigazione:** `nav_graph_anziano` startDestination → `nuovaRichiestaHomeFragment`;
  `bottom_nav_anziano` prima voce "Nuova richiesta"; `HomeAnzianoFragment` AppBarConfiguration
  aggiornata al nuovo id.
- **Accessibilità:** target ≥ 56dp, testo in `sp`, palette esistente. **TTS rimandato a T2**
  (deciso con l'utente).
- **File rimossi:** `DashboardAnzianoFragment.kt`, `DashboardAnzianoViewModel.kt`,
  `fragment_dashboard_anziano.xml`.
- **Grafica:** la personalizzazione visiva della Home e dell'intera interfaccia Anziano è
  rimandata (l'utente la gestirà con skill grafiche dedicate).
- Testato end-to-end su dispositivo fisico (11 punti di verifica ok).

### T2 — SOS ripensato (17 agosto 2026) ✅
- **Doppio trigger, stesso percorso di codice:** bottone SOS rosso + **scuotimento** del telefono
  (accelerometro) confluiscono nello stesso overlay di conferma via `avviaFlussoSos()`.
- **Overlay di conferma robusto** (`ConfermaSosDialogFragment`): `DialogFragment` a tutto schermo
  con tema translucido (`CareConnect.Dialog.Sos`, la Home resta intravista sotto), countdown
  **5→0** in un cerchio rosso, tasto **ANNULLA** enorme (96dp). `isCancelable = false`: si esce
  solo con ANNULLA. Il conteggio è un job legato a `onStart()/onStop()`: uscendo dall'app si ferma
  e riprende dal numero rimasto → **nessuna chiamata in background** (bug trovato e corretto in test).
- **Voce (TTS):** nuovo `TtsHelper` riusabile (init asincrono, italiano, coda pre-init,
  `interrompi()` per la pausa, `chiudi()` per lo shutdown). Legge "Sto per chiamare aiuto" + il
  conteggio "5, 4, 3, 2, 1". (TTS era stato rimandato da T1.)
- **Scuotimento (`ShakeDetector`):** ascolta l'accelerometro e richiede più "strattoni" ravvicinati
  (soglia 2.7g, 3 strattoni in 1.5s, cooldown 3s) per distinguere uno scuotimento voluto da un
  urto singolo; legato al lifecycle della Home (`onResume/onPause`). Nessun permesso runtime.
- **Fine countdown:** l'overlay comunica l'esito alla Home via **Fragment Result**; la Home chiama
  `viewModel.inviaSos()` (un `SosAlert` per familiare → **push FCM** dalla Cloud Function
  `notificaSosAlFamiliare`, già esistente) + `ACTION_DIAL` 112. **ANNULLA prima dello zero non
  scrive nulla** (nessun falso allarme ai familiari — decisione presa con l'utente).
- **Backend invariato:** repository, modelli, Cloud Function e banner in-app al familiare erano già
  pronti (Fasi 8/12); T2 ha lavorato solo sul livello trigger + conferma.
- **v1: scuotimento solo ad app aperta.** Il background (app chiusa) richiede un Foreground Service
  ed è **rimandato come estensione prioritaria** post-T3 (vedi `Roadmap_Tesi.md` → Backlog).
- **File nuovi:** `util/TtsHelper.kt`, `util/ShakeDetector.kt`, `ui/anziano/ConfermaSosDialogFragment.kt`,
  `layout/dialog_conferma_sos.xml`, `drawable/bg_cerchio_sos.xml`, stile `CareConnect.Dialog.Sos`
  in `styles.xml`. **File modificato:** `NuovaRichiestaHomeFragment.kt` (rimosso il vecchio
  AlertDialog di conferma; aggiunti `ShakeDetector` + listener del Fragment Result).
- Testato su dispositivo fisico (doppio trigger, voce, ANNULLA, chiamata, push al familiare,
  pausa/ripresa in background, rotazione).

### T3 — Chat integrata Anziano ↔ Volontario (18–19 agosto 2026) ✅ (verifica multi-profilo rimandata)
- **Nuova collezione `messaggi`** (top-level, non sotto-collezione della richiesta). Ogni messaggio
  denormalizza `requestId`, `anzianoId`, `volontarioId` (partecipanti) + `mittenteId`, `testo`,
  `timestamp`. La denormalizzazione serve a tre cose senza query extra: security rules, query del
  garante, e Cloud Function push. Scelta motivata: coerenza con il pattern "un repository per
  collezione" e con i `callbackFlow` filtrati per campo già usati (SOS/Request).
- **Repository:** `MessageRepository` / `MessageRepositoryImpl` — `inviaMessaggio()` +
  `osservaMessaggiPerRichiesta(requestId, campoUtente, uidUtente)` con `callbackFlow` realtime,
  senza `orderBy` (ordinamento nel ViewModel, come il resto del codice → nessun indice composito).
- **UI condivisa a 3 ruoli:** una sola `ChatFragment` + `ChatViewModel` + `ChatAdapter`
  (`ui/chat/`, `viewmodel/chat/`), con due destinazioni per grafo (anziano/volontario) + una per il
  familiare. Lato **anziano** semplificato con lettura vocale (riuso `TtsHelper`, pulsante "Ascolta"
  sui messaggi ricevuti). Lato **volontario** standard. Ingresso dalle righe di "Le mie richieste"
  (anziano) e "Prese in carico" (volontario), visibile da `PRESA_IN_CARICO` in poi.
- **Ciclo di vita della chat:** scrittura consentita **solo mentre `PRESA_IN_CARICO`**; sugli stati
  successivi la chat diventa **sola lettura** (storico, barra di invio nascosta) — imposto sia in UI
  (`ARG_SOLO_LETTURA`) sia dalle rules.
- **Safeguarding — garante in sola lettura + trasparenza:** il familiare collegato apre la chat
  dell'assistito da "Attività" (pulsante "Vedi chat"), in sola lettura, con i messaggi distinti per
  ruolo (nome sopra la bolla, anziano a destra / volontario a sinistra, colori diversi). Ai **due
  partecipanti** compare un **avviso di trasparenza** ("un familiare di riferimento può leggere questa
  conversazione"); al garante no. È il punto che rende difendibile il contatto diretto tra persona
  vulnerabile e volontario: trasparenza dichiarata, non sorveglianza occulta.
- **Security rules `messaggi`** (pubblicate in console): helper `getRichiesta()`,
  `messaggioCoerente()`, `partecipanteOGarante()`. **Create** solo da un partecipante, solo se la
  richiesta è `presa_in_carico` e i campi denormalizzati combaciano con la richiesta padre (anti
  spoofing). **Read** ai due partecipanti + familiare collegato (`msg.anzianoId ==
  getAnzianoCollegato(uid)`). **Update/Delete = false**: messaggi immutabili → log di audit integro.
- **Decisione tecnica chiave (rules-not-filters):** su una query in `list` Firestore blocca l'intera
  richiesta se *potrebbe* restituire un documento non leggibile. Quindi la query deve **vincolare lo
  stesso campo controllato dalla regola**: anziano/volontario filtrano il proprio campo partecipante,
  il garante filtra `anzianoId == assistito`. (Bug trovato e corretto in test: prima si filtrava solo
  `requestId` → `PERMISSION_DENIED`.)
- **Notifiche push:** nuova Cloud Function `notificaNuovoMessaggio` (`functions/index.js`, gemella di
  `notificaSosAlFamiliare`): a ogni nuovo messaggio calcola il destinatario (il partecipante diverso
  dal mittente), legge il suo `fcmToken` e invia una push sul canale **generale** (`tipo="messaggio"`,
  gestito dal `CareConnectMessagingService` esistente — lato Android nessuna modifica). Testata: la
  notifica arriva anche a telefono bloccato.
- **File nuovi:** `model/Message.kt`, `repository/MessageRepository.kt` + `MessageRepositoryImpl.kt`,
  `ui/chat/ChatFragment.kt` + `ChatAdapter.kt` + `MessaggioDiffCallback.kt`,
  `viewmodel/chat/ChatViewModel.kt`, `layout/fragment_chat.xml` + `item_messaggio.xml`.
  **Modificati:** i tre nav graph, gli adapter/fragment/item di richieste (anziano/volontario/familiare)
  per il pulsante Chat, `functions/index.js`.
- **Grafica rimandata:** la resa visiva della chat (in tutte le viste) e il pulsante Chat NON fanno
  parte di T3; sono raccolti in una **fase grafica finale separata** (vedi §9 e `Roadmap_Tesi.md`),
  da affrontare dopo aver installato una skill grafica dedicata.
- **Testato:** percorso felice a due lati (realtime, TTS, sola lettura dopo completamento, garante in
  sola lettura, push a telefono bloccato). **Rimandati** i test negativi multi-profilo delle rules
  (volontario non assegnato, accessi negati) per mancanza di profili di prova: da svolgere più avanti.

---

## 1. Descrizione del progetto

**Nome app:** CareConnect
**Tema:** Piattaforma di supporto domiciliare per anziani, basata su una rete di volontari per piccoli aiuti quotidiani (spesa, bollette, assistenza digitale) e gestione emergenze (SOS). Tre ruoli utente: **Anziano**, **Volontario**, **Familiare/Garante**.
**Ambito lavoro:** Progetto per esame di Programmazione Mobile — verrà poi ampliato come base della tesi di laurea (l'ampliamento NON fa parte del lavoro d'esame).

## 2. Decisioni tecniche prese

| Decisione | Valore scelto |
|---|---|
| Linguaggio | Kotlin |
| UI | XML Layout + View/DataBinding (no Jetpack Compose) |
| Architettura | MVVM (ViewModel + LiveData/StateFlow) |
| Navigazione | Navigation Component + Toolbar — **costruiti in Fase 3**, completati in Fase 4, estesi in Fase 5 |
| Backend | Firebase Auth (email/password + Google) + Cloud Firestore |
| Background | Coroutine / WorkManager (da definire nel dettaglio in Fase 10) |
| Package name / applicationId | `com.careconnect.app` (allineato all'app registrata su Firebase; i package Kotlin dei sorgenti restano `com.careconnect.*`) |
| Project Android Studio | `CareConnect` |
| minSdk | 26 (Android 8.0) |
| targetSdk / compileSdk | 36 |
| AGP | 9.2.1 (Kotlin **built-in**, NON si applica più il plugin `org.jetbrains.kotlin.android` esplicitamente) |
| Lavoro individuale o gruppo | Individuale |
| Stato entità (Request/User/Sos) | `enum class` con `firestoreValue: String` + `fromFirestoreValue()`, NON String libere |
| Struttura repository | Un repository (interfaccia + impl) per collezione Firestore, non un repository unico generico |
| Lettura dati | Mix `suspend fun` (operazioni singole) + `Flow` (ascolti realtime via `callbackFlow`) |
| Gestione errori nei repository | `Result<T>` (stdlib Kotlin) per le `suspend fun`; propagazione via `close()`/eccezione per i `Flow` |
| Mapping Firestore ↔ dominio | Manuale (funzioni di estensione private su `DocumentSnapshot`/modello, dentro ogni `RepositoryImpl`), NO `toObject()` automatico |
| Aggiornamento `ratingMedio` volontario | Orchestrato dal ViewModel (Fase 8), non da Cloud Function |
| Rating + conferma richiesta | Operazione atomica tramite Firestore Transaction (`RatingRepository.creaRatingEConfermaRichiesta`) |
| **`RequestRepository.aggiornaStato()` — concorrenza** | **Convertito in Firestore Transaction in Fase 5** (anticipato rispetto al piano originale, che lo rimandava come rischio accettato a fine progetto). Motivo: con più volontari, un `get()` + `update()` separati permetteva a due di "prendere in carico" la stessa richiesta nello stesso istante. `SosRepository.aggiornaStato()` resta invece non transazionale, da rivalutare in Fase 13 |
| Autenticazione: FirebaseUI vs SDK diretto | **SDK diretto** (`FirebaseAuth` puro), NON FirebaseUI Auth. Motivo: FirebaseUI impone architettura a due Activity (Welcome/Home), incompatibile con Single-Activity + Navigation Component; inoltre non supporta il campo custom "ruolo" in registrazione |
| Google Sign-In: API scelta | `CredentialManager` (moderna, raccomandata Google), non `GoogleSignInClient` (deprecata) |
| AuthRepository: scope | Gestisce solo credenziali Firebase Auth (uid/email/nome da Google). Nome e ruolo restano responsabilità di `UserRepository`, orchestrati da `AuthViewModel` |
| AuthViewModel: scoping | Condiviso tra Login/Registrazione/CompletaProfiloGoogle tramite `activityViewModels()` (unica Activity nell'app, nessun problema pratico rispetto a `navGraphViewModels()`) |
| Palette colori app | **Aggiornata l'8 luglio (revisione grafica).** Blu-indaco `care_primary` `#FF4A5AD9` (primario/struttura: barre, bottoni principali) + arancione `care_accent` `#FFF26522` con pastello `care_accent_container` `#FFFCE3D4` (accento/azioni) + rosso `care_sos` `#FFD32F2F` (riservato ESCLUSIVAMENTE all'SOS, mai riusato). Palette **agganciata al tema** (`colorPrimary`/`colorSecondary`/… in `themes.xml`), così i componenti Material usano i colori dell'app e non il viola di default. Aggiunti `care_primary_container` (lavanda), `care_on_surface_variant` (testo attenuato), `care_outline` (bordi), e i colori di stato richieste (`stato_*_bg`/`stato_*_fg`: aperta=lavanda, presa_in_carico=arancione, completata=ambra, confermata=verde, annullata=grigio). **ATTENZIONE:** `care_primary_dark` `#FF111820` NON è una variante del primario, è il **colore del testo** principale usato in ~18 layout: non va cambiato. Fix storico: `care_primary` era `#C05249B4` (alpha 75%, barre slavate), reso opaco |
| Accessibilità UI | Touch target minimo 56dp (`touch_target_min`), testo body 18sp (`text_size_body`) — validi per tutti i ruoli |
| **Architettura navigazione** | **Single-Activity**: `MainActivity` ospita un `NavHostFragment` unico. Grafo principale (`nav_graph_main`) include come nested graph quello di autenticazione (`nav_graph_auth`) e, per Anziano e Volontario, un secondo grafo annidato dedicato al ruolo (`nav_graph_anziano`, `nav_graph_volontario`) |
| **Routing post-login** | `AuthUiState.Autenticato` porta sia `AuthUser` che `ruolo: UserRole`; una funzione condivisa (`Fragment.navigaAllaHomePerRuolo()`) mappa il ruolo alla destinazione corretta e pulisce lo stack di autenticazione (`popUpTo(nav_graph_auth, inclusive = true)`) |
| **Toolbar** | **Rivisto l'8 luglio (strada B — una Toolbar per ruolo).** Ogni Fragment contenitore di ruolo ha la **propria** `Toolbar` nel layout (`anzianoToolbar`/`volontarioToolbar`/`familiareToolbar`), collegata al **proprio** grafo annidato via `AppBarConfiguration(setOf(<startDestination del ruolo>))` + `setupWithNavController`. Effetto: freccia Indietro assente sulla home del ruolo, presente e funzionante su tab secondari e schermate di dettaglio. La `Toolbar` in `activity_main.xml` resta solo per le schermate di autenticazione e viene **nascosta** sulle 3 home di ruolo (listener sul NavController principale in `MainActivity`). Titolo dinamico da `android:label`. NB: label home Anziano cambiata da "Home Anziano" a "Home" |
| **Sessione/auto-login (Fase 4)** | `SplashFragment` come vero `startDestination` di `nav_graph_main`: legge `AuthRepository.utenteCorrente()` (sincrono) + `SessionCache` (ruolo salvato in `SharedPreferences`), con fallback su `UserRepository.getUtente()` solo se la cache è vuota ma la sessione Firebase è valida. Se il fallback Firestore fallisce (profilo mai completato), si torna al login per sicurezza |
| **Cache locale del ruolo** | `SessionCache` (in `util/`), wrapper su `SharedPreferences`. Iniettata in `AuthViewModel`: scritta ad ogni login/registrazione riuscita, pulita al logout. Motivo: evitare una query Firestore ad ogni avvio app solo per conoscere il ruolo, che altrimenti servirebbe anche offline |
| **BottomNavigation per ruolo** | **Grafo annidato per ruolo** (Opzione B, preferita a una bottom nav globale in `activity_main.xml`): ogni Fragment "contenitore" di ruolo (es. `HomeAnzianoFragment`, `HomeVolontarioFragment`) ospita un proprio `NavHostFragment` figlio + propria `BottomNavigationView`, con grafo dedicato. Isola completamente la sotto-navigazione di ogni ruolo |
| **Collegamento bottom nav ↔ NavController** | **Manuale** (`setOnItemSelectedListener` + `popUpTo(startDestinationId)` senza `saveState`), non `NavigationUI.setupWithNavController()`: il comportamento di default di quest'ultimo (`saveState`/`restoreState`) creava un back stack inconsistente in combinazione con la gestione custom del tasto Indietro. Compromesso accettato: si perde il "ricordo" dello stato di ogni tab (es. scroll position) quando si cambia tab |
| **Tasto Indietro nel grafo annidato** | **Rivisto l'8 luglio.** Callback su `OnBackPressedDispatcher` basato su `navController.popBackStack()`: se torna `true` è tornato indietro nello stack del ruolo (tab secondario/dettaglio → home); se torna `false` siamo sulla home del ruolo, quindi si disabilita il callback e si lascia agire il sistema (`onBackPressedDispatcher.onBackPressed()`) → l'app esce. Sostituisce il vecchio trucco fragile (`isEnabled=false; onBackPressed(); isEnabled=true`) che faceva funzionare l'Indietro una volta sola e in alcuni casi portava al login o alla schermata di un altro ruolo (§10 A1) |
| **RecyclerView con liste di richieste** | `RecyclerView.Adapter` classico + `notifyDataSetChanged()`, **non** `ListAdapter`/`DiffUtil`: scelta per coerenza con quanto visto a lezione, più semplice da spiegare all'orale. Compromesso accettato: meno efficiente su liste grandi, irrilevante con il volume di richieste in gioco |
| **Tipo richiesta (form "Nuova richiesta")** | Set chiuso di opzioni (RadioGroup: Spesa / Bolletta / Assistenza digitale / Altro). Selezionando "Altro" appare un campo di testo facoltativo |
| **Campo "data" nel form** | **Non implementato come campo separato**: si usa solo `timestampCreazione`, automatico |
| **Eliminazione richiesta** | Interpretata come **annullamento** (soft-delete), non cancellazione fisica: riusa `RequestRepository.aggiornaStato()`, stato → `ANNULLATA` |
| **Modifica richiesta** | Riusa `NuovaRichiestaFragment` in una "modalità modifica" (stesso Fragment, argomenti opzionali via `Bundle`). Permessa solo se `stato == APERTA`, controllo sia in UI che lato repository |
| **Logout in UI (Anziano)** | Bottone "Esci" nel **Profilo** Anziano (spostato dalla Dashboard in Fase 8), con conferma. Richiama `AuthViewModel.logout()` e naviga al grafo di autenticazione tramite il `NavController` **principale**. **Aggiornato l'8 luglio (A6):** usa `popUpTo(navController.graph.id) { inclusive = true }` (deterministico), non più `popUpTo(0)` che faceva uscire dall'app invece di tornare al login |
| **Home screen Volontario** | **Nessuna Dashboard separata** (a differenza dell'Anziano): apertura diretta su "Richieste disponibili" (`startDestination` di `nav_graph_volontario`). Ragionamento UX: il Volontario è un utente *task-oriented* (stesso schema delle app "a incarichi", es. Uber driver/Glovo), una schermata di solo benvenuto sarebbe frizione gratuita |
| **Logout in UI (Volontario)** | Spostato nel tab **Profilo** (non nella home, a differenza dell'Anziano): pattern standard per bottom nav a 3+ tab, le azioni sull'account vivono in un posto dedicato e prevedibile |
| **Query "Le mie richieste" del Volontario** | `RequestRepository.osservaRichiestePerVolontario()` filtra lato query solo gli stati "attivi" (`PRESA_IN_CARICO`/`COMPLETATA_DAL_VOLONTARIO`), non l'intero storico del volontario — combina `whereEqualTo` + `whereIn`, può richiedere un indice composito Firestore (creabile in un click dal link che compare in Logcat al primo utilizzo) |
| **Rilascio richiesta (Volontario)** | Azione "Rilascia" con **dialog di conferma** (come "Annulla" per l'Anziano): rimette la richiesta a disposizione di tutti i volontari, non deve essere un tap accidentale |
| **Bio profilo Volontario** | Campo opzionale `bio: String?` aggiunto al modello `User` (valorizzato solo dal Volontario per ora). Il salvataggio riusa `UserRepository.salvaUtente()` esistente — **attenzione**: è un `.set()` completo, non un update parziale, va sempre passato l'oggetto `User` intero (letto con `getUtente()`, poi copiato con `.copy(bio = ...)`) per non perdere gli altri campi |
| **Immagine profilo** | Valutate 3 opzioni (foto vera con Firebase Storage / avatar semplice predefinito / rimandare) — **rimandata di proposito** come possibile estensione per la tesi, vedi §9 |
| **Collegamento Anziano ↔ Familiare (Fase 6)** | Relazione **1:N**, non 1:1 come pianificato inizialmente: un anziano può avere più familiari/garanti collegati (`User.familiariCollegatiIds: List<String>`), ogni familiare segue un solo anziano (`User.anzianoCollegatoId: String?`, resta singolare). Cambio deciso esplicitamente durante la Fase 6, verificato che un'estensione futura (familiare che segue più anziani) non richiederebbe una riscrittura, solo una query `whereIn` invece di `whereEqualTo` |
| **Codice invito (Fase 6)** | Generato lato Anziano (6 caratteri, alfabeto senza caratteri ambigui tipo 0/O/1/I), non l'UID Firebase mostrato direttamente: più leggibile per un'utenza anziana. Riutilizzabile per più familiari nel tempo (non "a consumo singolo"). Controllo di unicità con retry (max 5 tentativi) in `UserRepository.ottieniOCreaCodiceInvito()`. Collegamento (`collegaFamiliareAdAnziano()`) via `WriteBatch` + `FieldValue.arrayUnion` — atomico e concorrenza-sicuro senza bisogno di una vera `Transaction` |
| **`HomeFamiliareFragment` — grafo annidato (Fase 6)** | A differenza di Anziano/Volontario, il grafo annidato (`nav_graph_familiare`) **non** è dichiarato staticamente in XML (`app:navGraph`): viene agganciato a runtime (`childFragmentManager.beginTransaction()`) solo quando lo stato del Familiare risulta `Collegato`. Motivo: esiste uno stato "non ancora collegato" che gli altri due ruoli non hanno, e le schermate annidate presuppongono un anziano già collegato |
| **Indirizzo Anziano (Fase 7)** | Campo `indirizzo: String?` su `User` (solo Anziano), **fisso sul profilo**, non per singola richiesta: un volontario che accetta deve sapere dove andare, e la maggior parte delle richieste riguarda lo stesso posto (casa dell'anziano). Impostato dal tab Profilo Anziano (Fase 8), riusa `salvaUtente()` esistente |
| **`volontarioNome` su `Request` (Fase 7)** | **Opzione B scelta** (tra le due valutate): il ViewModel recupera il nome con `UserRepository.getUtente()` PRIMA di chiamare `aggiornaStato()`, che resta "puro" (legge/scrive solo `requests`, mai `users`) — coerente con "un repository per collezione" fissato in Fase 1. Stesso schema per `autoreNome`/`autoreIndirizzo` (letti dal profilo dell'Anziano al momento della creazione della richiesta) |
| **Privacy nome/indirizzo Anziano verso il Volontario (Fase 7)** | Visibili **solo dopo** "Prendi in carico" (`RichiestePreseInCaricoAdapter`), mai nella lista "Richieste disponibili" (`RichiesteDisponibiliAdapter`, invariato): un anziano non deve essere identificabile da un volontario che sta solo guardando le richieste aperte, prima di essersi impegnato ad accettarne una |
| **Descrizione richiesta più guidata (Fase 7)** | Hint dinamico nel form "Nuova richiesta", diverso per tipo selezionato, con un esempio concreto di cosa scrivere. Alternativa scartata: una vera chat anziano↔volontario — valutata, considerata troppo costosa (nuova collezione, listener realtime, UI dedicata) per il tempo residuo, spostata nel backlog tesi (§9) |
| **Profilo pubblico del Volontario (Fase 7)** | Dialog di sola lettura (nome, bio, valutazione), condiviso tra Anziano e Familiare tramite un'unica funzione (`ui/common/ProfiloVolontarioDialog.kt`) invece di duplicarlo in due package: stesso contenuto identico in entrambi i ruoli |
| **Home Anziano ridisegnata (Fase 8)** | Da 3 tab (Dashboard/Nuova richiesta/Le mie richieste) a 3 tab (Home a 2 pulsanti/Le mie richieste/Profilo, nuovo). "Nuova richiesta" resta nel grafo ma non più come tab bottom nav: raggiunta con navigazione in avanti da un bottone, back stack gestito dalla stessa logica manuale già esistente (nessuna modifica necessaria a `gestisciTastoIndietro()`, già abbastanza generica) |
| **SOS: conferma prima dell'invio (Fase 8)** | Dialog di conferma ("Contattare i soccorsi?") prima di scrivere `SosAlert`/aprire il compositore: un falso allarme (tap accidentale) costa più di un secondo perso in un'emergenza vera. Stesso principio già applicato a logout/annulla/rilascia |
| **SOS multi-familiare (Fase 8)** | `SosAlert.familiareId` è singolare (Fase 1), incompatibile con la relazione 1:N introdotta in Fase 6. Fix: un documento `SosAlert` per ciascun familiare in `familiariCollegatiIds` (loop nel ViewModel), non un cambio di schema. Se anche un solo invio riesce, l'SOS è considerato "inviato" (meglio avvisarne alcuni che nessuno) |
| **Banner SOS lato Familiare (Fase 8)** | Mostra solo l'alert `ATTIVO` più recente (non una lista): più chiaro da leggere in un momento di emergenza. Azione "Ho visto, chiudi" → `SosRepository.aggiornaStato(CHIUSO)`, stato intermedio `VISTO` non usato per ora |
| **DataBinding dichiarativo (Fase 13)** | Portato da "DataBinding usato come ViewBinding" a **DataBinding dichiarativo** sui 3 profili (Anziano/Volontario/Familiare): `<data><variable name="viewModel">` + espressioni `@{}` in XML (testo e `android:enabled`), `binding.viewModel` + `binding.lifecycleOwner` nel Fragment. Schema della lezione 9 (`MutableLiveData` privato + `LiveData` pubblico). Scelta **LiveData** (non StateFlow) su queste schermate perché il DataBinding osserva nativamente LiveData tramite il `lifecycleOwner`; il resto dell'app resta ViewModel + StateFlow osservato nel Fragment (variante MVVM valida). I campi editabili (bio, indirizzo) restano gestiti a mano: niente two-way binding, per non entrare in conflitto col pre-riempimento una tantum |
| **Insets / edge-to-edge (Fase 13)** | `android:fitsSystemWindows="true"` sulla root dei 3 contenitori di ruolo (approccio a tempo di layout, robusto), NON gestione a runtime con listener (fragile: sulla Home, prima schermata, gli insets erano già distribuiti prima che la Toolbar esistesse → barra "corta"). Root sfondo navy per continuità delle barre; nel Familiare la root diventa navy solo da collegati (il form "Collegati" resta chiaro) |
| **Tastiera (IME) in edge-to-edge (Fase 13)** | In edge-to-edge la finestra non si ridimensiona da sola per la tastiera. Schermate con input in `NestedScrollView` + padding in basso = inset `ime()` (il campo attivo sale sopra la tastiera). La bottom nav che "saltava" sopra la tastiera è nascosta da un helper condiviso (`ui/common/nascondiBottomNavQuandoTastieraAperta`) che rileva la tastiera **misurando l'area visibile della finestra**, non gli insets (qui inaffidabili per via di `fitsSystemWindows` + ridimensionamento) |
| **Toolbar login + titolo crea/modifica (Fase 13)** | Login = destinazione di primo livello (`AppBarConfiguration(setOf(splashFragment, loginFragment))`) → niente freccia Indietro sull'accesso. Titolo Toolbar del login **vuoto** (`android:label=""`), titolo grande in pagina (scelta utente). Su Nuova richiesta il titolo in pagina è stato rimosso e la Toolbar mostra dinamicamente "Nuova richiesta"/"Modifica richiesta" a seconda della modalità |
| **Tema bloccato in chiaro (Fase 13)** | `CareConnectApp : Application` con `AppCompatDelegate.setDefaultNightMode(MODE_NIGHT_NO)`: l'app non usa mai le risorse "night" (il tema scuro non è negli obiettivi del progetto e `values-night` era vuoto → rischio UI illeggibile in dark mode). Registrata in Manifest con `android:name=".CareConnectApp"` |
| **Adattabilità schermo (Fase 13)** | Form di auth (login/registrati/completa profilo) in `NestedScrollView` per reggere landscape e schermi piccoli; testo in `sp`, misure in `@dimen`; guideline in **percentuale** (non dp fisso) per posizionare il form del login in proporzione all'altezza. Verifica rotazione + emulatore piccolo eseguita. Layout tablet dedicati (`sw600dp`) esplicitamente **fuori scope** (la griglia chiede adattabilità, non ottimizzazione tablet) |

## 3. Setup completato
- [x] Progetto Android Studio creato e funzionante
- [x] Progetto Firebase collegato (google-services.json presente, `applicationId` allineato a `com.careconnect.app`)
- [x] Gradle sync funzionante con dipendenze Firebase Auth + Firestore
- [x] Risolto conflitto plugin Kotlin (rimosso `kotlin-android` dal version catalog, non necessario con AGP 9.2.1)
- [x] ViewBinding + DataBinding attivati in `build.gradle.kts`
- [x] `compileOptions`/`kotlin { compilerOptions { jvmTarget } }` allineati su Java 11
- [x] Dipendenze coroutine aggiunte (`kotlinx-coroutines-core`, `kotlinx-coroutines-play-services`) tramite version catalog
- [x] Migrati `firebase-auth-ktx`/`firebase-firestore-ktx` (deprecati) → `firebase-auth`/`firebase-firestore`
- [x] Cloud Firestore creato (Standard edition, region europea, security rules in modalità test, scadenza 2 agosto 2026 — **da sostituire con vere regole in Fase 9**)
- [x] Google Sign-In abilitato in Firebase Authentication, SHA-1 debug registrato
- [x] Dipendenze `androidx.fragment:fragment-ktx`, `androidx.credentials:*`, `googleid` aggiunte
- [x] Dipendenze `androidx.navigation:navigation-fragment-ktx`, `navigation-ui-ktx` già presenti
- [x] Tema app (`Theme.CareConnect`) già `NoActionBar` di base — nessuna modifica necessaria per usare la Toolbar custom
- [x] `AndroidManifest.xml`: aggiunto `android:enableOnBackInvokedCallback="true"` al tag `<application>` (buona pratica per targetSdk 33+)

## 4. Stato di avanzamento per fase
*(Vedi `Roadmap.md` per il dettaglio completo dei singoli task)*

| Fase | Stato | Note |
|---|---|---|
| 0 — Setup progetto | ✅ Completata | |
| 1 — Fondamenta architetturali | ✅ Completata | Struttura pacchetti, enum, data class, 4 repository |
| 2 — Autenticazione | ✅ Completata | Email/password + Google Sign-In, testati end-to-end su emulatore |
| 3 — Navigazione e shell | ✅ Completata | NavGraph + Toolbar testati |
| 4 — Modulo Anziano | ✅ Completata | Auto-login, BottomNavigation (grafo annidato), CRUD completo richieste, logout — tutto testato end-to-end |
| 5 — Modulo Volontario | ✅ Completata | BottomNav 3 tab, CRUD stato richieste, Transaction anti-race-condition, Profilo con bio — tutto testato end-to-end |
| 6 — Modulo Familiare | ✅ Completata | Relazione 1:N, codice invito, BottomNav 2 tab (Attività/Profilo) — tutto testato end-to-end |
| 7 — Visibilità volontario (Anziano/Familiare) | ✅ Completata | Opzione B scelta, indirizzo Anziano, profilo pubblico Volontario, privacy pre/post accettazione — tutto testato end-to-end. Bug reale trovato e corretto: logout Volontario (vedi §7) |
| 8 — Riconfigurazione Home Anziano + SOS parziale | ✅ Completata | Home 2 pulsanti, tab Profilo Anziano, SOS con fix multi-familiare, banner realtime Familiare — tutto testato end-to-end |
| 9 — Sistema rating | ✅ Completata | `ratingMedio` calcolato con media da query (Opzione A): `UserRepository.aggiornaRatingMedio()` interroga tutti i `Rating` del volontario e aggiorna il campo con `update()` parziale, chiamato dal ViewModel dopo il successo del rating. UI a stelle (RatingBar standard) e scrittura Rating erano già pronte da fasi precedenti |
| 10 — Security Rules | ✅ Completata (validata a livello logico) | Regole vere basate su ruolo/uid pubblicate. Semplificate su richiesta esplicita per essere coerenti col livello del corso (tolti quasi tutti i `diff().hasOnly()`, ridotti gli helper a `isSignedIn`/`getRuolo`/`getAnzianoCollegato`). Testate nel Rules Playground: 8 casi su 11 passano direttamente; i 3 basati su `get()` cross-document falliscono per un limite noto del Playground nella sessione (NON un difetto delle regole), da validare col percorso felice sull'app. Preparata una scheda orale di giustificazione riga-per-riga. **Nota:** i bug del dispositivo fisico (§10) sono emersi proprio durante questi test |
| 11 — Background Task | ✅ Completata | Worker periodico (`WorkManager` + `CoroutineWorker`): controllo nuove richieste per il Volontario e conferme per il Familiare. Innesco demo via long-press sulla Toolbar (`eseguiOraPerDemo`). Verificato nel codice (17 rif. `WorkManager`) |
| 12 — Notifiche push (SOS locale già in Fase 8) | ✅ Completata | Firebase Cloud Messaging: `CareConnectMessagingService` (`FirebaseMessagingService` + `onMessageReceived`), canale notifiche, permesso `POST_NOTIFICATIONS`. Verificato nel codice |
| 13 — Testing/rifinitura | ✅ Completata (12 luglio) | **Layout consolidato a 3/3** e **DataBinding a 3/3** (vedi `HANDOFF_4`). Fatto: insets con `fitsSystemWindows` (barra corta Home risolta, A4 bottom bar), tastiera che copre l'input + bottom nav che salta (helper condiviso), titoli doppi rimossi, form di auth scrollabili (landscape/schermi piccoli), guideline % sul login, tema chiaro bloccato (`AppCompatDelegate.MODE_NIGHT_NO`), freccia tolta da "Accedi", titolo Toolbar dinamico crea/modifica, DataBinding dichiarativo (`@{}`) sui 3 profili. Verifica rotazione + schermo piccolo eseguita. **Decisioni estetiche:** card lasciate bianche e bottoni principali lasciati navy (varianti provate e scartate dall'utente, rimandate alla tesi) |
| 14 — Consegna | ⬜ Da iniziare | Ultimo giorno utile (17 luglio), zero margine residuo |

**Geolocalizzazione — RITIRATA** dal piano d'esame il 5 luglio (era pianificata come Fase 12 extra): spostata nel backlog tesi, §9.

## 5. Data model Firestore (aggiornato in Fase 7)

```
users/{uid}                          # id documento = uid Firebase Auth
  - nome: String
  - ruolo: String ("anziano" | "volontario" | "familiare")   → enum UserRole
  - familiariCollegatiIds: List<String> (per anziano — Fase 6, era familiareCollegatoId singolare)
  - codiceInvito: String? (per anziano — Fase 6, generato, univoco, riutilizzabile)
  - indirizzo: String? (per anziano — Fase 7, dove il volontario deve andare)
  - anzianoCollegatoId: String? (per familiare, resta singolare: un familiare segue un solo anziano)
  - ratingMedio: Double? (per volontario)
  - bio: String? (per volontario, facoltativo — aggiunto in Fase 5)

requests/{requestId}
  - autoreId: String (uid anziano)
  - autoreNome: String (Fase 7 — denormalizzato dal profilo dell'anziano alla creazione)
  - autoreIndirizzo: String (Fase 7 — denormalizzato, visibile al volontario solo dopo l'accettazione)
  - tipo: String (es. "spesa", "bolletta", "assistenza_digitale", "altro" — libero, non enum)
  - descrizione: String
  - stato: String ("aperta" | "presa_in_carico" | "completata_dal_volontario" | "confermata" | "annullata")   → enum RequestStatus
  - volontarioId: String?
  - volontarioNome: String? (Fase 7 — scritto da aggiornaStato() quando volontarioId viene impostato)
  - timestampCreazione: Timestamp
  - posizione: GeoPoint?

ratings/{ratingId}
  - requestId: String
  - volontarioId: String
  - stelle: Int (1-5)
  - commento: String?
  - valutatoreId: String (familiare o anziano)

sosAlerts/{alertId}
  - anzianoId: String
  - familiareId: String   [singolare, risale alla Fase 1 — un anziano può avere PIÙ familiari (Fase 6).
                            Fix applicato in Fase 8: un documento SosAlert per ciascun familiare collegato,
                            scritto in loop da DashboardAnzianoViewModel.inviaSos(). Non un cambio di schema.]
  - stato: String ("attivo" | "visto" | "chiuso")   → enum SosStatus
  - messaggio: String?
  - timestampCreazione: Timestamp

messaggi/{messaggioId}                # T3 — chat anziano ↔ volontario, legata a una richiesta
  - requestId: String                 (la richiesta a cui appartiene la conversazione)
  - anzianoId: String                 (denormalizzato: autore della richiesta)
  - volontarioId: String              (denormalizzato: volontario assegnato)
  - mittenteId: String                (uid di chi ha scritto: anzianoId oppure volontarioId)
  - testo: String
  - timestamp: Timestamp
```

**Workflow stati `Request` (enum `RequestStatus.canTransitionTo`):**
- `APERTA` → `PRESA_IN_CARICO` (volontario prende in carico) | `ANNULLATA` (solo l'anziano autore)
- `PRESA_IN_CARICO` → `COMPLETATA_DAL_VOLONTARIO` | `APERTA` (rilascio volontario, reset `volontarioId`) | `ANNULLATA` (solo l'anziano autore)
- `COMPLETATA_DAL_VOLONTARIO` → `CONFERMATA` (solo tramite creazione di un `Rating`, transazione atomica)
- `CONFERMATA` / `ANNULLATA` → stati terminali, nessuna transizione ulteriore
- L'annullamento è consentito solo all'anziano autore, mai al familiare
- La modifica del contenuto (`tipo`/`descrizione`, non lo stato) è permessa solo mentre `stato == APERTA`

## 6. File/moduli già scritti

**`model/`**
- `UserRole.kt`, `RequestStatus.kt` (con `canTransitionTo`), `SosStatus.kt` — enum
- `User.kt` — esteso in Fase 5 (`bio`), **esteso in Fase 6** (`familiariCollegatiIds: List<String>`, `codiceInvito: String?`, relazione 1:N), **esteso in Fase 7** (`indirizzo: String?`)
- `Request.kt` — **esteso in Fase 7**: `autoreNome`, `autoreIndirizzo`, `volontarioNome`
- `Rating.kt`, `SosAlert.kt` — invariati (`SosAlert.familiareId` singolare, da correggere in Fase 8, vedi §5)
- `AuthUser.kt` — wrapper di dominio per l'identità autenticata (uid, email, nome), disaccoppiato da `FirebaseUser`

**`repository/`**
- `RequestRepository.kt` / `RequestRepositoryImpl.kt` — esteso in Fase 4 (`modificaRichiesta()`), Fase 5 (`osservaRichiestePerVolontario()`, `aggiornaStato()` convertito in Transaction), **esteso in Fase 7**: `aggiornaStato()` accetta anche `nuovoVolontarioNome` (Opzione B), mapping estende `autoreNome`/`autoreIndirizzo`/`volontarioNome`
- `UserRepository.kt` / `UserRepositoryImpl.kt` — esteso in Fase 5 (`bio`), **esteso in Fase 6**: `ottieniOCreaCodiceInvito()`, `trovaAnzianoPerCodiceInvito()`, `collegaFamiliareAdAnziano()` (`WriteBatch` + `arrayUnion`), **esteso in Fase 7**: mapping estende `indirizzo`
- `RatingRepository.kt` / `RatingRepositoryImpl.kt` (`creaRatingEConfermaRichiesta` via Transaction) — invariato, riusato in Fase 6 dal Familiare
- `SosRepository.kt` / `SosRepositoryImpl.kt` (incluso `Flow` realtime per il familiare) — invariato, verrà usato in Fase 8 (limite noto: `familiareId` singolare, vedi §5)
- `AuthRepository.kt` / `AuthRepositoryImpl.kt` (email/password, Google Sign-In via ID token, `osservaStatoAutenticazione()` realtime, `utenteCorrente()` sincrono, `logout()`)

**`util/`**
- `SessionCache.kt` — wrapper su `SharedPreferences` per la cache locale del ruolo utente

**`viewmodel/auth/`**
- `AuthViewModel.kt` — `AuthUiState` sealed class + `AuthViewModelFactory`
- `SplashViewModel.kt` — `SplashUiState` + `SplashViewModelFactory`

**`viewmodel/anziano/`**
- `NuovaRichiestaViewModel.kt` — creazione e modifica richiesta; **esteso in Fase 7**: legge il profilo per denormalizzare `autoreNome`/`autoreIndirizzo`, blocca la creazione se `indirizzo` non è ancora impostato
- `MieRichiesteViewModel.kt` — `StateFlow<List<Request>>` + `annullaRichiesta(requestId)`
- `DashboardAnzianoViewModel.kt` — **nuovo in Fase 6**, esteso in Fase 7: codice invito + indirizzo modificabile (verrà smontato in Fase 8, contenuto spostato in un nuovo `ProfiloAnzianoViewModel`)

**`viewmodel/volontario/`** *(nuovo in Fase 5)*
- `RichiesteDisponibiliViewModel.kt` — `StateFlow<List<Request>>` (da `osservaRichiesteAperte`) + `prendiInCarico(requestId)`; **esteso in Fase 7**: `prendiInCarico()` recupera il proprio nome da `UserRepository` prima di chiamare `aggiornaStato()` (Opzione B)
- `RichiestePreseInCaricoViewModel.kt` — `StateFlow<List<Request>>` (da `osservaRichiestePerVolontario`) + `segnaCompletata(requestId)` + `rilasciaRichiesta(requestId)`
- `ProfiloVolontarioViewModel.kt` — carica il profilo una tantum (non realtime), espone `email` (da `AuthUser`), `utente` (da Firestore), gestisce `salvaBio()`. **Bug corretto in Fase 7**: aveva un proprio `logout()` che disconnetteva Firebase ma non resettava lo stato condiviso di `AuthViewModel` — rimosso, il logout ora passa da `AuthViewModel` condiviso (vedi §7)

**`viewmodel/familiare/`** *(nuovo in Fase 6)*
- `HomeFamiliareViewModel.kt` — stato collegamento (`StatoHomeFamiliare` sealed class: `Caricamento`/`NonCollegato`/`Collegato`) + `collegati(codice)`
- `AttivitaFamiliareViewModel.kt` — `StateFlow<List<Request>>` (da `osservaRichiestePerAnziano`) + `confermaEValuta(richiesta, stelle, commento)`
- `ProfiloFamiliareViewModel.kt` — carica una tantum nome proprio + nome dell'anziano collegato

**`ui/auth/`**
- `LoginFragment.kt`, `RegistrazioneFragment.kt`, `CompletaProfiloGoogleFragment.kt`, `NavigazionePerRuolo.kt`, `SplashFragment.kt` — invariati dalla Fase 4

**`ui/anziano/`**
- `HomeAnzianoFragment.kt` + `fragment_home_anziano.xml` — contenitore (NavHost annidato + BottomNavigationView), invariato dalla Fase 4
- `DashboardAnzianoFragment.kt` + `fragment_dashboard_anziano.xml` — **ridisegnato in Fase 8**: solo 2 pulsanti ("Crea una nuova richiesta" + "SOS"), codice invito/indirizzo/logout spostati in `ProfiloAnzianoFragment`
- `ProfiloAnzianoFragment.kt` + `fragment_profilo_anziano.xml` — **nuovo in Fase 8**: nome/email/ruolo/indirizzo modificabile/codice invito/logout, stesso schema di Profilo Volontario/Familiare
- `NuovaRichiestaFragment.kt` + `fragment_nuova_richiesta.xml` — form creazione/modifica; esteso in Fase 7 (hint dinamico, blocco se indirizzo mancante); **dalla Fase 8 raggiunto con un bottone dalla Home, non più come tab bottom nav**
- `MieRichiesteFragment.kt` + `fragment_mie_richieste.xml` — lista realtime, azioni Modifica/Annulla
- `RichiesteAdapter.kt` + `item_richiesta.xml` — mostra "Volontario: [nome]" cliccabile (Fase 7)

**`ui/volontario/`** *(contenuto reale da Fase 5)*
- `HomeVolontarioFragment.kt` + `fragment_home_volontario.xml` — contenitore (NavHost annidato + BottomNavigationView), stesso schema di `HomeAnzianoFragment`
- `RichiesteDisponibiliFragment.kt` + `fragment_richieste_disponibili.xml` — lista realtime richieste APERTA
- `RichiesteDisponibiliAdapter.kt` + `item_richiesta_disponibile.xml` — singola azione "Prendi in carico"
- `RichiestePreseInCaricoFragment.kt` + `fragment_richieste_prese_in_carico.xml` — lista realtime richieste attive del volontario
- `RichiestePreseInCaricoAdapter.kt` + `item_richiesta_incarico.xml` — azioni "Segna come completata" / "Rilascia" (con conferma), visibili solo se `PRESA_IN_CARICO`; **esteso in Fase 7**: mostra nome/indirizzo dell'anziano (SOLO qui, non in `RichiesteDisponibiliAdapter` — privacy, vedi §2)
- `ProfiloVolontarioFragment.kt` + `fragment_profilo_volontario.xml` — nome/email/ruolo/valutazione (placeholder)/bio modificabile/logout. **Bug corretto in Fase 7**: il logout ora usa `AuthViewModel` condiviso, non più un `logout()` locale — **verrà esteso in futuro** (non pianificato nel dettaglio) con immagine profilo, vedi §9

**`ui/familiare/`** *(contenuto reale da Fase 6, non più placeholder)*
- `HomeFamiliareFragment.kt` + `fragment_home_familiare.xml` — doppio stato: form collegamento (codice invito) se `NonCollegato`, altrimenti BottomNav + NavHost annidato agganciato a runtime (vedi §2)
- `AttivitaFamiliareFragment.kt` + `fragment_attivita_familiare.xml` — lista realtime richieste dell'anziano collegato (stato + storico insieme)
- `AttivitaFamiliareAdapter.kt` + `item_richiesta_familiare.xml` — sola lettura + azione "Conferma e valuta" (dialog con `RatingBar`), visibile solo se `COMPLETATA_DAL_VOLONTARIO`; **esteso in Fase 7**: "Volontario: [nome]" cliccabile
- `ProfiloFamiliareFragment.kt` + `fragment_profilo_familiare.xml` — nome proprio, nome anziano seguito, logout
- `dialog_conferma_valutazione.xml` — layout del dialog di valutazione (`RatingBar` + commento facoltativo)

**Nota Fase 8**: `AttivitaFamiliareFragment.kt`/`AttivitaFamiliareViewModel.kt` estesi con il banner SOS (alert `ATTIVO` più recente + azione "Ho visto, chiudi"), `AttivitaFamiliareViewModel` ora inietta anche `SosRepository`. File sorgente salvato con un refuso nel nome (`AttivitaFamililareFragment.kt`, classe interna comunque corretta `AttivitaFamiliareFragment`) — cosmetico, non impatta la compilazione, da correggere quando comodo.

**`ui/common/`** *(nuovo in Fase 7 — primo package condiviso tra ruoli)*
- `ProfiloVolontarioDialog.kt` — funzione di estensione `Fragment.mostraProfiloVolontario(volontarioId)`, dialog di sola lettura (nome/bio/valutazione), usata sia da Anziano che da Familiare
- `dialog_profilo_volontario.xml` — layout del dialog
- `StatoRichiestaColori.kt` — colore pillola di stato, usato dai 3 Adapter
- **`KeyboardVisibility.kt`** *(nuovo Fase 13)* — `nascondiBottomNavQuandoTastieraAperta(root, bottomNav, lifecycleOwner)`: nasconde la bottom nav quando la tastiera è aperta, rilevandola dall'area visibile della finestra. Usata dai 3 contenitori di ruolo

**`res/navigation/`**
- `nav_graph_auth.xml` — Login → Registrazione / CompletaProfiloGoogle
- `nav_graph_main.xml` — `startDestination` = `splashFragment`; include `nav_graph_auth` + `homeAnzianoFragment` + `homeVolontarioFragment` + `homeFamiliareFragment` (tutti contenitori reali)
- `nav_graph_anziano.xml` — `dashboardAnzianoFragment` (start) + `nuovaRichiestaFragment` + `mieRichiesteFragment` + `profiloAnzianoFragment` (nuovo in Fase 8)
- `nav_graph_volontario.xml` *(nuovo in Fase 5)* — `richiesteDisponibiliFragment` (start) + `richiestePreseInCaricoFragment` + `profiloVolontarioFragment`
- `nav_graph_familiare.xml` *(nuovo in Fase 6)* — `attivitaFamiliareFragment` (start) + `profiloFamiliareFragment`, agganciato a runtime (non staticamente in XML)

**`res/menu/`**
- `bottom_nav_anziano.xml` — 3 voci: Home / Le mie richieste / Profilo (**ristrutturato in Fase 8**, era Dashboard/Nuova richiesta/Le mie richieste)
- `bottom_nav_volontario.xml` *(nuovo in Fase 5)* — 3 voci: Disponibili / Le mie richieste / Profilo
- `bottom_nav_familiare.xml` *(nuovo in Fase 6)* — 2 voci: Attività / Profilo

**`res/layout/`**
- `activity_main.xml` — `Toolbar` + `NavHostFragment` (grafo: `nav_graph_main`)

**`res/values/`**
- `colors.xml`, `dimens.xml` — palette e dimensioni accessibili
- `strings.xml` — stringhe di Login/Registrazione/CompletaProfilo

**`MainActivity.kt`**
- Host di Navigation Component. **Fase 13:** `AppBarConfiguration(setOf(splashFragment, loginFragment))` (login di primo livello, niente freccia)

**`CareConnectApp.kt`** *(nuovo Fase 13)*
- `Application` che blocca il tema in chiaro (`AppCompatDelegate.setDefaultNightMode(MODE_NIGHT_NO)`). Registrata in `AndroidManifest.xml` (`android:name=".CareConnectApp"`)

## 7. Problemi noti / da tenere a mente

- AGP 9.2.1 ha il supporto Kotlin integrato: NON applicare mai `org.jetbrains.kotlin.android` esplicitamente, causa crash di sync.
- Ricordarsi di limitare lo scope al solo esame: niente integrazione servizi sanitari professionali.
- **SOS**: usare `Intent.ACTION_DIAL`, NON `ACTION_CALL`.
- **Completamento richiesta**: flusso `presa_in_carico → completata_dal_volontario → confermata`.
- **Race condition**: quella di `RequestRepository.aggiornaStato()` è stata **risolta in Fase 5** con una Transaction. Resta invece non transazionale `SosRepository.aggiornaStato()` — da rivalutare in Fase 13 (rischio accettato per ora, impatto minore: meno probabile un doppio intervento simultaneo su un SOS rispetto a "prendi in carico" su una richiesta).
- **`ratingMedio`**: ✅ RISOLTO in Fase 9 — media da query (`UserRepository.aggiornaRatingMedio()`), chiamata dal ViewModel dopo il rating.
- **Firebase `-ktx` deprecati**: usare sempre `firebase-auth`/`firebase-firestore` senza suffisso.
- **Security Rules**: ✅ RISOLTO in Fase 10 — regole vere basate su ruolo/uid pubblicate (versione semplificata, vedi §4). Resta da completare il percorso felice sull'app per la validazione sul campo (checklist nel documento di handoff).
- **Errore Credential Manager gestito fuori dal ViewModel**: in `LoginFragment.avviaLoginGoogle()`, incoerenza voluta e accettata, citabile come limite noto all'orale.
- **Bug del tasto Indietro — ✅ RISOLTO l'8 luglio (vedi §10 A1)**: la radice era l'interazione tra la gestione manuale fragile dell'Indietro (`isEnabled=false; onBackPressed()...`), i NavController annidati e il logout con `popUpTo(0)`. Risolto: (1) callback Indietro basato su `popBackStack()`; (2) una Toolbar per ruolo (strada B) collegata al proprio grafo con `AppBarConfiguration`; (3) logout con `popUpTo(graph.id)`. Comportamento ora identico e prevedibile su tutti i ruoli. **Nota emulatore ancora valida:** il tasto Indietro *disegnato* dell'emulatore in uso non genera un vero evento; testare su dispositivo fisico o con navigazione a gesti.
- **`android:enableOnBackInvokedCallback="true"`**: buona pratica per targetSdk 33+, mantenuta anche se non ha risolto il bug sopra (che era della UI dell'emulatore, non del codice).
- **Nota tecnica — DataBinding**: ogni volta che si crea/sostituisce un layout XML con `<layout>` come tag radice, serve rigenerare la classe `Binding` corrispondente prima che l'IDE la veda. Se compare "Unresolved reference" su una classe `FragmentXxxBinding`/`ItemXxxBinding` subito dopo aver incollato un file: **non è quasi mai un errore di codice**. Prima `Sync Project with Gradle Files` → poi `Build → Rebuild Project` (non "Make") → se persiste, `File → Invalidate Caches... → Invalidate and Restart`. Il pannello "Problems" di Android Studio riflette l'indicizzazione IDE e può restare non aggiornato anche dopo un Rebuild riuscito: per verificare se c'è un errore di compilazione **reale**, guardare invece il pannello **Build**, righe che iniziano con `e:`.
- **Bottone di logout**: presente in Dashboard Anziano, Profilo Volontario e Profilo Familiare (Fase 6).
- **Bug reale trovato e corretto in Fase 7 — logout Volontario**: `ProfiloVolontarioViewModel` aveva un proprio `logout()` che chiamava solo `authRepository.logout()`, senza pulire `SessionCache` né resettare `AuthUiState` sul condiviso `AuthViewModel`. Effetto: `LoginFragment`, osservando lo stesso `AuthViewModel` (StateFlow, riemette sempre l'ultimo valore), trovava ancora `Autenticato(...)` e rimandava immediatamente alla Home Volontario — il logout sembrava "non funzionare". Fix: rimosso il `logout()` locale, `ProfiloVolontarioFragment` ora inietta e usa il condiviso `AuthViewModel.logout()`, stesso schema già corretto di Anziano/Familiare. **Lezione per l'orale**: un secondo "percorso" di logout indipendente da quello condiviso è esattamente il tipo di duplicazione che genera bug di stato silenziosi — buon esempio di comunicazione di un errore reale trovato durante il testing, non solo di codice che ha funzionato al primo colpo.
- **`popUpTo(0)` per il logout — ✅ CORRETTO l'8 luglio (A6)**: `popUpTo(0)` è un idiom non documentato il cui effetto dipende dallo stato dello stack; causava l'uscita dall'app dal logout Volontario (e non dall'Anziano, con codice identico — nondeterminismo). Sostituito in tutti e tre i Fragment di logout con `popUpTo(navController.graph.id) { inclusive = true }`, deterministico: svuota lo stack fino alla radice inclusa e riparte dal login.
- **Restyling grafico (8 luglio) — file nuovi da conoscere**: `res/color/bottom_nav_item_tint.xml` (tint bottom bar), `res/values/styles.xml` (`CareConnect.BottomNav.ActiveIndicator`, `CareConnect.Card`, `CareConnect.StatusChip`, `CareConnect.Button.Accent`), `res/drawable/bg_status_chip.xml`, `bg_avatar_circle.xml`, `ic_person.xml`, `bg_input_outline.xml`, e `ui/common/StatoRichiestaColori.kt` (colore pillola per stato, usato dai 3 Adapter). I 3 layout profilo, i 4 `item_richiesta*`, `fragment_dashboard_anziano`, i 3 `fragment_home_*`, `activity_main`, `themes.xml`, `colors.xml`, `nav_graph_anziano` sono stati modificati.

## 8. Prossimo step consigliato

**Fasi 9 e 10 completate.** Il prossimo lavoro va fatto in una **nuova chat**: la chat attuale ha accumulato moltissimo contesto di debug delle Security Rules (in particolare l'anomalia del `get()` cross-document nel Rules Playground) non più rilevante.

**Priorità immediata (PRIMA della rifinitura estetica e delle Fasi 11-12):** sistemare i **bug del dispositivo fisico** raccolti in §10, emersi testando l'app sul Samsung SM-A546B (Android 13). Il più grave è il cluster di **navigazione/tasto Indietro** (A1 in §10): la freccia Indietro funziona una volta sola, e in alcuni casi porta al login o addirittura alla schermata di un altro ruolo. Esiste un documento di handoff dedicato (`HANDOFF_Bug_Dispositivo_Fisico.md`) pronto da incollare in una chat nuova.

Dopo i bug: completare il **percorso felice sull'app** per validare definitivamente le Security Rules (checklist nel documento di handoff, sezione B), poi procedere con Fase 11 (Background Task) e Fase 12 (Notifiche push FCM).

## 9. Backlog — idee rimandate (non fanno parte del piano d'esame attuale)

Raccolte qui perché emerse durante lo sviluppo ma esplicitamente rimandate. Vedere anche `Visione_e_Requisiti.md` §6 per le idee di estensione tesi già tracciate in precedenza (canale servizi sanitari, interfaccia HCI con il prof. Camurri, ecc.) — le voci sotto sono nuove, emerse dopo la Fase 5, e **non sono ancora presenti in quel file**: se si vuole un'unica fonte per il backlog tesi, andrebbero eventualmente copiate anche lì.

- **Immagine profilo (Volontario, ed eventualmente altri ruoli)**: valutate 3 opzioni in Fase 5 (foto vera con Firebase Storage / avatar semplice predefinito / rimandare). Scelta: rimandare, possibile estensione per la tesi. Se ripresa in futuro, ricordare che comporta una nuova dipendenza (Firebase Storage), permessi Android per galleria/fotocamera, e security rules dedicate allo Storage (non solo a Firestore).
- **Geolocalizzazione (Extra, 1pt)** — **RITIRATA dal piano d'esame il 5 luglio**, per fare spazio alla Fase 8 senza sforare la scadenza del 17 luglio (vedi Roadmap, nota di aggiornamento e Fase 12). Era: permessi posizione, associazione posizione a richiesta, filtro/ordinamento per vicinanza nella vista Volontario. `Request.posizione: GeoPoint?` esiste già nel modello (Fase 1, mai popolato) — se ripresa per la tesi, il campo dati è già pronto, manca solo la UI/permessi.
- **Vista "chi sono i miei garanti/familiari collegati" per l'Anziano**: l'anziano vedrebbe l'elenco dei familiari che hanno accesso alle sue attività (oggi l'Anziano non ha visibilità su chi si è collegato con il suo codice invito). Non pianificata in una fase specifica, valutabile come piccola aggiunta al nuovo Profilo Anziano (Fase 8) o come estensione successiva.
- **Chat anziano↔volontario per i dettagli della richiesta**: valutata in Fase 7, scartata per il tempo residuo a favore di una descrizione guidata (hint dinamico per tipo). Se ripresa per la tesi: nuova collezione Firestore (messaggi per richiesta), listener realtime su entrambi i lati, UI dedicata, security rules apposite — non un piccolo ritocco.
- **Revisione grafica dell'app** (richiesta esplicita, 5 luglio): ✅ **avviata e in gran parte completata l'8 luglio** (vedi §4 Fase 13 e `HANDOFF_2`). Restano rifiniture puntuali nel backlog dell'handoff.
- **Colore delle card** (8 luglio → chiuso 12 luglio): provate varianti azzurro e pesca leggerissimo, **scartate** dall'utente. Decisione: card lasciate **bianche**. Eventuale ripresa alla tesi. Meccanismo pronto se ripresa: stile `CareConnect.Card.Request` che eredita da `CareConnect.Card` e cambia solo `cardBackgroundColor`, così si colora solo un sottoinsieme di card senza toccare le altre.
- **Diffusione arancione "a tema" (Step F)** (8 luglio → chiuso 12 luglio): provata la strada "azione = arancione" (bottoni principali arancioni con testo navy, via stile `CareConnect.Button.Primary`), **scartata** dall'utente perché non convinceva. Bottoni principali lasciati navy. Rimandato alla tesi.

## 10. Bug del dispositivo fisico — STATO AGGIORNATO (8 luglio)

> Emersi il 7 luglio testando l'app sul **Samsung Galaxy A54 (SM-A546B), Android 13, API 33**.
> **Sessione dell'8 luglio:** ✅ risolti **A1** (navigazione/Indietro), **A2** (contrasto) e **A6** (logout).
> **Sessione del 12 luglio:** ✅ **CHIUSI TUTTI i restanti** — A3 (tastiera), A4 (bottom bar), barra corta su
> Home, freccia Accedi, titoli doppi, colore card (deciso: bianche). A4 e la barra corta avevano la stessa
> radice (edge-to-edge/insets), chiusi insieme con `fitsSystemWindows`. Dettaglio in
> `HANDOFF_4_Fase13_Rifinitura_Completata.md`. **Non restano bug §10 aperti.**

### ✅ A1 — Navigazione / tasto Indietro — RISOLTO (8 luglio)
Radice: interazione tra gestione manuale fragile dell'Indietro, NavController annidati e logout
`popUpTo(0)`. Fix: callback Indietro basato su `popBackStack()`, una Toolbar per ruolo (strada B),
logout con `popUpTo(graph.id)`. Comportamento ora identico e prevedibile su tutti i ruoli.

### ✅ A2 — Contrasto testo — RISOLTO (8 luglio, blindato il 12 luglio)
Causato dal tema che seguiva il dark mode di sistema con colori per sfondo chiaro. Risolto
forzando il tema chiaro. **Blindato il 12 luglio:** aggiunta la classe `CareConnectApp : Application` con
`AppCompatDelegate.setDefaultNightMode(MODE_NIGHT_NO)` — l'app non usa MAI le risorse "night", a prescindere
dall'impostazione del telefono. Il file `values-night/themes.xml` resta inutilizzato (innocuo).

### ✅ A6 — Logout Volontario usciva dall'app — RISOLTO (8 luglio, nuovo)
`popUpTo(0)` non deterministico → sostituito con `popUpTo(graph.id) { inclusive = true }` sui 3 profili.

### ✅ Backlog Fase 13 — TUTTO RISOLTO (12 luglio, vedi HANDOFF_4)
- **Barra in alto più corta su Home** (insets/edge-to-edge) — ✅ RISOLTO. NON togliendo `enableEdgeToEdge()`
  (strada scartata), ma con `android:fitsSystemWindows="true"` sulla root dei 3 contenitori di ruolo: il
  framework riserva lo spazio delle barre di sistema a tempo di layout, quindi la barra ha sempre l'altezza
  giusta anche sulla Home (prima schermata). Root sfondo navy per continuità della barra; nel Familiare la
  root diventa navy solo da collegati (il form resta chiaro).
- **A4** bottom bar sotto la barra di sistema — ✅ RISOLTO (stesso `fitsSystemWindows`).
- **A3** tastiera copre l'input — ✅ RISOLTO. Causa reale: in edge-to-edge la finestra non si ridimensiona
  da sola per l'IME. Fix: form/schermate con input in `NestedScrollView` + padding in basso pari all'inset
  `ime()`, così il campo attivo sale sopra la tastiera (Nuova richiesta, Profilo Anziano indirizzo, Profilo
  Volontario bio). In più: la bottom nav "saltava" sopra la tastiera → helper condiviso
  `nascondiBottomNavQuandoTastieraAperta` (rileva la tastiera misurando l'area visibile, NON gli insets che
  qui sono inaffidabili).
- **Colore delle card** — ✅ CHIUSO come decisione: card lasciate **bianche** (varianti azzurro/pesca provate
  e scartate dall'utente; rimandate alla tesi).
- **Freccia su "Accedi"** — ✅ RISOLTO: `AppBarConfiguration(setOf(splashFragment, loginFragment))` → login
  di primo livello, niente freccia; Registrati/Completa profilo la mantengono.
- **Titoli doppi** (Toolbar + titolo bianco in pagina) — ✅ RISOLTO su liste e Nuova richiesta.
- **Step F arancione** — provato (bottoni principali arancioni con testo navy) e **scartato** dall'utente:
  lasciati navy, rimandato alla tesi.

---

### (Storico) Descrizione originale dei bug del 7 luglio

### A1. 🔴 Navigazione / tasto Indietro (il più grave — cluster con probabile radice unica)
Sintomi osservati sul dispositivo fisico:
- La freccia/tasto Indietro **funziona una sola volta, poi smette**.
- Da account **Volontario**, Indietro porta **alla schermata di un Anziano** (raggiunge una
  destinazione che non dovrebbe essere accessibile; i nomi restano invariati).
- Da account **Anziano**, Indietro **fa uscire dall'account** (torna al login) senza uscire dall'app.
- Il **logout dal Volontario esce dall'intera app** invece di tornare al login.

**Comportamento desiderato: DA CHIARIRE con l'utente prima di scrivere codice** — nel messaggio
originale i requisiti sembrano in tensione ("nella home la freccia non si deve vedere" vs "la
freccia deve rimanere lì"). Obiettivo dichiarato: un metodo di gestione della freccia Indietro
**identico e prevedibile in tutte le interfacce**, coerente con i tab della bottom bar.
**Punto di partenza tecnico:** la radice è quasi certamente nell'interazione tra
`OnBackPressedDispatcher.addCallback` (gestione manuale), i NavController annidati per ruolo e
il logout con `popUpTo(0)`. È il primo posto da investigare.

### A2. 🟠 Contrasto testo — testo troppo chiaro/illeggibile
- Campi email/password (Login/Registrazione): testo digitato troppo chiaro.
- Scritte nella pagina "Nuova richiesta": troppo chiare.
- In generale contrasto insufficiente su ciò che va letto/scritto.
Causa probabile: `textColor`/`textColorHint` troppo tenui, o conflitto tema chiaro/scuro (l'app
potrebbe seguire il dark mode di sistema con colori pensati solo per sfondo chiaro). Verificare
`colors.xml`, tema `Theme.CareConnect`, attributi dei campi input.

### A3. 🟠 Tastiera copre il campo di input (schermate Anziano)
Aprendo la tastiera, la bottom bar dell'Anziano si alza a livello della tastiera e copre il
campo attivo; la schermata non fa scroll per tenerlo visibile. Verificare
`android:windowSoftInputMode` (probabile `adjustResize`) e che i layout con input siano dentro
uno `ScrollView`/`NestedScrollView`; attenzione all'interazione con la bottom bar annidata.

### A4. 🟡 Bottom bar — forma/estensione (profilo Volontario)
La bottom bar appare "quadrata" e non copre tutta la parte bassa sul dispositivo (profilo
Volontario). Probabile gestione mancante degli insets di sistema / edge-to-edge su Android 13.

### A5. 🟡 Colori da rifinire (generale)
Rifinitura palette — già tracciata come "revisione grafica" (richiesta esplicita, vedi §9 e
Roadmap Fase 13). Da coordinare con A2 (contrasto), che è più urgente perché è leggibilità.

### Nota — Test Security Rules ancora da completare
Le Security Rules (Fase 10) sono pubblicate e validate a livello logico, ma il **percorso
felice sull'app** (checklist B nel documento di handoff) va ancora eseguito end-to-end sul
dispositivo, ideale da fare subito dopo aver sistemato A1 (senza una navigazione funzionante è
scomodo testare i flussi completi).

# HANDOFF 4 — Fase 13 (Rifinitura Layout + DataBinding) COMPLETATA

**Data sessione:** 12 luglio 2026
**Scadenza consegna:** 17 luglio 2026
**Stato:** Fase 13 chiusa. Consolidate le due voci di punteggio a rischio (Layout, DataBinding). Tutte e 9 le voci della griglia verificate nel codice. Nessun bug §10 aperto.

> Questo file registra il lavoro della sessione e le motivazioni tecniche delle scelte (utili anche all'orale). Il documento di preparazione all'orale vero e proprio è separato e verrà preparato come passo successivo.

---

## A. Verifica requisiti d'esame (9 voci, max 29) — controllata nel codice

| # | Voce (punti) | Stato | Riscontro nel codice |
|---|---|---|---|
| 1 | Autenticazione e registrazione (5) | ✅ | Email/password (`signInWithEmailAndPassword`/`createUserWithEmail`) + Google via `CredentialManager`/`GoogleIdToken` + completamento profilo |
| 2 | Integrazione Firebase (5) | ✅ | Firestore (mapping manuale, Transaction, `WriteBatch`), Auth, FCM, Security Rules basate su ruolo/uid |
| 3 | Architettura UI Activity/Fragment (4) | ✅ | Single-Activity + Fragment; NavHost annidato per ruolo |
| 4 | Complessità applicazione (4) | ✅ | 3 ruoli, flusso richieste completo, SOS, inviti, rating |
| 5 | **Layout (3)** | ✅ **consolidato** | Vedi §B.1 |
| 6 | **DataBinding + ViewModel MVVM (3)** | ✅ **consolidato** | Vedi §B.2 — 3 `<data>`, 3 `<variable>`, 9 espressioni `@{}`, `binding.lifecycleOwner` |
| 7 | Navigation Component + Toolbar (2) | ✅ | 4 nav graph, `setupWithNavController`, Toolbar per ruolo |
| 8 | Background Task / Service (2) | ✅ | `WorkManager` + `CoroutineWorker` (Fase 11) + `FirebaseMessagingService` (Fase 12) |
| 9 | Funzionalità extra (1) | ✅ | (come da Project State) |

**Nota onesta:** le voci 5 e 6 sono state costruite/verificate riga per riga in questa sessione. Le altre 7 sono confermate presenti nel codice ma non ri-testate end-to-end qui: lo stato "completa" si appoggia al Project State e alla presenza nel codice.

**Le vere variabili rimaste non sono le feature (tutte presenti), ma il malus in demo (fino a −3 per crash/glitch) e l'orale.**

---

## B. Cosa è stato fatto in questa sessione

### B.1 — Layout (da 2/3 verso 3/3) — criterio griglia: *organizzazione, coerenza grafica, usabilità, adattabilità a diverse dimensioni di schermo*

- **Insets / barra "corta" su Home** — Causa: gestione insets a runtime con listener, che sulla prima schermata (Home) non scattava (insets già distribuiti prima che la Toolbar esistesse). Fix: `android:fitsSystemWindows="true"` sulla root dei 3 contenitori di ruolo → lo spazio delle barre di sistema è riservato **a tempo di layout** dal framework, sempre corretto. Root sfondo navy per continuità delle barre. Nel Familiare la root diventa navy solo **da collegati** (il form "Collegati" resta chiaro, root chiara di default → colorata a runtime in `agganciaNavGraphAnnidato`).
- **Tastiera che copre l'input** — Causa: in edge-to-edge la finestra non si ridimensiona da sola per l'IME. Fix: schermate con input in `NestedScrollView` + `clipToPadding="false"` + padding in basso pari all'inset `ime()` → il campo attivo sale sopra la tastiera. Applicato a Nuova richiesta, Profilo Anziano (indirizzo), Profilo Volontario (bio).
- **Bottom nav che "salta" sopra la tastiera** — Fix: helper condiviso `nascondiBottomNavQuandoTastieraAperta` che rileva la tastiera **misurando l'area visibile** (`getWindowVisibleDisplayFrame`), non gli insets (inaffidabili qui per via di `fitsSystemWindows` + ridimensionamento). Nasconde la bottom nav mentre si scrive.
- **Titoli doppi** (Toolbar + titolo bianco in pagina, stesso testo) — rimossi sulle liste (Le mie richieste, Disponibili, Prese in carico, Attività) e su Nuova richiesta. Su ogni rimozione ri-agganciati i vincoli del `ConstraintLayout`. Padding verticale spostato dentro la RecyclerView (`clipToPadding=false`) per eliminare la "barretta bianca" fissa in cima.
- **Titolo Toolbar dinamico** su Nuova richiesta: "Nuova richiesta" in creazione, "Modifica richiesta" in modifica (impostato dal Fragment).
- **Freccia su "Accedi"** rimossa: `AppBarConfiguration(setOf(splashFragment, loginFragment))`. Login = primo livello. Titolo Toolbar login vuoto (`label=""`), titolo grande in pagina (scelta utente).
- **Adattabilità** — form di auth in `NestedScrollView` (reggono landscape e schermi piccoli); guideline in **percentuale** per posizionare il form del login; testo in `sp`, misure in `@dimen`. Verifica rotazione + emulatore piccolo eseguita. Layout tablet (`sw600dp`) esplicitamente fuori scope.
- **Tema chiaro bloccato** — `CareConnectApp : Application` + `AppCompatDelegate.setDefaultNightMode(MODE_NIGHT_NO)`: niente risorse "night" (evita UI illeggibile in dark mode; `values-night` era vuoto). Criterio "coerenza grafica".

### B.2 — DataBinding + ViewModel MVVM (da 1.5/3 verso 3/3)

- Portato da "DataBinding usato come ViewBinding" (0 espressioni) a **DataBinding dichiarativo** sui 3 profili.
- Schema della **lezione 9**: nel layout `<data><variable name="viewModel" type="...">` + `@{viewModel.campo}`; nel Fragment `binding.viewModel = ...` e `binding.lifecycleOwner = viewLifecycleOwner`; nel ViewModel `MutableLiveData` privato di appoggio + `LiveData` pubblico di sola lettura.
- **Perché LiveData e non StateFlow su queste schermate:** il DataBinding osserva nativamente `LiveData` tramite il `lifecycleOwner` (slide 32 della lezione: "legare a LiveData in XML elimina l'observer nel codice"). Il resto dell'app resta ViewModel + `StateFlow` osservato nel Fragment — l'altra variante MVVM valida (la griglia cita "LiveData/StateFlow").
- **Campi editabili** (bio, indirizzo): niente two-way binding. Restano gestiti a mano (pre-riempiti una volta, letti al Salva) per non entrare in conflitto col pre-riempimento. Buon punto da spiegare all'orale.
- Espressioni usate: testo (`@{viewModel.nome}`, `@{viewModel.valutazione}`) e `android:enabled="@{viewModel.copiaAbilitato}"` sul tasto Copia (Profilo Anziano).

---

## C. File nuovi / modificati in questa sessione

**Nuovi:**
- `com/careconnect/CareConnectApp.kt` — Application, blocco tema chiaro.
- `com/careconnect/ui/common/KeyboardVisibility.kt` — helper nascondi-bottom-nav-con-tastiera.

**Modificati (principali):**
- ViewModel: `ProfiloAnzianoViewModel`, `ProfiloVolontarioViewModel`, `ProfiloFamiliareViewModel` (esposizione LiveData per il binding).
- Fragment: i 3 `Profilo*Fragment` (binding + lifecycleOwner), i 3 `Home*Fragment` (fitsSystemWindows via layout, helper tastiera; Familiare colora la root da collegato), `NuovaRichiestaFragment` (padding IME, titolo Toolbar), `MainActivity` (AppBarConfiguration).
- Layout: 3 `fragment_profilo_*`, 3 `fragment_home_*`, `fragment_nuova_richiesta`, `fragment_login`, `fragment_completa_profilo_google`, le 4 `fragment_*` di lista, `nav_graph_auth` (label login vuota).
- Risorse: `themes.xml` (stile `CareConnect.Card` elevata; eventuali stili sperimentali scartati), `AndroidManifest.xml` (`android:name=".CareConnectApp"`).

**Decisioni estetiche provate e SCARTATE dall'utente (rimandate alla tesi):** card colorate (azzurro/pesca) → lasciate bianche; bottoni principali arancioni → lasciati navy.

---

## D. Prossimi passi

1. **Prova generale anti-malus** (sul dispositivo): sweep anti-crash su ogni flusso, percorso felice con Security Rules attive, comportamento senza rete, controllo che nessun testo scuro finisca su sfondo navy illeggibile. Un crash/glitch in demo costa più di ogni singola feature.
2. **Documento di preparazione all'orale** (passo successivo concordato): spiegazione semplice e difendibile di ogni parte chiave — MVVM + DataBinding, insets/`fitsSystemWindows`, soluzione tastiera/bottom nav, back stack, Security Rules, Worker, FCM.
3. (Opzionale) Allineare `Roadmap.md` allo stato aggiornato (Fasi 11/12/13 completate).

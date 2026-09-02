# Roadmap Tesi — CareConnect

**Finestra di lavoro: 16 agosto → 12 settembre 2026.** (CONSEGNA DEFINITIVA: **12 settembre 2026**)

> Documento operativo **attivo** per la tesi. Riparte da zero: le fasi d'esame (0–14) sono concluse
> e restano solo come *baseline* (app funzionante consegnata).
> Da tenere nella cartella del progetto (sottocartella `docs/`), così ogni sessione Code/Cowork
> parte già con il contesto. Aggiornare solo su richiesta esplicita.

---

## ⚠️ Prima di iniziare (time-sensitive)

- **Firestore / regole di sicurezza:** la modalità test di Firestore aveva scadenza **2 agosto 2026**.
  In Fase 10 sono state pubblicate regole *vere* (basate su ruolo/uid), quindi molto probabilmente
  l'app funziona ancora — ma dato che quella data è ormai passata, **prima di costruire sopra
  (16 agosto) verifica che l'app legga e scriva ancora su Firestore**. Se qualcosa è bloccato,
  la causa è quasi certamente lì.

## Pre-fase (da fare ORA, prima del 16 agosto — cose a lenta risposta)

Non serve aspettare il 16 per ciò che dipende dagli altri o dal setup:
- [ ] **Inviare subito i contatti esterni** (le risposte arrivano con i loro tempi): ente di
      volontariato reale + prof. Camurri (HCI).
- [ ] **Setup ambiente** (vedi sotto): collegare la cartella del progetto a Code/Cowork.
- [ ] **Verifica Firestore** (vedi sopra).

---

## Setup ambiente & file di progetto

**Come si collega** (Claude Desktop, piano a pagamento):
1. App → tab **Code** (il migliore per i sorgenti Android; capisce la struttura). Cowork per il
   documento di tesi e la ricerca sugli enti.
2. *Select a folder* → radice del progetto Android Studio `CareConnect`.
3. Concedi i permessi; **tieni l'app aperta** mentre lavoriamo (l'accesso ai file locali richiede questo).
4. Modalità **Plan/Ask**: io propongo, tu rivedi il diff e applichi → resta "tu guidi, io eseguo".
5. Documenti attivi in una sottocartella `docs/` dentro il progetto.

**Quali file tenere:**

| File | Uso | Dove |
|---|---|---|
| **(intero progetto Android Studio)** | il codice — la cosa più importante da collegare | radice cartella |
| `Roadmap_Tesi.md` (questo) | piano attivo | `docs/` |
| `Design_System_CareConnect.md` | riferimento grafico attivo | `docs/` |
| `Visione_e_Requisiti.md` | direzione / visione | `docs/` |
| `Project_State.md` | stato tecnico + decisioni (palette, architettura) | `docs/` |
| `Roadmap.md` (vecchia, esame) | archivio storico (decisioni, rischi, gotcha emulatore) | `docs/archivio/` |
| `HANDOFF_4_Fase13...md` | archivio (cosa fatto in rifinitura) | `docs/archivio/` |
| Specifiche esame (PDF) | non più il target (esame concluso); solo se vuoi i vincoli documentati | opzionale |
| **PDF del corso (lezioni 0–15)** | **NON caricarli**: materiale didattico, ingombrano e non aiutano. Eccezione: `15 Background Tasks & Services.pdf`, solo se faremo l'SOS in background | fuori dalla cartella |

---

## Priorità (decise con l'utente)

1. **Home Anziano (modifiche)** 🔴
2. **Sistema SOS Anziano** 🔴
3. **Chat integrata Anziano ↔ Volontario** 🔴

> **Grafica / Design System:** non è più un blocco di lavoro a sé.
> La parte grafica la gestisce l'utente, aggiungendo **skill dedicate** quando lo
> riterrà necessario. Fino ad allora non si apre un cantiere "Design System": si
> rispetta la palette/gli stili già esistenti nel codice.

Il resto → in secondo piano, sotto **Backlog**, con ordine deciso da me.

---

## Calendario (16 agosto → 2 settembre)

### 16–17 ago · T0 — Setup + verifica Firestore 🔴
- Collegare la cartella (✅ fatto) e verificare che l'app legga/scriva ancora su Firestore
  (test a runtime, da fare con l'app in esecuzione — vedi ⚠️ in alto).
- **Design System:** non più parte del T0. La grafica la gestisce l'utente con skill
  dedicate quando lo riterrà necessario (vedi nota in "Priorità").

### 18–22 ago · T1 — Home Anziano ✅ COMPLETATO (16 ago)
- ✅ Home = form **"Nuova richiesta" diretto** (nuovo `NuovaRichiestaHomeFragment` +
  `NuovaRichiestaHomeViewModel`, una schermata/un ViewModel).
- ✅ **Bottom nav** a 3 voci: *Nuova richiesta* (home) · *Le mie richieste* · *Profilo*.
- ✅ **Banner "richiesta in corso"** in cima al form: acceso solo se ci sono richieste
  attive (aperta/presa in carico/completata), tap → "Le mie richieste".
- ✅ **SOS** ricollocato in fondo alla Home (interni ridisegnati in T2).
- ✅ Split **creazione/modifica**: `NuovaRichiestaFragment` resta solo per la modifica.
- ✅ Accessibilità: target ≥ 56dp, testo scalabile (`sp`), contrasto palette esistente.
- ⏭️ **TTS rimandato a T2** (deciso con l'utente): si introduce dove serve davvero, con l'SOS.
- Testato end-to-end su dispositivo fisico (11 punti di verifica ok).

### 23–27 ago · T2 — SOS ripensato ✅ COMPLETATO (17 ago)
- ✅ **Doppio trigger, stesso percorso di codice:** pulsante SOS rosso + scuotimento
  (`ShakeDetector`, accelerometro), confluiscono nello stesso overlay via `avviaFlussoSos()`.
- ✅ **Conferma robusta:** overlay translucido `ConfermaSosDialogFragment` (countdown 5→0 in un
  cerchio rosso + **ANNULLA** enorme) + voce `TtsHelper` ("Sto per chiamare aiuto" + conteggio).
  Il conteggio è legato a `onStart/onStop`: si ferma uscendo dall'app, niente chiamate in background.
- ✅ **Fine countdown:** `inviaSos()` (un `SosAlert` per familiare → push FCM già esistente) +
  `ACTION_DIAL` verso **112**. ANNULLA prima dello zero non scrive nulla (nessun falso allarme).
- ✅ **v1:** scuotimento **solo ad app aperta**. Background (app chiusa) → **estensione prioritaria**
  post-blocchi (richiede Foreground Service, vedi Backlog).
- Testato end-to-end su dispositivo fisico. Dettaglio in `Project_State` §0bis (T2).

### 28 ago–1 set · T3 — Chat Anziano ↔ Volontario ✅ COMPLETATO (18–19 ago)
*Era il blocco più a rischio: nuova collezione + regole + realtime + safeguarding. Consegnato in anticipo.*
- ✅ **Modello dati:** collezione top-level `messaggi` (scelta motivata vs sotto-collezione) +
  **security rules dedicate** pubblicate (create solo dai partecipanti e solo se `presa_in_carico`;
  read partecipanti + garante; update/delete = false → messaggi immutabili).
- ✅ **Realtime** via `callbackFlow` (coerente con SOS/Request). Decisione tecnica emersa in test:
  le rules non sono filtri → la query deve vincolare il campo controllato dalla regola.
- ✅ **UI** chat condivisa a 3 ruoli: lato Anziano semplificata con **TTS** (riuso `TtsHelper`),
  lato Volontario standard, lato Garante in sola lettura.
- ✅ **Safeguarding:** il garante (familiare collegato) legge la chat dell'assistito in **sola
  lettura** + **avviso di trasparenza** ai due partecipanti. È il punto che rende la feature
  difendibile per un ente reale.
- ✅ **Notifiche:** push FCM via nuova Cloud Function `notificaNuovoMessaggio` (gemella dell'SOS),
  testata anche a telefono bloccato. Lato Android nessuna modifica (canale generale già presente).
- ⏭️ **Rimandati:** test negativi multi-profilo delle rules (volontario non assegnato, accessi negati)
  per mancanza di profili di prova. Dettaglio completo in `Project_State` §0bis (T3).

> **Grafica della chat (tutte le viste) + evidenza del pulsante Chat:** NON fanno parte di T3.
> Confluiscono nella **fase grafica finale** (vedi Backlog), che l'utente affronterà per ultima,
> dopo aver installato una skill grafica dedicata per scelte mirate.

### 23–27 ago · T4 — Scuotimento SOS in background (Foreground Service) ✅ COMPLETATO (21 ago)
- ✅ **`SosShakeService`** (Foreground Service `specialUse`) tiene attivo l'accelerometro anche ad app
  chiusa, con notifica permanente discreta. Riusa lo stesso `ShakeDetector` di T2.
- ✅ **Sensore accelerometro wake-up** (con fallback): eventi anche a schermo spento **senza wake lock**
  → nessun costo batteria da CPU sempre sveglia.
- ✅ **`ConfermaSosActivity`** a tutto schermo: riusa layout, countdown/TTS/ANNULLA e la stessa
  `inviaSos()` di T2 (**logica SOS non duplicata**); appare anche a telefono bloccato.
- ✅ **Apertura diretta dell'overlay** anche fuori dall'app a schermo sbloccato, via permesso
  **"Compari sopra le altre app"** (`SYSTEM_ALERT_WINDOW`, chiesto una volta); la notifica full-screen
  resta solo come fallback. Risolve il caso in cui, da sbloccato fuori dall'app, Android non lascia a
  un'app in background aprire una Activity.
- ✅ **Sempre attiva di default (opt-out):** toggle "Protezione SOS" nel Profilo Anziano; il logout la
  ferma. Coordinamento anti-doppia-rilevazione tra Service e Home (`CareConnectApp.inPrimoPiano`).
- Limiti dichiarati in tesi: OEM aggressivi possono uccidere il service (affidabilità non 100%);
  device senza sensore wake-up → rilevazione ridotta in sospensione profonda; `SYSTEM_ALERT_WINDOW` è
  permesso sensibile, qui giustificato. **Non incluso** (scelta): persistenza dopo reboot.
- Testato end-to-end su dispositivo fisico. Dettaglio in `Project_State` §0bis (T4).

### 28 ago · T5 — Stelle nel profilo Volontario ✅ COMPLETATO (22 ago)
- ✅ `ratingMedio` mostrato come **stelline** (`RatingBar` in modalità indicatore, `stepSize=0.5` per le
  mezze stelle, colore `care_accent`) in **entrambi** i punti in cui appare il rating: profilo proprio del
  volontario (`fragment_profilo_volontario.xml` + `ProfiloVolontarioViewModel`) e dialog pubblico di sola
  lettura visto da Anziano/Familiare (`dialog_profilo_volontario.xml` + `ProfiloVolontarioDialog.kt`).
- ✅ **Accessibilità:** le stelle **affiancano** il numero, non lo sostituiscono (regola "mai il
  colore/forma da solo"); `contentDescription` con il valore per lo screen reader; microcopy con virgola
  italiana ("4,5 / 5"). Caso "non ancora valutato" → **nessuna stella** (una `RatingBar` a 0 sembrerebbe
  un voto pessimo), resta solo il testo.
- Testato su dispositivo fisico (con e senza valutazione, in entrambe le viste).
- ✅ **Solo stelle (22 ago):** rimossa la parte numerica ("4,5 / 5"), restano le sole stelle; senza voto
  resta il testo "Non ancora valutato". La lettura vocale è garantita dalla `contentDescription`.

### 29 ago · T6 — Vista "i miei garanti collegati" (Anziano) ✅ COMPLETATO (22 ago)
- ✅ Nel profilo Anziano, nuova card "I tuoi familiari collegati" (sotto il codice invito) con l'elenco
  dei familiari collegati; con lista vuota, messaggio "Nessun familiare collegato. Condividi il codice
  qui sopra". Sola lettura (trasparenza), caricamento una volta come il resto del profilo.
- ✅ **Vincolo dati:** il documento `User` su Firestore non contiene l'email → si mostra il **solo nome**
  di ogni familiare.
- ✅ **Fix security rules (necessario):** l'`allow read` di `users` non permetteva a un anziano di leggere
  il documento di un familiare (c'erano sé stesso, i volontari, e "familiare legge anziano", ma non il
  verso opposto) → la lettura del nome falliva in silenzio e la lista restava vuota. Aggiunta la clausola
  simmetrica in OR: `resource.data.get('ruolo','') == 'familiare' && resource.data.get('anzianoCollegatoId','') == request.auth.uid`.
  Nessuna modifica al codice dell'app.
- ✅ **File:** `ProfiloAnzianoViewModel.kt` (LiveData `garanti` + `caricaGaranti`), `ProfiloAnzianoFragment.kt`
  (`osservaGaranti`), `fragment_profilo_anziano.xml` (card), nuovo `item_garante_collegato.xml`.
- Testato su dispositivo fisico dopo la pubblicazione delle rules.

### 30 ago–1 set · T7 — Pagina "Servizi sanitari a domicilio" (informativa nazionale) — ⏸️ ACCANTONATO (29 ago)
> **Accantonato (deciso con l'utente):** è l'unico blocco "informativo di servizio pubblico" in un'app
> che per il resto fa una cosa coerente (contatto anziano↔volontario↔familiare) → eterogeneo e difficile
> da difendere all'orale come funzione. **Da presentare come capitolo "sviluppi futuri / fattibilità
> normativa" della tesi, non da implementare ora**; eventuale ripresa tardiva a basso costo (statico).
> Dettaglio e motivo anche in `Project_State` §9. Descrizione originale del blocco qui sotto.

Schermata **condivisa di sola informazione** (una UI riusata), che *indica canali ufficiali* e **non
eroga prestazioni**. Contenuto in schede (con **lettura vocale TTS** lato Anziano):
- **Emergenze:** 112 (NUE) / 118 — coerente con l'SOS già esistente.
- **Cure non urgenti:** **116117** — continuità assistenziale (ex guardia medica), h24, gratuito,
  multilingua.
- **Assistenza Domiciliare Integrata (ADI):** cos'è (medico/infermiere/riabilitazione a casa, gratuita,
  rientra nei LEA del SSN) e come si attiva (richiesta tramite il **medico di base**). L'ADI presuppone
  un supporto familiare → si lega al ruolo del familiare nell'app. È il canale giusto per prestazioni
  cliniche (es. una puntura): la fa l'infermiere ADI, non il volontario.
- **Collocazione (decisa con l'utente):** presente in **tutti e tre i profili** — Anziano (con TTS),
  Familiare e Volontario — con una **sola schermata condivisa** riusata. È informazione generica
  nazionale, quindi nessun problema di privacy. *(Opzione da valutare: limitarla ad Anziano +
  Familiare, se sul Volontario risultasse poco pertinente.)*
- Info generica nazionale → **nessun dato personale, nessuna responsabilità clinica**. Contenuto statico
  e stabile (numeri/servizi cambiano di rado): manutenzione quasi nulla; realizzabile come schede
  statiche (eventuale piccola collezione Firestore se un domani si vorranno aggiornare da remoto).
  → **Sonnet**. Fonti: ADI (LeggiOggi, Medicasa), 116117 (ASL Roma 1, Wikipedia) — vedi `Project_State`.

## Chiusura verso la consegna — 🎯 CONSEGNA DEFINITIVA **12 settembre 2026**

Ordine e stime (target, con margine). Le date sono indicative, il vincolo fermo è il **12 set**.

### 1 · Grafica residua — ✅ CHIUSA (31 ago)
- ✅ **Colore definitivo confermato:** prugna più scura `#4A2140` (variant `#3A1A33`), applicata solo
  cambiando i valori dei token in `colors.xml`. Contrasti verificati.
- ✅ **Icona launcher ricolorata:** sfondo prugna `#4A2140`, cuore bianco + pesca `#E39B7B`; arancione
  `#F26522` rimosso da tutto il progetto.
- ✅ **Logo:** il cuore ricolorato è il marchio definitivo. Nessun logo in-app/splash separato — lo
  splash resta sobrio (scelta confermata, brandizzazione scartata).
- ✅ **Bibliografia font (Lexend)** aggiunta in `Grafica_Design_e_Skill.md` §2.

### 2 · Pulizia del codice puro (2 – 4 set, 3 gg — oggi compreso) 🟡 IN CORSO
- **Pulizia generale del codice:** codice morto, TODO/commenti di servizio, import e log di debug
  inutilizzati, e le **scorciatoie "solo demo"** (es. il long-press sulla Toolbar che lancia subito il
  Worker, `eseguiOraPerDemo`).
- **Metodo (deciso con l'utente, 2 set):** revisione file per file; per le funzioni indicate dall'utente
  Claude fornisce una **spiegazione semplice** prima di proporre qualsiasi modifica. Modalità manuale
  (Claude propone, l'utente applica).
- Al termine: verificare **build pulita** e che l'app compili/funzioni dopo le rimozioni.

### 3 · Rimozione tracce di tooling (5 set, 1 gg)
- **Rimuovere `.claude/` e le skill** dal progetto; rimuovere i file **`CLAUDE.md`** (radice e `app/`).
- Rimuovere **riferimenti a Claude / AI / strumenti** in commenti, nomi e documenti interni non
  pertinenti al progetto Android da consegnare. Valutare quali file `docs/` tenere: alcuni servono alla
  tesi (fonti, design reference), altri sono note di lavoro da togliere dall'archivio di consegna.
- Verificare che l'app compili dopo le rimozioni (`.claude/skills`, `CLAUDE.md` e `docs/` non sono
  compilati né finiscono nell'APK → rimozione sicura). Preparare l'**archivio di consegna**
  (progetto pulito, eventuale README di consegna).

### 4 · Slide + screenshot (6 – 9 set, 4 gg)
- Costruire le slide (gestite dall'utente) e produrre gli **screenshot per la tesi** (schermate chiave
  dei 3 ruoli, prima/dopo grafica). Materiale utile già pronto: `README_HCI_Colori_Anziani.md`
  (fonti percezione colore anziani) e `Design_Reference_CareConnect.md` (scelte di design difendibili).

### 5 · Buffer (10 – 11 set)
- Margine di sicurezza prima della consegna: rifiniture last-minute, imprevisti, e lo slot per i
  **test aperti** qui sotto se non ancora incastrati.

### 6 · 🎯 CONSEGNA DEFINITIVA — 12 settembre 2026
- Upload del progetto pulito + slide. (Ricordare: iscrizione appello + eventuale mail al docente, vedi
  regole di consegna in `Visione_e_Requisiti.md` §4.)

> ✅ **Rifinitura tecnica + test finali (device + test negativi rules T3): dati per conclusi** (2 set).

> Nota: l'esame dichiara "uso responsabile dell'AI (non vietato, non abusabile)"; la rimozione delle
> tracce di tooling è una scelta di consegna dell'utente ed è sicura per il build (quei file non sono
> compilati). Attenzione solo a non rimuovere per errore risorse effettivamente usate dall'app.

### Fase GRAFICA — ✅ CHIUSA (31 ago)
> **Fatto:** restyle completo dell'app. Palette ricolorata su **Prugna**, colore definitivo confermato
> su una **prugna più scura** (`#4A2140` + pesca), font **Lexend** ovunque, componenti e schermate
> allineati (Home Anziano, liste, Profilo, Chat, Auth con etichette flottanti), bottom nav bianca,
> inset edge-to-edge rivisti, **icona launcher ricolorata**. **Riferimento/direttiva:**
> `Design_Reference_CareConnect.md`; fonti HCI: `README_HCI_Colori_Anziani.md`; bibliografia font in
> `Grafica_Design_e_Skill.md` §2. Dettaglio in `Project_State` §0bis.
> **Nessun punto aperto:** colore e icona chiusi il 31 ago; splash lasciato sobrio (brandizzazione scartata).
>
> Punti storici della fase (tutti coperti):
- **Chat in tutte le viste** (anziano, volontario, garante): bolle, colori/contrasto, distinzione
  mittenti, intestazione, avviso di trasparenza, barra di invio, accessibilità testo grande.
- **Pulsante Chat più riconoscibile** (lato volontario e anziano).
- **Chat in sola lettura senza messaggi:** NON mostrare "Scrivi per iniziare" (invito valido solo a
  chat attiva); eventualmente un testo neutro o nulla.
- **Home Anziano — grafica dedicata:** palette e bottoni più sofisticati/usabili per la terza età, con
  riferimenti a studi su colori/contrasto/dimensione dei target (materiale utile anche al capitolo HCI).

> **Sviluppi futuri — da dimostrare / dire nella discussione di laurea (implementazione MOLTO opzionale):**
> geolocalizzazione (matching per vicinanza), dashboard familiare arricchita, percorsi guidati (HCI),
> "guida ai servizi locali" (spesa/farmacia a domicilio, curata dall'ente e/o suggerita dal volontario,
> per non chiudere l'anziano in una bolla digitale). Sono materiale di **discussione per la laurea**,
> non impegni di sviluppo.
> **Azione per la tesi:** contattare un ente di volontariato per anziani (es. **Comunità di
> Sant'Egidio**) per sapere se hanno già supporti informatici/tecnologici per queste attività, così da
> **rafforzare in sede di discussione** il posizionamento "app a supporto di enti già esistenti".
> **Posizionamento (fermo):** anche quando l'app è usata da un ente, la **supervisione del familiare
> resta e non è soppiantata dall'ente**.

---

## Backlog — "il resto" (ordine di priorità proposto da me)

Solo se avanza tempo, o in parallelo al documento di tesi. Ordinato per rapporto valore-tesi/sforzo
e sinergia:

> 🔴 **Prima priorità dopo i blocchi pianificati (decisa con l'utente) — ora PROMOSSO a T4 (pianificato, vedi sopra):**
> **scuotimento SOS in
> background** — estensione di T2. Un **Foreground Service** che tiene attivo l'accelerometro anche
> ad app chiusa (con notifica permanente), così lo scuotimento fa scattare l'SOS anche fuori
> dall'app. Non tocca la logica SOS esistente né la grafica: cambia solo *dove vive* il sensore.
> Limite da dichiarare in tesi: gli OEM aggressivi (Xiaomi/Huawei/Samsung in risparmio energetico)
> possono uccidere il service in background → affidabilità non garantita al 100%. Le voci numerate
> qui sotto restano "il resto" e vengono dopo.

1. **Geolocalizzazione** — già costruita all'esame e ritirata → basso costo di ripristino, alto valore
   reale (matching per vicinanza, cosa che un ente vero apprezza).
2. **Dashboard familiare arricchita** — frequenza richieste, tempi medi; rafforza il tema "supervisione".
3. **Percorsi guidati** (tutorial assistenza digitale) — forte sinergia col tema HCI/accessibilità.
4. **Vista "i miei garanti collegati"** (Anziano) — piccola, trasparenza/accessibilità. *(→ PROMOSSO a T6, pianificato.)*
5. **Immagine profilo** — richiede Firebase **Storage** + permessi + rules Storage (nuova dipendenza):
   valore medio, costo maggiore.
6. **Multilingua** — valore reale (anziani stranieri/badanti) ma i18n è ampia; eventualmente parziale.
7. **Reputazione/verifica volontari** — importante per un ente (fiducia/safeguarding) ma potenzialmente
   pesante; possibile "sviluppo futuro" o versione leggera.

---

## Aggiunte senza data (decise con l'utente, da collocare più avanti)

- **Profilo Volontario — valutazione a stelle:** mostrare il `ratingMedio` come
  **stelline** nel profilo del Volontario (oggi è un placeholder). Feature piccola:
  i dati (`ratingMedio`) sono già calcolati e disponibili. *(→ PROMOSSO a T5, pianificato.)*
- **Home Anziano — grafica dedicata:** nella ridefinizione della Home (T1) curare
  anche l'aspetto visivo — palette e bottoni più **sofisticati, usabili e adatti a
  un'utenza anziana**, possibilmente con riferimenti a studi su colori/contrasto/
  dimensione dei target per la terza età (materiale utile anche al capitolo HCI
  della tesi).
  > NB: lo **scuotimento del telefono** per chiamare aiuto NON fa parte della
  > "nuova richiesta" ordinaria. È il **trigger dell'SOS**, già pianificato in **T2**
  > (scuotimento + pulsante rosso → conferma vocale TTS + countdown → `ACTION_DIAL`
  > verso il 112). La grafica la gestirà l'utente con skill dedicate quando servirà.

---

## Prospettiva — Canale servizi sanitari (decisione aperta)

Ipotesi: come funzione del ruolo **Volontario**.
- 🔴 **FLAG:** se un *volontario* (non professionista) eroga prestazioni sanitarie → qualifica/
  responsabilità legale + **GDPR** (dati sanitari = categoria particolare).
- ✅ **Versione difendibile:** il volontario *accompagna* alle visite / *aiuta a prenotare* su canali
  ufficiali (CUP/ASL), **non** esegue atti clinici.
- Se non si scrive codice: ottimo **capitolo "sviluppi futuri / fattibilità normativa"**.

---

## Decisioni ancora aperte

1. ~~**Chat:** collezione separata vs sotto-collezione della richiesta; livello di visibilità per il garante.~~
   ✅ **CHIUSA (T3):** collezione top-level `messaggi`; garante in **sola lettura** + avviso di trasparenza.
2. ~~**Scuotimento in background** (serve Foreground Service): deciso SÌ, estensione prioritaria.~~
   ✅ **CHIUSA (T4, 21 ago):** `SosShakeService` sempre attivo (opt-out), sensore wake-up,
   `ConfermaSosActivity` a tutto schermo, apertura diretta via `SYSTEM_ALERT_WINDOW`. Reboot escluso.
3. **Servizi sanitari:** funzione (non-clinica) sotto Volontario vs solo capitolo prospettive.
4. **Ordine fine del backlog** (modificabile).

*Chiuse:* numero SOS = **112 + notifica al familiare**; ambiente = **Code/Cowork con cartella collegata**;
primo passo = **Design System**.

---

## Note di rischio

- **Verifica Firestore** prima di costruirci sopra (vedi in alto).
- Feature che toccano **dati personali/sanitari** o il **contatto diretto** con l'anziano: valutare
  safeguarding/GDPR *prima* del codice — è anche ciò che rende la tesi difendibile e utile a enti reali.
- **Testare su dispositivo fisico**, non solo emulatore (occhio al gotcha del tasto Indietro
  sull'emulatore, annotato nella vecchia roadmap).
- Non lasciare che un agente riscriva blocchi grossi: alla discussione va difesa **ogni riga**.
- Le date sono **target**, con il 2 set come buffer; **T3 (chat)** è il blocco da comprimere se si slitta.

NOTA MODELLI
Modello e impegno, per tipo di task (da impostare nella nuova chat):

Task complesse / architetturali / di sicurezza — Foreground Service SOS, security rules, qualsiasi cosa tocchi lifecycle o GDPR: Opus, ragionamento alto (come T3).
Task medie — ripristino geolocalizzazione, dashboard familiare: Opus, ragionamento medio (o Sonnet alto).
Task piccole e meccaniche — stelle nel profilo volontario, viste di sola lettura: Sonnet, medio (più veloce ed economico, sufficiente).
Fase grafica finale (dopo la skill): Sonnet per l'implementazione, Opus solo se serve ragionare sul design.
Scrittura/ricerca del documento di tesi: Opus (o Cowork per i documenti lunghi).
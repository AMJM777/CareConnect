# Roadmap Tesi — CareConnect

**Finestra di lavoro: 16 agosto → 2 settembre 2026.**

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

### 18–22 ago · T1 — Home Anziano 🔴
- Home = form **"Nuova richiesta" diretto**.
- **Bottom nav** a 3 voci: *Nuova richiesta* (home) · *Le mie richieste* · *Profilo*.
- **Banner "richiesta in corso"** in cima al form (per non nascondere lo stato dietro al form).
- Rifinitura accessibilità: contrasto WCAG, target ≥ 56dp, testo scalabile, TTS sugli elementi chiave.

### 23–27 ago · T2 — SOS ripensato 🔴
- **Doppio trigger, stesso percorso di codice:** pulsante SOS rosso ben visibile + scuotimento
  (accelerometro). Accanto: *"Oppure scuoti il telefono se sei in pericolo"*.
- **Conferma robusta:** TTS "Sto per chiamare aiuto" + **countdown 5→0** + tasto **ANNULLA** enorme.
- **Fine countdown:** `ACTION_DIAL` verso **112** (apre il compositore, nessun permesso runtime) +
  **notifica in-app al familiare** (già presente dalla baseline) + push FCM.
- **v1:** scuotimento attivo **solo ad app aperta** (background = estensione futura, richiede Service).

### 28 ago–1 set · T3 — Chat Anziano ↔ Volontario 🔴
*Il blocco più a rischio: nuova collezione + regole + realtime + safeguarding.*
- **Modello dati:** collezione messaggi (dedicata o sotto-collezione della richiesta) + **security
  rules dedicate**.
- **Realtime** via `Flow` / `callbackFlow` (coerente con l'architettura repository esistente).
- **UI** chat semplificata e accessibile lato Anziano (testo grande, TTS lettura messaggi); standard
  lato Volontario.
- 🔴 **Safeguarding:** valutare **visibilità/log della chat per il garante** (contatto diretto tra
  persona vulnerabile e volontario — è ciò che rende la feature difendibile e utile a un ente reale).

### 2 set · Rifinitura + buffer + documento
- Test su dispositivo fisico, fix, verifica lifecycle/rotazione.
- Screenshot **prima/dopo** dell'interfaccia Anziano.
- Aggiornare `Project_State.md` e questa roadmap.

**Nota realistica (spirito critico):** i 4 blocchi prioritari sono già un carico pieno e sano per
~2,5 settimane. Se qualcosa slitta, il candidato naturale da comprimere/spostare è parte della
**chat (T3)**, che è la più ampia. Il backlog sotto è realisticamente *stretch* o materiale che
cresce in parallelo al documento — non garantito dentro la finestra.

---

## Backlog — "il resto" (ordine di priorità proposto da me)

Solo se avanza tempo, o in parallelo al documento di tesi. Ordinato per rapporto valore-tesi/sforzo
e sinergia:
1. **Geolocalizzazione** — già costruita all'esame e ritirata → basso costo di ripristino, alto valore
   reale (matching per vicinanza, cosa che un ente vero apprezza).
2. **Dashboard familiare arricchita** — frequenza richieste, tempi medi; rafforza il tema "supervisione".
3. **Percorsi guidati** (tutorial assistenza digitale) — forte sinergia col tema HCI/accessibilità.
4. **Vista "i miei garanti collegati"** (Anziano) — piccola, trasparenza/accessibilità.
5. **Immagine profilo** — richiede Firebase **Storage** + permessi + rules Storage (nuova dipendenza):
   valore medio, costo maggiore.
6. **Multilingua** — valore reale (anziani stranieri/badanti) ma i18n è ampia; eventualmente parziale.
7. **Reputazione/verifica volontari** — importante per un ente (fiducia/safeguarding) ma potenzialmente
   pesante; possibile "sviluppo futuro" o versione leggera.

---

## Aggiunte senza data (decise con l'utente, da collocare più avanti)

- **Profilo Volontario — valutazione a stelle:** mostrare il `ratingMedio` come
  **stelline** nel profilo del Volontario (oggi è un placeholder). Feature piccola:
  i dati (`ratingMedio`) sono già calcolati e disponibili.
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

1. **Chat:** collezione separata vs sotto-collezione della richiesta; livello di visibilità per il garante.
2. **Scuotimento in background** (serve Service) sì/no — per ora **no**.
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

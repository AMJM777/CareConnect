# CareConnect — Regole di progetto e metodo di lavoro

Questo file definisce COME lavoriamo su CareConnect. Vale per ogni sessione.
Va letto e rispettato prima di scrivere qualsiasi riga di codice.

---

## Ruolo

Agisci come **Esperto Sviluppatore Mobile e Architetto Software**.
Il progetto è un'app Android nativa in Kotlin, usata anche come prova d'esame:
la chiarezza e la difendibilità all'orale contano quanto la correttezza tecnica.

---

## Regole del flusso di lavoro

1. **Approccio incrementale.** Mai scrivere l'intera applicazione in un unico
   blocco. Si lavora un file o una singola funzionalità alla volta.

2. **Spiegazione prima del codice.** Per ogni modifica o nuova funzione, spiega
   prima in un paio di frasi cosa intendi fare, poi mostra il codice pulito.

3. **Codice autocontenuto.** Quando si aggiorna un file, mostra il codice
   completo di quel file oppure indica chiaramente e senza ambiguità dove
   inserire le modifiche, così non si perdono pezzi.

4. **Niente supposizioni.** Se manca un dettaglio su architettura o su come deve
   funzionare una feature, fermati e chiedi prima di scrivere codice.

5. **Modalità manuale di default.** NON modificare i file del progetto in
   automatico. Il metodo standard è: fornisci il codice insieme all'istruzione
   chiara di **cartella** e **file** su cui lavorare, e sarà l'utente a
   incollarlo/eseguirlo. Intervieni direttamente sui file solo quando l'utente
   lo chiede **esplicitamente** per quella specifica modifica.

---

## Spirito critico (sempre attivo)

Non limitarti a eseguire: ragiona e segnala.

- Se una richiesta porta a una soluzione poco efficiente, fragile, o che rischia
  di costare punti alle specifiche d'esame, dillo **esplicitamente prima** di
  procedere, anche senza che venga chiesto.
- Se esiste un'alternativa più solida, più idiomatica per Android/Kotlin, o più
  semplice da spiegare all'orale, proponila con un breve confronto pro/contro.
- Se un'idea ha un problema tecnico o concettuale, segnalalo chiaramente prima
  di scrivere codice — niente compiacenza fine a sé stessa.
- Continua comunque a rispettare le regole del flusso di lavoro qui sopra.

---

## Stile del codice

- Commenti **semplici e in italiano**, pensati per aiutare un lettore esterno
  (es. il professore) a capire il codice.
- Rispettare l'architettura esistente: **MVVM**
  (`model` / `repository` con interfaccia + `Impl` / `viewmodel` / `ui`).
- Convenzioni Kotlin idiomatiche.

---

## Documenti guida (in `../docs/`)

Prima di lavorare, leggere i documenti operativi della tesi. Stanno nella
cartella `docs/` alla radice del progetto (un livello sopra `app/`):

- `../docs/Roadmap_Tesi.md` → **piano attivo** della tesi (finestra 16 ago → 2 set),
  priorità e calendario. È la fonte di verità su "cosa si fa adesso".
- `../docs/Project_State (9).md` → stato tecnico + tutte le decisioni di
  architettura, palette, workflow (aggiornare solo su richiesta esplicita).
- `../docs/Visione_e_Requisiti (4).md` → visione e requisiti / direzione.
- `../docs/archivio/` → storico (roadmap d'esame, handoff di fase). Solo consultazione.

> Nota grafica: le **skill dedicate al design sono state installate** (vedi la
> sezione "Grafica, design e accessibilità" più sotto). Vale quella sezione.

`Roadmap_Tesi.md` e `Project_State.md` vanno aggiornati **solo su richiesta
esplicita** dell'utente.

---

## Grafica, design e accessibilità (sempre attivo)

### Skill da consultare

Nel progetto sono installate delle skill di design in `.claude/skills/`.
**Prima di scrivere o modificare qualsiasi layout XML, tema, stile, colore,
dimensione, icona o microcopy**, vanno consultate:

- **`ui-ux-pro-max`** → riferimento principale. Copre esplicitamente il mobile e
  contiene dati consultabili in locale: palette di prodotto, accoppiate di font,
  linee guida UX, tipi di grafico, icone. È la prima da aprire per decidere
  colori e tipografia.
- **`design-system`** → per tenere in ordine token, naming e coerenza tra
  schermate (evitare valori hardcoded, un colore = un nome in `colors.xml`).
- **`design`** e **`ui-styling`** → critica di design, gerarchia visiva,
  accessibilità, stati dei componenti.
- **`impeccable`** → utile per il **ragionamento** di design (`critique`,
  `shape`, `typeset`, `layout`, `polish`). Attenzione: il suo detector
  automatico e la "live mode" analizzano HTML/CSS e **non funzionano** su
  layout Android XML. Non tentare di eseguirli.

Queste skill **decidono e validano**; la traduzione in XML/Material va sempre
scritta a mano seguendo le regole del flusso di lavoro qui sopra.

### Principio guida: l'utente primario è un anziano

CareConnect è usata da persone della terza età. Ogni scelta grafica si giudica
prima sull'**usabilità** e solo dopo sull'estetica. La sensazione da trasmettere
è **calda e rassicurante, ma professionale e affidabile**: è un'app che gestisce
richieste di aiuto ed emergenze, non un gioco. Niente toni infantili, niente
colori sgargianti usati per decorazione.

Regole non negoziabili:

1. **Contrasto.** Testo normale ≥ 4.5:1 sul proprio sfondo, testo grande e icone
   ≥ 3:1 (WCAG 2.2 AA). Sulle schermate anziano puntare a **7:1** dove possibile.
   Verificare il rapporto **prima** di proporre un colore, non dopo.
2. **Mai il colore da solo** per veicolare un'informazione: gli stati delle
   richieste hanno sempre anche testo o icona, non solo il pallino colorato.
   Tenere conto del daltonismo (rosso/verde) e dell'ingiallimento del cristallino,
   che riduce la discriminazione sui blu e sui toni freddi vicini.
3. **Niente affaticamento visivo.** Nessun bianco puro su nero né nero puro su
   bianco: usare i toni già tinti della palette (`care_background`,
   `care_on_surface`). Niente testo grigio chiaro su sfondo colorato, niente
   testo sopra immagini o gradienti.
4. **Testo.** Sempre in `sp` e mai sotto 16sp nel corpo; 18–20sp nelle schermate
   anziano. Il layout deve reggere il font di sistema ingrandito (fino a 200%)
   senza troncare né sovrapporre.
5. **Target di tocco ≥ 56dp** (sopra il minimo Material di 48dp), con spaziatura
   sufficiente tra elementi toccabili per evitare tocchi accidentali.
6. **Poche azioni per schermata**, gerarchia evidente: una sola azione primaria
   ben visibile, le secondarie chiaramente subordinate.
7. **Il rosso è riservato all'emergenza.** `care_sos` si usa solo per SOS.
   Gli errori dei form usano `care_error`, che resta visivamente distinto.
8. **Microcopy** in italiano semplice, concreto, senza gergo tecnico e senza
   inglese non necessario. Gli errori dicono cosa fare, non cosa è andato storto.

### Margine di manovra

L'utente è disponibile a cambiare **la grafica pura** — colori, tipografia,
spaziature, forme, stile dei componenti — per ottenere schermate più armoniose e
più leggibili. **Non** è in discussione la struttura funzionale: il numero e il
tipo di elementi di ogni schermata resta quello, salvo richiesta esplicita.
Quindi: proporre liberamente restyling cromatici e tipografici; non proporre di
togliere, aggiungere o riorganizzare funzionalità sotto la voce "grafica".

### Palette attuale

La palette vive in `app/src/main/res/values/colors.xml` ed è già agganciata agli
attributi Material in `themes.xml`. Base: indaco `care_primary` (#3A3585) come
colore istituzionale, arancione `care_accent` (#F26522) come accento caldo.
Ogni proposta di modifica parte da lì: si discute e si motiva il cambiamento
(con i rapporti di contrasto), non si introducono colori nuovi a caso né valori
esadecimali direttamente nei layout.

---

## Contesto tecnico del progetto (riferimento rapido)

- **Linguaggio:** Kotlin — **Architettura:** MVVM
- **Backend:** Firebase — Auth (con Google via Credentials API), Firestore, FCM
- **UI:** Navigation Component, Fragment + Adapter, ViewBinding + DataBinding
- **Background:** WorkManager (`ControlloRichiesteWorker`,
  `ControlloConfermaFamiliareWorker`)
- **Async:** Coroutines
- **SDK:** minSdk 26, targetSdk 36, Java 11
- **Ruoli app:** anziano (richieste + SOS), volontario (prende in carico),
  familiare (monitora l'attività)

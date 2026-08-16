# CareConnect — Visione, Requisiti e Roadmap Tesi

**Documento di riferimento direzionale.** Da consultare insieme a `Roadmap.md` (piano operativo per fasi/giorni) e `Project_State.md` (stato tecnico aggiornato) ogni volta che si apre una nuova chat su questo progetto.

---

## 0. Regole di collaborazione (da rispettare in ogni chat)

**Metodo di lavoro:**
- Approccio incrementale: un file/una funzionalità alla volta, mai l'intera app in blocco
- Spiegazione prima del codice: sempre una breve descrizione di cosa si intende fare, poi il codice
- Codice autocontenuto: file completi o modifiche chiaramente indicate, per non far perdere pezzi
- Niente supposizioni: se manca un dettaglio su architettura o funzionamento di una feature, fermarsi e chiedere prima di scrivere codice

**Spirito critico richiesto:**
- Mantenere sempre uno spirito critico attivo, non limitarsi a eseguire le richieste
- Se una richiesta porta a una soluzione poco efficiente, fragile, o che rischia di costare punti alle specifiche d'esame, dirlo esplicitamente prima di procedere, anche senza che venga chiesto
- Se esiste un'alternativa più solida, più idiomatica per Android/Kotlin, o più semplice da spiegare all'orale, proporla con un breve confronto pro/contro
- Non essere solo compiacente: se un'idea ha un problema tecnico o concettuale, segnalarlo chiaramente prima di scrivere codice

## 1. Il problema e la visione

Molti anziani che vivono soli affrontano quotidianamente piccole difficoltà pratiche — pagare una bolletta, fare la spesa, capire come usare uno smartphone — che singolarmente sono banali, ma che si accumulano e minano la loro autonomia e sicurezza. Allo stesso tempo, i familiari spesso non possono essere fisicamente presenti tutti i giorni, e mancano strumenti semplici per delegare questi piccoli aiuti a una rete di fiducia mantenendo comunque supervisione.

**CareConnect** nasce per colmare questo spazio: una piattaforma che mette in comunicazione tre figure chiave attorno alla vita quotidiana dell'anziano, con un'interfaccia dedicata pensata per l'accessibilità.

## 2. I tre ruoli utente

### 👴 Anziano
- Interfaccia estremamente semplificata (testi grandi, poche azioni per schermata, icone chiare)
- Può richiedere piccoli aiuti quotidiani (spesa, bollette, assistenza digitale, altro)
- Può lanciare un **SOS** in caso di emergenza (es. caduta)
- Può vedere lo stato delle proprie richieste

### 🤝 Volontario
- Visualizza le richieste di aiuto disponibili nella propria zona
- Può prendere in carico una richiesta
- Segna la richiesta come completata
- Accumula un rating (valutazione a stelle) visibile nel proprio profilo

### 👨‍👩‍👧 Familiare / Garante
- Supervisione a distanza della situazione dell'anziano collegato
- Riceve notifica immediata in caso di SOS
- Gestisce (insieme al parere dell'anziano, se in grado) la valutazione a stelle del volontario a fine servizio
- Vista storica delle richieste passate

## 3. Funzionalità chiave (scope esame)

| Funzionalità | Ruolo coinvolto | Note |
|---|---|---|
| Registrazione/login con scelta ruolo | Tutti | Email/password + Google Sign-In |
| Creazione richiesta di aiuto | Anziano | Tipo, descrizione, eventuale posizione |
| Elenco richieste disponibili | Volontario | Filtrabile/ordinabile per vicinanza (extra) |
| Presa in carico richiesta | Volontario | Cambio stato in tempo reale |
| Completamento richiesta | Volontario | Volontario segna "completata dal volontario" — non chiude la richiesta |
| Conferma finale + valutazione | Familiare (o Anziano) | Il garante conferma il completamento reale e assegna il rating a stelle. Solo dopo la conferma lo stato diventa "confermata" |
| Pulsante SOS | Anziano | Intent `ACTION_DIAL` verso il 112 (apre il compositore, non chiama in automatico — più sicuro, nessun permesso runtime necessario) |
| Notifica SOS | Familiare | Push notification immediata |
| Dashboard supervisione | Familiare | Stato richieste, storico |
| Collegamento Anziano ↔ Familiare | Anziano, Familiare | Meccanismo tipo codice invito |

## 3bis. Workflow stati della richiesta

Il completamento **non è automatico**: il volontario propone il completamento, ma è il garante (o l'anziano, se in grado) a confermarlo definitivamente e a lasciare la valutazione. Questo riflette fedelmente l'idea originale del progetto e va implementato come stato esplicito nel modello dati, non solo come logica applicativa:

```
aperta
  ↓ (volontario prende in carico)
presa_in_carico
  ↓ (volontario segna fatto)
completata_dal_volontario
  ↓ (familiare/anziano conferma + valuta)
confermata
```

Stato alternativo: `annullata` (può essere raggiunto da `aperta` o `presa_in_carico`, es. se l'anziano annulla la richiesta o il volontario rinuncia).

## 4. Requisiti ufficiali d'esame (fonte: specifiche del corso)

> L'esame si basa su un progetto individuale/di gruppo (max 3 persone) di applicazione Android a tema libero che rispetti le seguenti specifiche. Punteggio massimo progetto: 29 punti (soglia sufficienza: >15). Fino a 3 punti aggiuntivi dall'orale. Malus fino a -3 punti per bug/crash evidenti.

| Specifica | Punti max | Come la copre CareConnect |
|---|---|---|
| Autenticazione e Registrazione | 5 | Firebase Auth email/password + Google, registrazione con ruolo, gestione sessione multi-utente |
| Integrazione Firebase | 5 | Cloud Firestore, CRUD completo su `users`/`requests`/`ratings`, security rules per ruolo |
| Architettura UI (Activity/Fragment) | 4 | Single-Activity + Fragment per schermata, modularità per ruolo |
| Complessità dell'applicazione | 4 | 3 ruoli con flussi distinti, gestione richieste, SOS, rating, collegamento familiare-anziano |
| Layout dell'applicazione | 3 | UI dedicata e semplificata per l'anziano, UI standard ma coerente per volontario/familiare |
| DataBinding e ViewModel (MVVM) | 3 | Architettura MVVM completa, LiveData/StateFlow, logica fuori da Activity/Fragment |
| Navigation Component e Toolbar | 2 | Grafo di navigazione condizionato al ruolo, Toolbar per contesto |
| Background Task / Service | 2 | Coroutine/WorkManager per operazioni asincrone (dettaglio da definire in Fase 10) |
| Funzionalità Extra | 1 | Notifiche push (SOS) e/o geolocalizzazione |

**Regole di consegna importanti:**
- Upload entro 7 giorni prima dell'appello + mail a `progmobile@ai-lab.it`
- Iscrizione anche su EasyAcademy (separata dal form)
- Demo live richiesta all'orale (10-15 min, no slide necessarie)
- **Uso responsabile dell'AI**: non vietato ma non abusabile — il docente verifica con strumenti automatici e in sede di orale la capacità di commentare il proprio lavoro. Non saper commentare = punteggio 0 anche con progetto sufficiente.

## 5. Cosa NON fa parte dello scope esame (esplicitamente escluso)

- Integrazione con servizi sanitari professionali (es. prenotazione infermieri per prelievi a domicilio) → idea valida solo come estensione futura, **non va implementata per l'esame**
- Qualsiasi funzionalità che aumenti la complessità senza contribuire a una voce di punteggio, se non a scopo di solidità/qualità generale

## 6. Idee di estensione per la Tesi (fuori scope esame — solo appunti per dopo)

Queste idee **non vanno sviluppate durante la fase d'esame**, ma vale la pena tenerle tracciate qui per non perderle:

- **Canale servizi sanitari professionali**: prenotazione di infermieri/operatori qualificati per prestazioni a domicilio (es. prelievi). Da valutare fattibilità tecnica e soprattutto **normativa** (dati sanitari = categoria particolare GDPR, richiede probabilmente consulenza legale/etica prima di procedere)
- **Interfaccia HCI dedicata all'anziano**: confronto con il prof. Camurri (Human-Computer Interaction) per progettare/validare l'interfaccia semplificata secondo principi di accessibilità per la terza età. Da contattare autonomamente o tramite il docente di riferimento — verificare con il docente di Programmazione Mobile come procedere
- **Materiale utile da raccogliere già ora**: screenshot delle scelte UI per l'anziano, motivazioni di design, eventuali test informali di usabilità — utile per portare qualcosa di concreto al colloquio con Camurri
- **Possibili sviluppi futuri aggiuntivi** (idee libere, da vagliare più avanti):
  - Assistenza digitale strutturata come "percorsi guidati" (es. tutorial passo-passo per videochiamate)
  - Statistiche/dashboard più ricche per il familiare (frequenza richieste, tempo medio di risposta)
  - Sistema di reputazione/verifica identità più solido per i volontari (background check, certificazioni)
  - Modalità multi-lingua per anziani con background migratorio o badanti straniere

**Emerse durante lo sviluppo, dopo la Fase 5 (nuove rispetto alle voci sopra):**
- **Immagine profilo (Volontario, ed eventualmente altri ruoli)**: valutate 3 opzioni durante la Fase 5 (foto vera con Firebase Storage / avatar semplice predefinito scelto da un set fisso / rimandare del tutto). Scelta per l'esame: rimandare. Se ripresa per la tesi, comporta una nuova dipendenza (Firebase Storage), permessi Android per galleria/fotocamera, e security rules dedicate allo Storage, non solo a Firestore
- **Home Anziano ridisegnata come schermata unica semplificata**: bottone "Nuova richiesta" + bottone SOS nella stessa vista principale, invece che su tab separate — pensata per ridurre il numero di passaggi richiesti a un utente anziano. Tocca la Fase 4 (già completata e testata per l'esame) e si interseca con la Fase 11 (SOS, dove il bottone `ACTION_DIAL` è già pianificato): da trattare come task esplicito a sé, non come piccolo ritocco, quando si deciderà di riprenderla
- **Sezione Profilo per l'Anziano**: non prevista nel piano d'esame attuale, menzionata insieme alla ridefinizione della home Anziano sopra
- **Vista "chi sono i miei garanti/familiari collegati" per l'Anziano**: l'anziano vedrebbe l'elenco dei familiari che hanno accesso alle sue attività (distinta dalla Fase 7 della Roadmap, che riguarda la visibilità del *volontario* per Anziano/Familiare, non il collegamento familiare stesso). Da valutare insieme alla Fase 6 (meccanismo di collegamento Anziano↔Familiare) o come estensione successiva

## 7. Stack tecnico di riferimento

Kotlin · XML Layout + DataBinding · MVVM (ViewModel/LiveData) · Navigation Component · Firebase Auth + Cloud Firestore · Coroutine/WorkManager · minSdk 26 · AGP 9.2.1 (Kotlin built-in)

*(Per lo stato di avanzamento tecnico dettagliato e le decisioni già prese, fare sempre riferimento a `Project_State.md`. Per il piano operativo giorno per giorno, fare riferimento a `Roadmap.md`.)*

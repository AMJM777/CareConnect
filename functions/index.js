// cloud function che invia una notifica push al familiare quando l'anziano lancia un sos alla creazione di un documento in sosAlerts

const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const { initializeApp } = require("firebase-admin/app");
const { getFirestore } = require("firebase-admin/firestore");
const { getMessaging } = require("firebase-admin/messaging");
const logger = require("firebase-functions/logger");

// inizializza admin sdk per l'accesso server a firestore e cloud messaging
initializeApp();

// trigger attivato a ogni nuovo documento inserito nella collezione sosAlerts
exports.notificaSosAlFamiliare = onDocumentCreated("sosAlerts/{alertId}", async (event) => {
  const snapshot = event.data;
  if (!snapshot) {
    logger.log("Evento senza dati, esco.");
    return;
  }

  const alert = snapshot.data();
  const familiareId = alert.familiareId;
  const anzianoId = alert.anzianoId;

  if (!familiareId) {
    logger.log("SosAlert senza familiareId, niente da inviare.");
    return;
  }

  const db = getFirestore();

  // recupera il token fcm del familiare destinatario per l'invio
  const familiareSnap = await db.collection("users").doc(familiareId).get();
  const fcmToken = familiareSnap.get("fcmToken");
  if (!fcmToken) {
    logger.log(`Il familiare ${familiareId} non ha un token FCM salvato: niente push.`);
    return;
  }

  // recupera il nome dell'anziano per personalizzare il messaggio di allarme
  let nomeAnziano = "Il tuo assistito";
  if (anzianoId) {
    const anzianoSnap = await db.collection("users").doc(anzianoId).get();
    nomeAnziano = anzianoSnap.get("nome") || nomeAnziano;
  }

  const titolo = "Allarme SOS";
  const testo = `${nomeAnziano} ha lanciato un allarme SOS`;

  // costruisce il messaggio con priorità alta forzando il canale sos sia in background che in foreground
  const messaggio = {
    token: fcmToken,
    notification: { title: titolo, body: testo },
    android: {
      priority: "high",
      notification: { channelId: "careconnect_sos" },
    },
    data: { tipo: "sos", titolo: titolo, testo: testo },
  };

  // invia la notifica e registra l'esito o l'eventuale errore nei log
  try {
    const id = await getMessaging().send(messaggio);
    logger.log(`Push SOS inviata al familiare ${familiareId}: ${id}`);
  } catch (errore) {
    logger.error(`Invio push SOS fallito per ${familiareId}:`, errore);
  }
});


// cloud function che invia una notifica push al destinatario quando in
// messaggi viene creato un nuovo messaggio di chat
exports.notificaNuovoMessaggio = onDocumentCreated("messaggi/{messaggioId}", async (event) => {
  const snapshot = event.data;
  if (!snapshot) {
    logger.log("Evento senza dati, esco.");
    return;
  }

  const messaggio = snapshot.data();
  const mittenteId = messaggio.mittenteId;
  const anzianoId = messaggio.anzianoId;
  const volontarioId = messaggio.volontarioId;
  const testo = messaggio.testo || "";

  // il destinatario è il partecipante diverso dal mittente
  const destinatarioId = (mittenteId === anzianoId) ? volontarioId : anzianoId;
  if (!destinatarioId) {
    logger.log("Destinatario non determinabile, niente push.");
    return;
  }

  const db = getFirestore();

  // recupera il token FCM del destinatario
  const destinatarioSnap = await db.collection("users").doc(destinatarioId).get();
  const fcmToken = destinatarioSnap.get("fcmToken");
  if (!fcmToken) {
    logger.log(`Il destinatario ${destinatarioId} non ha un token FCM salvato: niente push.`);
    return;
  }

  // recupera il nome del mittente per personalizzare la notifica
  let nomeMittente = "Qualcuno";
  if (mittenteId) {
    const mittenteSnap = await db.collection("users").doc(mittenteId).get();
    nomeMittente = mittenteSnap.get("nome") || nomeMittente;
  }

  const titolo = `Messaggio da ${nomeMittente}`;
  // anteprima breve del testo nel corpo della notifica
  const anteprima = testo.length > 120 ? testo.substring(0, 117) + "..." : testo;

  // canale "generale" (non SOS): tipo="messaggio" fa scegliere al lato Android
  // il canale a importanza normale
  const messaggioPush = {
    token: fcmToken,
    notification: { title: titolo, body: anteprima },
    android: {
      priority: "high",
      notification: { channelId: "careconnect_generale" },
    },
    data: {
      tipo: "messaggio",
      titolo: titolo,
      testo: anteprima,
      requestId: messaggio.requestId || "",
    },
  };

  // invia la notifica e registra l'esito o l'eventuale errore nei log
  try {
    const id = await getMessaging().send(messaggioPush);
    logger.log(`Push messaggio inviata al destinatario ${destinatarioId}: ${id}`);
  } catch (errore) {
    logger.error(`Invio push messaggio fallito per ${destinatarioId}:`, errore);
  }
});
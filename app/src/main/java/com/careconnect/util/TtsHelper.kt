package com.careconnect.util

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

// Helper riusabile per la sintesi vocale (Text-To-Speech) in italiano
// L'inizializzazione del motore TTS è asincrona: finchè non è pronto,
// le frasi richieste vengono messe in coda e pronunciate appena possibile
class TtsHelper(
    context: Context,
    private val onPronto: (() -> Unit)? = null
) : TextToSpeech.OnInitListener {

    // application context: non trattiene Activity/Fragment (evita memory leak)
    private val tts = TextToSpeech(context.applicationContext, this)

    private var pronto = false

    // frasi arrivate prima che il motore fosse pronto: le dico appena posso
    private val inAttesa = mutableListOf<String>()

    // chiamato dal sistema quando il motore TTS ha finito di inizializzarsi
    override fun onInit(status: Int) {
        if (status != TextToSpeech.SUCCESS) {
            Log.w(TAG, "Inizializzazione TTS fallita: status=$status")
            return
        }

        // imposta l'italiano; se manca lo segnalo ma non blocco l'app
        val esito = tts.setLanguage(Locale.ITALIAN)
        if (esito == TextToSpeech.LANG_MISSING_DATA || esito == TextToSpeech.LANG_NOT_SUPPORTED) {
            Log.w(TAG, "Lingua italiana non disponibile per il TTS")
        }

        pronto = true

        // svuota la coda accumulata durante l'init
        inAttesa.forEach { frase -> parla(frase) }
        inAttesa.clear()

        onPronto?.invoke()
    }

    // pronuncia una frase. Se il motore non è ancora pronto la mette in coda.
    // svuotaCoda=true interrompe ciò che sta dicendo e parte subito (frase iniziale);
    // false accoda dopo le frasi in corso (numeri del conteggio)
    fun parla(testo: String, svuotaCoda: Boolean = false) {
        if (!pronto) {
            inAttesa.add(testo)
            return
        }
        val modo = if (svuotaCoda) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
        tts.speak(testo, modo, null, testo.hashCode().toString())
    }

    // interrompe ciò che sta dicendo senza spegnere il motore (riusabile:
    // serve a mettere in pausa la voce quando l'app va in background)
    fun interrompi() {
        tts.stop()
    }

    // ferma la voce e libera il motore: da chiamare quando la schermata viene distrutta
    fun chiudi() {
        tts.stop()
        tts.shutdown()
        pronto = false
    }

    private companion object {
        const val TAG = "TtsHelper"
    }
}
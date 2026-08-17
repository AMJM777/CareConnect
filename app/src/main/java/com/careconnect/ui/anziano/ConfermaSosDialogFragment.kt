package com.careconnect.ui.anziano

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.lifecycleScope
import com.careconnect.R
import com.careconnect.util.TtsHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job

// Overlay di conferma dell'SOS: countdown 5-4-3-2-1 con voce (TTS) e ANNULLA enorme.
// Non contiene la logica dell'SOS: alla fine del countdown comunica alla Home
// (via Fragment Result) l'esito CONFERMATO, e la Home fa partire allarme + chiamata.
class ConfermaSosDialogFragment : DialogFragment() {

    private lateinit var tts: TtsHelper
    private var secondiRimasti = SECONDI_INIZIALI
    private var conclusa = false   // evita doppi esiti (annulla + fine countdown)
    private var countdownJob: Job? = null   // il conteggio in corso, cosi' lo posso fermare

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // tema translucido a tutto schermo definito in styles.xml
        setStyle(STYLE_NO_FRAME, R.style.CareConnect_Dialog_Sos)
        // non si chiude toccando fuori o col tasto Indietro: si esce solo con ANNULLA
        isCancelable = false
        // dopo una rotazione riprende il conteggio invece di ripartire da 5
        savedInstanceState?.let { secondiRimasti = it.getInt(KEY_SECONDI, SECONDI_INIZIALI) }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.dialog_conferma_sos, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val annullaButton = view.findViewById<Button>(R.id.annullaButton)

        // voce: prima la frase, poi i numeri verranno detti a ogni secondo
        tts = TtsHelper(requireContext())
        tts.parla("Sto per chiamare aiuto", svuotaCoda = true)

        annullaButton.setOnClickListener { annulla() }
        // il countdown NON parte qui: parte in onStart(), cosi' e' legato alla visibilita'
    }

    // onStart/onStop sono i callback affidabili di visibilita': il countdown
    // vive solo mentre l'overlay e' davvero a schermo. Uscendo dall'app si ferma,
    // rientrando riprende dal numero rimasto (niente conteggio/chiamata in background).
    override fun onStart() {
        super.onStart()
        avviaCountdown()
    }

    override fun onStop() {
        super.onStop()
        countdownJob?.cancel()   // pausa il conteggio quando l'app va in background
        tts.interrompi()         // e zittisce la voce
    }

    // conta 5->0: aggiorna il numero, lo pronuncia, e a 0 conferma l'SOS
    // Il conteggio avanza SOLO mentre l'overlay e' in primo piano
    private fun avviaCountdown() {
        if (conclusa) return
        countdownJob?.cancel()   // evita due conteggi paralleli
        val countdownText = view?.findViewById<TextView>(R.id.countdownText) ?: return
        countdownJob = viewLifecycleOwner.lifecycleScope.launch {
            while (secondiRimasti > 0) {
                countdownText.text = secondiRimasti.toString()
                tts.parla(secondiRimasti.toString())
                delay(1000)
                secondiRimasti--
            }
            conferma()
        }
    }

    // ANNULLA: non fa partire nulla (nessun allarme e' stato inviato)
    private fun annulla() {
        if (conclusa) return
        conclusa = true
        dismiss()
    }

    // fine countdown: avvisa la Home che deve far partire allarme + chiamata
    private fun conferma() {
        if (conclusa) return
        conclusa = true
        setFragmentResult(RICHIESTA_KEY, Bundle().apply { putBoolean(RISULTATO_CONFERMATO, true) })
        dismiss()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_SECONDI, secondiRimasti)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        tts.chiudi()   // libera sempre il motore vocale
    }

    companion object {
        const val RICHIESTA_KEY = "conferma_sos"
        const val RISULTATO_CONFERMATO = "confermato"

        private const val SECONDI_INIZIALI = 5
        private const val KEY_SECONDI = "secondi_rimasti"

        fun nuova() = ConfermaSosDialogFragment()
    }
}
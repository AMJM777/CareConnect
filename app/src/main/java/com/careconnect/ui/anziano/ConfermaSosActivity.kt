package com.careconnect.ui.anziano

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.lifecycleScope
import com.careconnect.R
import com.careconnect.repository.AuthRepositoryImpl
import com.careconnect.repository.RequestRepositoryImpl
import com.careconnect.repository.SosRepositoryImpl
import com.careconnect.repository.UserRepositoryImpl
import com.careconnect.util.NotificationHelper
import com.careconnect.util.TtsHelper
import com.careconnect.viewmodel.anziano.NuovaRichiestaHomeViewModel
import com.careconnect.viewmodel.anziano.NuovaRichiestaHomeViewModelFactory
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Versione "a tutto schermo" della conferma SOS, aperta dal Foreground Service
// quando l'app e' chiusa
class ConfermaSosActivity : AppCompatActivity() {

    private val viewModel: NuovaRichiestaHomeViewModel by viewModels {
        NuovaRichiestaHomeViewModelFactory(
            RequestRepositoryImpl(),
            UserRepositoryImpl(),
            SosRepositoryImpl(),
            AuthRepositoryImpl()
        )
    }

    private lateinit var tts: TtsHelper
    private var secondiRimasti = SECONDI_INIZIALI
    private var conclusa = false          // evita doppi esiti (annulla + fine countdown)
    private var countdownJob: Job? = null
    private var sosGiaInviato = false     // dopo la conferma non fermare voce/countdown in onStop

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // deve comparire anche a schermo spento/telefono bloccato
        mostraSuSchermoBloccato()

        setContentView(R.layout.dialog_conferma_sos)

        // toglie la notifica full-screen che ha aperto questa schermata
        NotificationManagerCompat.from(this).cancel(NotificationHelper.ID_NOTIFICA_CONFERMA)

        // dopo una rotazione riprende dal numero rimasto invece di ripartire da 5
        savedInstanceState?.let { secondiRimasti = it.getInt(KEY_SECONDI, SECONDI_INIZIALI) }

        tts = TtsHelper(this)
        tts.parla("Sto per chiamare aiuto", svuotaCoda = true)

        findViewById<MaterialButton>(R.id.annullaButton).setOnClickListener { annulla() }

        // quando l'invio SOS e' concluso (o fallito) chiude la schermata
        osservaEsitoSos()
    }

    // mostra l'Activity sopra il lock screen e accende lo schermo
    private fun mostraSuSchermoBloccato() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
    }

    // il countdown vive solo mentre la schermata si trova in primo piano
    override fun onStart() {
        super.onStart()
        avviaCountdown()
    }

    override fun onStop() {
        super.onStop()
        // non fermare nulla se sto aprendo il dialer dopo la conferma
        if (!sosGiaInviato) {
            countdownJob?.cancel()
            tts.interrompi()
        }
    }

    // conta 5->0: aggiorna il numero, lo pronuncia, e a 0 conferma l'SOS
    private fun avviaCountdown() {
        if (conclusa) return
        countdownJob?.cancel()
        val countdownText = findViewById<TextView>(R.id.countdownText)
        countdownJob = lifecycleScope.launch {
            while (secondiRimasti > 0) {
                countdownText.text = secondiRimasti.toString()
                tts.parla(secondiRimasti.toString())
                delay(1000)
                secondiRimasti--
            }
            conferma()
        }
    }

    // ANNULLA: non invia nulla, chiude e basta
    private fun annulla() {
        if (conclusa) return
        conclusa = true
        finish()
    }

    // fine countdown: riusa inviaSos() del ViewModel e apre il dialer sul 112
    private fun conferma() {
        if (conclusa) return
        conclusa = true
        sosGiaInviato = true

        viewModel.inviaSos()

        try {
            startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:112")))
        } catch (e: ActivityNotFoundException) {
            // nessun dialer sul dispositivo: l'allarme ai familiari è comunque partito
        }

    }

    // aspetta il completamento dell'invio prima di chiudere
    private fun osservaEsitoSos() {
        lifecycleScope.launch {
            viewModel.sosInviato.collect { inviato -> if (inviato) finish() }
        }
        lifecycleScope.launch {
            viewModel.erroreSos.collect { errore -> if (errore != null) finish() }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_SECONDI, secondiRimasti)
    }

    override fun onDestroy() {
        super.onDestroy()
        tts.chiudi()   // libera sempre il motore vocale
    }

    private companion object {
        const val SECONDI_INIZIALI = 5
        const val KEY_SECONDI = "secondi_rimasti"
    }
}

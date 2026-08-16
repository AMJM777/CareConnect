package com.careconnect.ui.anziano

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import com.careconnect.R
import com.careconnect.databinding.FragmentNuovaRichiestaHomeBinding
import com.careconnect.repository.AuthRepositoryImpl
import com.careconnect.repository.RequestRepositoryImpl
import com.careconnect.repository.SosRepositoryImpl
import com.careconnect.repository.UserRepositoryImpl
import com.careconnect.viewmodel.anziano.NuovaRichiestaHomeViewModel
import com.careconnect.viewmodel.anziano.NuovaRichiestaHomeViewModelFactory
import com.careconnect.viewmodel.anziano.NuovaRichiestaUiState
import kotlinx.coroutines.launch

// Home dell'Anziano: form per CREARE una richiesta + banner "richiesta in corso"
// + SOS
class NuovaRichiestaHomeFragment : Fragment() {

    private var _binding: FragmentNuovaRichiestaHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: NuovaRichiestaHomeViewModel by viewModels {
        NuovaRichiestaHomeViewModelFactory(
            RequestRepositoryImpl(),
            UserRepositoryImpl(),
            SosRepositoryImpl(),
            AuthRepositoryImpl()
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_nuova_richiesta_home, container, false
        )
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // padding in basso pari all'altezza della tastiera
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val tastiera = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            v.updatePadding(bottom = tastiera)
            insets
        }

        // il campo "Altro" appare solo se si seleziona quel tipo
        binding.tipoRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            binding.altroInput.visibility =
                if (checkedId == R.id.tipoAltroRadio) View.VISIBLE else View.GONE
            aggiornaHintDescrizione(checkedId)
        }

        binding.inviaButton.setOnClickListener { onInviaClick() }
        binding.sosButton.setOnClickListener { mostraConfermaSos() }

        // tap sul banner -> tab "Le mie richieste"
        binding.bannerInCorso.setOnClickListener {
            val opzioni = navOptions {
                popUpTo(findNavController().graph.startDestinationId)
                launchSingleTop = true
            }
            findNavController().navigate(R.id.mieRichiesteFragment, null, opzioni)
        }

        osservaCreazione()
        osservaBanner()
        osservaSos()
    }

    // hint diverso per tipo, con un esempio concreto di cosa scrivere
    private fun aggiornaHintDescrizione(checkedId: Int) {
        binding.descrizioneInput.hint = when (checkedId) {
            R.id.tipoSpesaRadio ->
                "Es: 2kg di pasta, latte, pane. Lascia la spesa in cucina, busso al citofono."
            R.id.tipoBollettaRadio ->
                "Es: bolletta luce Enel, scadenza 15/07, importo 45€. Da pagare in posta."
            R.id.tipoAssistenzaDigitaleRadio ->
                "Es: aiutami a fare una videochiamata su WhatsApp, ho il telefono nuovo."
            else ->
                "Descrivi cosa ti serve con più dettagli possibili: aiuta il volontario a organizzarsi."
        }
    }

    // valida il form e chiede al ViewModel di creare la richiesta
    private fun onInviaClick() {
        val descrizione = binding.descrizioneInput.text.toString().trim()
        val tipo = tipoSelezionato()

        val erroreValidazione = when {
            tipo == null -> "Seleziona il tipo di aiuto"
            descrizione.isEmpty() -> "Descrivi di cosa hai bisogno"
            else -> null
        }

        if (erroreValidazione != null) {
            mostraErroreLocale(erroreValidazione)
            return
        }

        viewModel.creaRichiesta(tipo!!, descrizione)
    }

    // legge quale RadioButton è selezionato (con testo libero per "Altro")
    private fun tipoSelezionato(): String? = when (binding.tipoRadioGroup.checkedRadioButtonId) {
        R.id.tipoSpesaRadio -> "spesa"
        R.id.tipoBollettaRadio -> "bolletta"
        R.id.tipoAssistenzaDigitaleRadio -> "assistenza_digitale"
        R.id.tipoAltroRadio -> {
            val testoLibero = binding.altroInput.text.toString().trim()
            if (testoLibero.isNotEmpty()) testoLibero else "altro"
        }
        else -> null
    }

    private fun mostraErroreLocale(messaggio: String) {
        binding.errorText.text = messaggio
        binding.errorText.visibility = View.VISIBLE
    }

    // svuota il form dopo una creazione riuscita, così è pronto per la prossima
    private fun pulisciForm() {
        binding.tipoRadioGroup.clearCheck()
        binding.altroInput.text?.clear()
        binding.altroInput.visibility = View.GONE
        binding.descrizioneInput.text?.clear()
        binding.errorText.visibility = View.GONE
    }


    // stato della CREAZIONE richiesta
    private fun osservaCreazione() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { stato -> aggiornaUiCreazione(stato) }
            }
        }
    }

    private fun aggiornaUiCreazione(stato: NuovaRichiestaUiState) {
        val inCaricamento = stato is NuovaRichiestaUiState.Loading
        binding.loadingIndicator.visibility = if (inCaricamento) View.VISIBLE else View.GONE
        // disabilita "Invia" durante il caricamento: evita invii doppi
        binding.inviaButton.isEnabled = !inCaricamento

        when (stato) {
            is NuovaRichiestaUiState.Errore -> {
                mostraErroreLocale(stato.eccezione.message ?: "Errore, riprova")
                viewModel.statoConsumato()
            }
            is NuovaRichiestaUiState.Successo -> {
                pulisciForm()
                Toast.makeText(requireContext(), "Richiesta creata", Toast.LENGTH_SHORT).show()
                viewModel.statoConsumato()
            }
            else -> { /* Idle / Loading: niente da fare qui */ }
        }
    }

    // banner "richiesta in corso": acceso solo se c'è almeno una richiesta attiva
    private fun osservaBanner() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.richiesteAttive.collect { lista ->
                    val quante = lista.size
                    if (quante > 0) {
                        binding.bannerInCorsoText.text = if (quante == 1) {
                            "Hai 1 richiesta in corso. Tocca per vederla."
                        } else {
                            "Hai $quante richieste in corso. Tocca per vederle."
                        }
                        binding.bannerInCorso.visibility = View.VISIBLE
                    } else {
                        binding.bannerInCorso.visibility = View.GONE
                    }
                }
            }
        }
    }

    // esiti dell'SOS: conferma di invio ed eventuali errori
    private fun osservaSos() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.sosInviato.collect { inviato ->
                    if (inviato) {
                        Toast.makeText(requireContext(), "Familiari avvisati", Toast.LENGTH_SHORT).show()
                        viewModel.sosInviatoMostrato()
                    }
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.erroreSos.collect { errore ->
                    if (errore != null) {
                        Toast.makeText(requireContext(), errore, Toast.LENGTH_LONG).show()
                        viewModel.erroreSosMostrato()
                    }
                }
            }
        }
    }

    // chiede conferma prima di inviare: un tap accidentale non deve scattare l'SOS
    private fun mostraConfermaSos() {
        AlertDialog.Builder(requireContext())
            .setTitle("Contattare i soccorsi?")
            .setMessage("Si aprirà la chiamata al 112 e i tuoi familiari saranno avvisati subito.")
            .setPositiveButton("Sì, SOS") { _, _ -> avviaSos() }
            .setNegativeButton("Annulla", null)
            .show()
    }

    // avvisa i familiari e apre il compositore telefonico verso il 112
    private fun avviaSos() {
        viewModel.inviaSos()
        try {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:112"))
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(
                requireContext(),
                "Nessuna app per chiamare trovata sul dispositivo",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
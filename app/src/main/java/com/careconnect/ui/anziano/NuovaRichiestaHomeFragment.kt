package com.careconnect.ui.anziano

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
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
import com.google.android.material.button.MaterialButton
import com.careconnect.repository.AuthRepositoryImpl
import com.careconnect.repository.RequestRepositoryImpl
import com.careconnect.repository.SosRepositoryImpl
import com.careconnect.repository.UserRepositoryImpl
import androidx.appcompat.app.AlertDialog
import com.careconnect.sos.SosShakeService
import com.careconnect.util.OverlaySosPermesso
import com.careconnect.util.ProtezioneSosPrefs
import com.careconnect.util.ShakeDetector
import com.careconnect.viewmodel.anziano.NuovaRichiestaHomeViewModel
import com.careconnect.viewmodel.anziano.NuovaRichiestaHomeViewModelFactory
import com.careconnect.viewmodel.anziano.NuovaRichiestaUiState
import kotlinx.coroutines.launch

// Home dell'Anziano: form per CREARE una richiesta + banner "richiesta in corso" + SOS
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

    // rilevatore di scuotimento: secondo trigger dell'SOS, oltre al bottone
    private lateinit var shakeDetector: ShakeDetector

    // preferenza opt-out della protezione SOS in background (T4)
    private val protezionePrefs by lazy { ProtezioneSosPrefs(requireContext()) }

    // le quattro pillole del tipo di aiuto: selezione singola gestita a mano
    private val tipoButtons by lazy {
        listOf(
            binding.tipoSpesa, binding.tipoBolletta,
            binding.tipoAssistenzaDigitale, binding.tipoAltro
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

        // selezione singola manuale sulle pillole separate
        tipoButtons.forEach { pulsante ->
            pulsante.setOnClickListener { selezionaTipo(pulsante) }
        }

        binding.inviaButton.setOnClickListener { onInviaClick() }
        binding.sosButton.setOnClickListener { avviaFlussoSos() }

        // secondo trigger: lo scuotimento apre lo STESSO overlay di conferma
        shakeDetector = ShakeDetector(requireContext()) { avviaFlussoSos() }

        // riceve l'esito dell'overlay: se confermato (fine countdown), fa partire l'SOS
        childFragmentManager.setFragmentResultListener(
            ConfermaSosDialogFragment.RICHIESTA_KEY, viewLifecycleOwner
        ) { _, bundle ->
            if (bundle.getBoolean(ConfermaSosDialogFragment.RISULTATO_CONFERMATO)) {
                eseguiSos()
            }
        }

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

        // se la protezione in background e' attiva (default), assicura che il
        // Foreground Service sia in esecuzione e chiedi (una volta sola) il
        // permesso che fa aprire la conferma direttamente, senza notifica da toccare.
        if (protezionePrefs.isAttiva()) {
            SosShakeService.avvia(requireContext())
            chiediPermessoFullScreenUnaVolta()
        }
    }

    // Se manca il permesso "Compari sopra le altre app", lo chiede UNA sola volta
    // portando l'utente alle impostazioni. Con il permesso, lo scuotimento apre
    // subito l'overlay in ogni situazione, senza notifica intermedia da toccare.
    private fun chiediPermessoFullScreenUnaVolta() {
        if (OverlaySosPermesso.concesso(requireContext())) return
        if (protezionePrefs.permessoFullScreenGiaChiesto()) return
        protezionePrefs.segnaPermessoFullScreenChiesto()

        AlertDialog.Builder(requireContext())
            .setTitle("Attiva l'apertura automatica")
            .setMessage(
                "Per far comparire subito la richiesta di aiuto quando scuoti il " +
                    "telefono — anche fuori dall'app o a schermo bloccato — concedi a " +
                    "CareConnect il permesso \"Compari sopra le altre app\"."
            )
            .setPositiveButton("Vai alle impostazioni") { _, _ ->
                try {
                    startActivity(OverlaySosPermesso.intentImpostazioni(requireContext()))
                } catch (e: ActivityNotFoundException) {
                    // alcuni dispositivi non hanno questa schermata: si ignora
                }
            }
            .setNegativeButton("Più tardi", null)
            .show()
    }

    // Coordinamento anti-doppia-rilevazione: se la protezione in background e'
    // attiva, il rilevatore vive nel Service (che copre anche l'app aperta),
    // quindi qui NON ne avviamo un secondo. Se e' disattivata, la Home usa il
    // proprio rilevatore come in T2 (solo ad app aperta).
    override fun onResume() {
        super.onResume()
        if (!protezionePrefs.isAttiva()) {
            shakeDetector.avvia()
        }
    }

    override fun onPause() {
        super.onPause()
        shakeDetector.ferma()
    }

    // accende la pillola scelta e spegne le altre, poi aggiorna il campo "altro" e l'hint
    private fun selezionaTipo(scelto: MaterialButton) {
        tipoButtons.forEach { it.isChecked = (it.id == scelto.id) }
        binding.altroInput.visibility =
            if (scelto.id == R.id.tipoAltro) View.VISIBLE else View.GONE
        aggiornaHintDescrizione(scelto.id)
    }

    // hint diverso per tipo, con un esempio concreto di cosa scrivere
    private fun aggiornaHintDescrizione(checkedId: Int) {
        binding.descrizioneInput.hint = when (checkedId) {
            R.id.tipoSpesa ->
                "Es: 2kg di pasta, latte, pane. Lascia la spesa in cucina, busso al citofono."
            R.id.tipoBolletta ->
                "Es: bolletta luce Enel, scadenza 15/07, importo 45€. Da pagare in posta."
            R.id.tipoAssistenzaDigitale ->
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

    // tipo scelto, con testo libero per "altro"
    private fun tipoSelezionato(): String? {
        val checkedId = tipoButtons.firstOrNull { it.isChecked }?.id ?: return null
        return when (checkedId) {
            R.id.tipoSpesa -> "spesa"
            R.id.tipoBolletta -> "bolletta"
            R.id.tipoAssistenzaDigitale -> "assistenza_digitale"
            R.id.tipoAltro -> {
                val testoLibero = binding.altroInput.text.toString().trim()
                if (testoLibero.isNotEmpty()) testoLibero else "altro"
            }
            else -> null
        }
    }

    private fun mostraErroreLocale(messaggio: String) {
        binding.errorText.text = messaggio
        binding.errorText.visibility = View.VISIBLE
    }

    // svuota il form dopo una creazione riuscita, così è pronto per la prossima
    private fun pulisciForm() {
        tipoButtons.forEach { it.isChecked = false }
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

    // apre l'overlay di conferma (countdown + voce). Stesso percorso per bottone
    // e scuotimento. Guardia: non apre due overlay contemporaneamente.
    private fun avviaFlussoSos() {
        if (childFragmentManager.findFragmentByTag(TAG_CONFERMA_SOS) != null) return
        ConfermaSosDialogFragment.nuova().show(childFragmentManager, TAG_CONFERMA_SOS)
    }

    // eseguito SOLO a fine countdown: avvisa i familiari (-> push automatica) e
    // apre il compositore telefonico verso il 112
    private fun eseguiSos() {
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

    private companion object {
        const val TAG_CONFERMA_SOS = "conferma_sos_dialog"
    }
}
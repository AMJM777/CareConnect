package com.careconnect.ui.anziano

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.careconnect.R
import com.careconnect.databinding.FragmentNuovaRichiestaBinding
import com.careconnect.repository.AuthRepositoryImpl
import com.careconnect.repository.RequestRepositoryImpl
import com.careconnect.repository.UserRepositoryImpl
import com.careconnect.viewmodel.anziano.NuovaRichiestaUiState
import com.careconnect.viewmodel.anziano.NuovaRichiestaViewModel
import com.careconnect.viewmodel.anziano.NuovaRichiestaViewModelFactory
import kotlinx.coroutines.launch
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.appcompat.widget.Toolbar

class NuovaRichiestaFragment : Fragment() {

    private var _binding: FragmentNuovaRichiestaBinding? = null
    private val binding get() = _binding!!

    private val viewModel: NuovaRichiestaViewModel by viewModels {
        NuovaRichiestaViewModelFactory(RequestRepositoryImpl(), UserRepositoryImpl())
    }

    private var requestIdInModifica: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_nuova_richiesta, container, false
        )
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Tastiera: diamo allo scroll (la root del layout) un padding in basso
        // pari all'altezza della tastiera quando compare. Serve perché in
        // edge-to-edge la finestra NON si ridimensiona da sola: senza questo,
        // la tastiera coprirebbe i campi. Con il padding, il campo attivo può
        // scorrere sopra la tastiera.
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val tastiera = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            v.updatePadding(bottom = tastiera)
            insets
        }

        binding.tipoRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            binding.altroInput.visibility =
                if (checkedId == R.id.tipoAltroRadio) View.VISIBLE else View.GONE
            aggiornaHintDescrizione(checkedId)
        }

        binding.inviaButton.setOnClickListener { onInviaClick() }

        leggiArgomentiModalita()
        osservaStato()
    }

    /**
     * FASE 7: hint diverso per tipo, con un esempio concreto di cosa
     * scrivere — soluzione leggera per far sì che l'Anziano sia più
     * preciso, invece di costruire una vera chat con il Volontario.
     */
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

    private fun leggiArgomentiModalita() {
        val requestId = arguments?.getString(ARG_REQUEST_ID) ?: return
        val tipoEsistente = arguments?.getString(ARG_TIPO) ?: ""
        val descrizioneEsistente = arguments?.getString(ARG_DESCRIZIONE) ?: ""

        requestIdInModifica = requestId
        // In modifica cambiamo il titolo della Toolbar del ruolo (in creazione
        // resta "Nuova richiesta", impostato dalla label di navigazione). Così
        // la barra indica la modalità senza doppioni in pagina.
        requireActivity().findViewById<Toolbar>(R.id.anzianoToolbar)?.title = "Modifica richiesta"
        binding.inviaButton.text = "Salva modifiche"

        when (tipoEsistente) {
            "spesa" -> binding.tipoSpesaRadio.isChecked = true
            "bolletta" -> binding.tipoBollettaRadio.isChecked = true
            "assistenza_digitale" -> binding.tipoAssistenzaDigitaleRadio.isChecked = true
            else -> {
                binding.tipoAltroRadio.isChecked = true
                if (tipoEsistente != "altro") {
                    binding.altroInput.setText(tipoEsistente)
                }
            }
        }

        binding.descrizioneInput.setText(descrizioneEsistente)
    }

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

        val idInModifica = requestIdInModifica
        if (idInModifica != null) {
            viewModel.modificaRichiesta(idInModifica, tipo!!, descrizione)
        } else {
            val autoreId = AuthRepositoryImpl().utenteCorrente()?.uid
            if (autoreId == null) {
                mostraErroreLocale("Sessione scaduta, effettua di nuovo il login")
                return
            }
            viewModel.creaRichiesta(autoreId, tipo!!, descrizione)
        }
    }

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

    private fun osservaStato() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { stato -> aggiornaUi(stato) }
            }
        }
    }

    private fun aggiornaUi(stato: NuovaRichiestaUiState) {
        // Mostra la rotella solo durante il caricamento.
        val inCaricamento = stato is NuovaRichiestaUiState.Loading
        binding.loadingIndicator.visibility = if (inCaricamento) View.VISIBLE else View.GONE

        // Durante il caricamento disabilitiamo il bottone "Invia": su reti
        // lente l'utente vedrebbe la rotella girare e potrebbe premere di
        // nuovo, creando richieste duplicate. Bloccando il bottone finché
        // l'operazione non finisce, evitiamo invii doppi.
        binding.inviaButton.isEnabled = !inCaricamento

        if (stato is NuovaRichiestaUiState.Errore) {
            mostraErroreLocale(stato.eccezione.message ?: "Errore, riprova")
        }

        if (stato is NuovaRichiestaUiState.Successo) {
            val messaggio = if (requestIdInModifica != null) "Richiesta aggiornata" else "Richiesta creata"
            Toast.makeText(requireContext(), messaggio, Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val ARG_REQUEST_ID = "requestId"
        const val ARG_TIPO = "tipo"
        const val ARG_DESCRIZIONE = "descrizione"
    }
}
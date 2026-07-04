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
import com.careconnect.viewmodel.anziano.NuovaRichiestaUiState
import com.careconnect.viewmodel.anziano.NuovaRichiestaViewModel
import com.careconnect.viewmodel.anziano.NuovaRichiestaViewModelFactory
import kotlinx.coroutines.launch

/**
 * Form per creare O modificare una richiesta (stesso Fragment per entrambi
 * i casi). Se arriva un "requestId" negli argomenti, siamo in modalità
 * modifica: il form si pre-compila e "Invia" diventa "Salva modifiche".
 * Senza argomenti, è la normale creazione di una nuova richiesta.
 */
class NuovaRichiestaFragment : Fragment() {

    private var _binding: FragmentNuovaRichiestaBinding? = null
    private val binding get() = _binding!!

    private val viewModel: NuovaRichiestaViewModel by viewModels {
        NuovaRichiestaViewModelFactory(RequestRepositoryImpl())
    }

    // Se non null, siamo in modalità modifica di QUESTA richiesta.
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

        binding.tipoRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            binding.altroInput.visibility =
                if (checkedId == R.id.tipoAltroRadio) View.VISIBLE else View.GONE
        }

        binding.inviaButton.setOnClickListener { onInviaClick() }

        // Legge gli argomenti PRIMA di osservare lo stato: se siamo in
        // modifica, il form deve già mostrare i dati esistenti al primo
        // frame utile, non dopo un lampo di form vuoto.
        leggiArgomentiModalita()

        osservaStato()
    }

    /**
     * Controlla se siamo stati aperti in modalità modifica (arriva un
     * "requestId" negli argomenti) o creazione (nessun argomento).
     * Se modifica, pre-compila RadioGroup/descrizione e cambia il testo
     * del bottone, così l'utente capisce subito cosa sta facendo.
     */
    private fun leggiArgomentiModalita() {
        val requestId = arguments?.getString(ARG_REQUEST_ID) ?: return
        val tipoEsistente = arguments?.getString(ARG_TIPO) ?: ""
        val descrizioneEsistente = arguments?.getString(ARG_DESCRIZIONE) ?: ""

        requestIdInModifica = requestId
        binding.titoloText.text = "Modifica richiesta"
        binding.inviaButton.text = "Salva modifiche"

        // Se il tipo esistente corrisponde a una delle 3 opzioni fisse,
        // seleziona quel radio. Altrimenti era "Altro" (con o senza testo
        // libero): selezioniamo "Altro" e, se il valore non è letteralmente
        // "altro", lo mostriamo nel campo di testo libero.
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
            // Modalità modifica: aggiorna la richiesta esistente.
            viewModel.modificaRichiesta(idInModifica, tipo!!, descrizione)
        } else {
            // Modalità creazione: come prima, serve l'uid dell'utente loggato.
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
        binding.loadingIndicator.visibility =
            if (stato is NuovaRichiestaUiState.Loading) View.VISIBLE else View.GONE

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
        // Chiavi degli argomenti passati via Bundle per la modalità modifica.
        // Nessun Safe Args nel progetto: usiamo chiavi stringa semplici,
        // coerenti con lo stile già visto altrove.
        const val ARG_REQUEST_ID = "requestId"
        const val ARG_TIPO = "tipo"
        const val ARG_DESCRIZIONE = "descrizione"
    }
}
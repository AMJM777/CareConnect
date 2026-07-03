package com.careconnect.ui.anziano

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioGroup
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
 * Form per creare una nuova richiesta di aiuto (Fase 4, Task 2).
 * Non condivide il ViewModel con nessun'altra schermata (a differenza di
 * AuthViewModel): usiamo viewModels() semplice, scope legato solo a questo Fragment.
 */
class NuovaRichiestaFragment : Fragment() {

    private var _binding: FragmentNuovaRichiestaBinding? = null
    private val binding get() = _binding!!

    private val viewModel: NuovaRichiestaViewModel by viewModels {
        NuovaRichiestaViewModelFactory(RequestRepositoryImpl())
    }

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

        // Il campo "Altro" appare solo se l'utente seleziona quel radio,
        // per non mostrare un campo di testo inutile per Spesa/Bolletta/ecc.
        binding.tipoRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            binding.altroInput.visibility =
                if (checkedId == R.id.tipoAltroRadio) View.VISIBLE else View.GONE
        }

        binding.inviaButton.setOnClickListener { onInviaClick() }

        osservaStato()
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

        // L'autore è sempre l'utente loggato: lo leggiamo qui (non nel
        // ViewModel, che resta indipendente da AuthRepository) tramite
        // AuthRepository.utenteCorrente(), lettura sincrona già vista in Splash.
        val autoreId = AuthRepositoryImpl().utenteCorrente()?.uid
        if (autoreId == null) {
            mostraErroreLocale("Sessione scaduta, effettua di nuovo il login")
            return
        }

        viewModel.creaRichiesta(autoreId, tipo!!, descrizione)
    }

    /**
     * Mappa il RadioButton selezionato sul valore stringa salvato su
     * Firestore (stesse stringhe già definite in Request.kt). Per "Altro",
     * se il campo di testo è compilato usa quel valore, altrimenti resta
     * semplicemente "altro" — esattamente come richiesto.
     */
    private fun tipoSelezionato(): String? = when (binding.tipoRadioGroup.checkedRadioButtonId) {
        R.id.tipoSpesaRadio -> "spesa"
        R.id.tipoBollettaRadio -> "bolletta"
        R.id.tipoAssistenzaDigitaleRadio -> "assistenza_digitale"
        R.id.tipoAltroRadio -> {
            val testoLibero = binding.altroInput.text.toString().trim()
            if (testoLibero.isNotEmpty()) testoLibero else "altro"
        }
        else -> null // nessun radio selezionato
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
            Toast.makeText(requireContext(), "Richiesta creata", Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
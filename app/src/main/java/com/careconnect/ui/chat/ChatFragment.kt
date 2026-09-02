package com.careconnect.ui.chat

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
import androidx.recyclerview.widget.LinearLayoutManager
import com.careconnect.R
import com.careconnect.databinding.FragmentChatBinding
import com.careconnect.repository.AuthRepositoryImpl
import com.careconnect.repository.MessageRepositoryImpl
import com.careconnect.util.TtsHelper
import com.careconnect.viewmodel.chat.ChatViewModel
import com.careconnect.viewmodel.chat.ChatViewModelFactory
import kotlinx.coroutines.launch

/**
 * schermata chat di una richiesta, condivisa da Anziano e Volontario.
 * riceve per argomento gli id della richiesta e dei due partecipanti, il
 * nome dell'altra persona, un flag per la lettura vocale (solo Anziano) e
 * un flag "sola lettura" (chat chiusa: si legge lo storico ma non si scrive).
 */
class ChatFragment : Fragment() {

    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!

    // TTS creato solo se richiesto (lato Anziano); resta null lato Volontario
    private var tts: TtsHelper? = null

    private val requestId get() = arguments?.getString(ARG_REQUEST_ID) ?: ""
    private val anzianoId get() = arguments?.getString(ARG_ANZIANO_ID) ?: ""
    private val volontarioId get() = arguments?.getString(ARG_VOLONTARIO_ID) ?: ""
    private val altroNome get() = arguments?.getString(ARG_ALTRO_NOME) ?: ""
    private val anzianoNome get() = arguments?.getString(ARG_ANZIANO_NOME) ?: ""
    private val volontarioNome get() = arguments?.getString(ARG_VOLONTARIO_NOME) ?: ""
    private val titolo get() = arguments?.getString(ARG_TITOLO)
    private val mostraAscolto get() = arguments?.getBoolean(ARG_MOSTRA_ASCOLTO, false) ?: false
    private val soloLettura get() = arguments?.getBoolean(ARG_SOLO_LETTURA, false) ?: false

    private val viewModel: ChatViewModel by viewModels {
        ChatViewModelFactory(
            MessageRepositoryImpl(), AuthRepositoryImpl(),
            requestId, anzianoId, volontarioId
        )
    }

    private lateinit var adapter: ChatAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DataBindingUtil.inflate(inflater, R.layout.fragment_chat, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // intestazione: nome e ruolo dell'interlocutore (o titolo, per il garante)
        val sonoGarante = viewModel.uidCorrente != anzianoId && viewModel.uidCorrente != volontarioId
        if (sonoGarante) {
            // vista garante: solo "Chat" (i nomi sono già etichettati in ogni bolla)
            binding.avatarInterlocutore.visibility = View.GONE
            binding.ruoloInterlocutore.visibility = View.GONE
            binding.nomeInterlocutore.text = "Chat"
        } else {
            binding.avatarInterlocutore.visibility = View.VISIBLE
            binding.ruoloInterlocutore.visibility = View.VISIBLE
            binding.nomeInterlocutore.text = altroNome
            binding.ruoloInterlocutore.text =
                if (viewModel.uidCorrente == anzianoId) "Volontario" else "Anziano"
        }

        // barra di invio nascosta quando la chat e' chiusa (storico) o quando
        // chi guarda e' il garante (sola lettura); al suo posto il promemoria
        binding.barraInvio.visibility = if (soloLettura) View.GONE else View.VISIBLE
        binding.readOnlyHint.visibility = if (soloLettura) View.VISIBLE else View.GONE

        // avviso di trasparenza: lo vedono i due partecipanti (anziano/volontario), non il garante
        binding.avvisoTrasparenza.visibility = if (sonoGarante) View.GONE else View.VISIBLE

        // TTS solo lato Anziano
        if (mostraAscolto) {
            tts = TtsHelper(requireContext())
        }

        adapter = ChatAdapter(
            uidCorrente = viewModel.uidCorrente,
            anzianoId = anzianoId,
            volontarioId = volontarioId,
            anzianoNome = anzianoNome,
            volontarioNome = volontarioNome,
            mostraNomi = sonoGarante,
            mostraAscolto = mostraAscolto,
            onAscolta = { testo -> tts?.parla(testo, svuotaCoda = true) }
        )

        // stackFromEnd: gli ultimi messaggi restano in basso, come in una chat
        binding.messaggiRecyclerView.layoutManager =
            LinearLayoutManager(requireContext()).apply { stackFromEnd = true }
        binding.messaggiRecyclerView.adapter = adapter

        // la chat ha un header proprio (la toolbar del ruolo è nascosta): freccia indietro
        binding.backButton.setOnClickListener { findNavController().navigateUp() }

        binding.inviaButton.setOnClickListener {
            val testo = binding.messaggioInput.text.toString()
            viewModel.inviaMessaggio(testo)
            binding.messaggioInput.text?.clear()
        }

        osservaMessaggi()
        osservaErrori()
    }

    private fun osservaMessaggi() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.messaggi.collect { lista ->
                    adapter.aggiornaLista(lista)
                    // in sola lettura non ha senso invitare a scrivere
                    binding.emptyStateText.text =
                        if (soloLettura) "Nessun messaggio" else "Nessun messaggio. Scrivi per iniziare."
                    binding.emptyStateText.visibility =
                        if (lista.isEmpty()) View.VISIBLE else View.GONE
                    // porta in vista l'ultimo messaggio quando ne arriva uno nuovo
                    if (lista.isNotEmpty()) {
                        binding.messaggiRecyclerView.post {
                            binding.messaggiRecyclerView.scrollToPosition(lista.size - 1)
                        }
                    }
                }
            }
        }
    }

    private fun osservaErrori() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.errore.collect { errore ->
                    if (errore != null) {
                        Toast.makeText(requireContext(), errore, Toast.LENGTH_SHORT).show()
                        viewModel.erroreMostrato()
                    }
                }
            }
        }
    }

    // interrompe la voce quando la schermata va in background (coerente con T2)
    override fun onStop() {
        super.onStop()
        tts?.interrompi()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        tts?.chiudi()
        tts = null
        _binding = null
    }

    companion object {
        const val ARG_REQUEST_ID = "requestId"
        const val ARG_ANZIANO_ID = "anzianoId"
        const val ARG_VOLONTARIO_ID = "volontarioId"
        const val ARG_ALTRO_NOME = "altroNome"
        const val ARG_ANZIANO_NOME = "anzianoNome"
        const val ARG_VOLONTARIO_NOME = "volontarioNome"
        const val ARG_TITOLO = "titolo"
        const val ARG_MOSTRA_ASCOLTO = "mostraAscolto"
        const val ARG_SOLO_LETTURA = "soloLettura"
    }
}
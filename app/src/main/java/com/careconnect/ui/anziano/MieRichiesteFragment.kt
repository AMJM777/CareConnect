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
import androidx.recyclerview.widget.LinearLayoutManager
import com.careconnect.R
import com.careconnect.databinding.FragmentMieRichiesteBinding
import com.careconnect.repository.AuthRepositoryImpl
import com.careconnect.repository.RequestRepositoryImpl
import com.careconnect.viewmodel.anziano.MieRichiesteViewModel
import com.careconnect.viewmodel.anziano.MieRichiesteViewModelFactory
import kotlinx.coroutines.launch
import androidx.navigation.fragment.findNavController
import com.careconnect.ui.common.mostraProfiloVolontario
import com.careconnect.model.RequestStatus

// schermata "Le mie richieste", è una lista in tempo reale delle richieste con le azioni "Modifica"
// (solo se APERTA) e "Annulla" (APERTA o PRESA_IN_CARICO)
class MieRichiesteFragment : Fragment() {

    private var _binding: FragmentMieRichiesteBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MieRichiesteViewModel by viewModels {
        MieRichiesteViewModelFactory(RequestRepositoryImpl(), AuthRepositoryImpl())
    }

    // tre lambda per l'Adapter: modifica, annulla, apri profilo volontario

    private val adapter = RichiesteAdapter(
        onModificaClick = { richiesta ->
            val argomenti = androidx.core.os.bundleOf(
                NuovaRichiestaFragment.ARG_REQUEST_ID to richiesta.id,
                NuovaRichiestaFragment.ARG_TIPO to richiesta.tipo,
                NuovaRichiestaFragment.ARG_DESCRIZIONE to richiesta.descrizione
            )
            findNavController().navigate(R.id.nuovaRichiestaFragment, argomenti)
        },
        onAnnullaClick = { richiesta ->
            mostraConfermaAnnullamento(richiesta.id, richiesta.stato)
        },
        onVolontarioClick = { volontarioId -> mostraProfiloVolontario(volontarioId) },
        onChatClick = { richiesta -> apriChat(richiesta) }
    )
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_mie_richieste, container, false
        )
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.richiesteRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.richiesteRecyclerView.adapter = adapter
        binding.richiesteRecyclerView.itemAnimator = null

        osservaRichieste()
        osservaErroriAnnullamento()
    }

    // chiede conferma prima di annullare/eliminare
    // Se è ancora APERTA nessun volontario l'ha mai vista: si elimina
    // davvero (Delete). Se è già PRESA_IN_CARICO si preserva lo storico
    // passando ad ANNULLATA (Update)
    private fun mostraConfermaAnnullamento(requestId: String, stato: RequestStatus) {
        val eliminaDavvero = stato == RequestStatus.APERTA
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(if (eliminaDavvero) "Eliminare la richiesta?" else "Annullare la richiesta?")
            .setMessage(
                if (eliminaDavvero) "Questa azione non si può annullare: la richiesta verrà eliminata definitivamente."
                else "Questa azione non si può annullare."
            )
            .setPositiveButton(if (eliminaDavvero) "Sì, elimina" else "Sì, annulla") { _, _ ->
                if (eliminaDavvero) {
                    viewModel.eliminaRichiesta(requestId)
                } else {
                    viewModel.annullaRichiesta(requestId)
                }
            }
            .setNegativeButton("No", null)
            .show()
    }

    // apre la chat con il volontario della richiesta. Lato anziano è disponibile la lettura
    // vocale. Se la richiesta non è più presa in carico, la chat è in sola lettura
    private fun apriChat(richiesta: com.careconnect.model.Request) {
        val soloLettura = richiesta.stato != RequestStatus.PRESA_IN_CARICO
        val argomenti = androidx.core.os.bundleOf(
            com.careconnect.ui.chat.ChatFragment.ARG_REQUEST_ID to richiesta.id,
            com.careconnect.ui.chat.ChatFragment.ARG_ANZIANO_ID to richiesta.autoreId,
            com.careconnect.ui.chat.ChatFragment.ARG_VOLONTARIO_ID to (richiesta.volontarioId ?: ""),
            com.careconnect.ui.chat.ChatFragment.ARG_ALTRO_NOME to (richiesta.volontarioNome ?: ""),
            com.careconnect.ui.chat.ChatFragment.ARG_MOSTRA_ASCOLTO to true,
            com.careconnect.ui.chat.ChatFragment.ARG_SOLO_LETTURA to soloLettura
        )
        findNavController().navigate(R.id.chatFragment, argomenti)
    }

    // funzione per osservare la lista di richieste e aggiornare la RecyclerView
    private fun osservaRichieste() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.richieste.collect { lista ->
                    adapter.aggiornaLista(lista)
                    binding.emptyStateText.visibility =
                        if (lista.isEmpty()) View.VISIBLE else View.GONE
                    binding.richiesteRecyclerView.visibility =
                        if (lista.isEmpty()) View.GONE else View.VISIBLE
                }
            }
        }
    }

    // funzione per osservare eventuali errori durante l'annullamento e mostrarli con un toast
    private fun osservaErroriAnnullamento() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.erroreAnnullamento.collect { errore ->
                    if (errore != null) {
                        Toast.makeText(requireContext(), errore, Toast.LENGTH_SHORT).show()
                        viewModel.erroreMostrato()
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
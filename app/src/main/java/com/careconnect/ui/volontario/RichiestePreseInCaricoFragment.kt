package com.careconnect.ui.volontario

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.careconnect.R
import com.careconnect.databinding.FragmentRichiestePreseInCaricoBinding
import com.careconnect.repository.AuthRepositoryImpl
import com.careconnect.repository.RequestRepositoryImpl
import com.careconnect.viewmodel.volontario.RichiestePreseInCaricoViewModel
import com.careconnect.viewmodel.volontario.RichiestePreseInCaricoViewModelFactory
import kotlinx.coroutines.launch

// schermata "Le mie richieste prese in carico": lista in tempo reale delle
// richieste attive del volontario, con azioni "Segna come completata" e
// "Rilascia" (quest'ultima con conferma, è un'azione che toglie la
// richiesta al volontario stesso).
class RichiestePreseInCaricoFragment : Fragment() {

    private var _binding: FragmentRichiestePreseInCaricoBinding? = null
    private val binding get() = _binding!!

    private val viewModel: RichiestePreseInCaricoViewModel by viewModels {
        RichiestePreseInCaricoViewModelFactory(RequestRepositoryImpl(), AuthRepositoryImpl())
    }

    private val adapter = RichiestePreseInCaricoAdapter(
        onCompletaClick = { richiesta ->
            viewModel.segnaCompletata(richiesta.id)
        },
        onRilasciaClick = { richiesta ->
            mostraConfermaRilascio(richiesta.id)
        },
        onChatClick = { richiesta ->
            apriChat(richiesta)
        }
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_richieste_prese_in_carico, container, false
        )
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.richiesteRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.richiesteRecyclerView.adapter = adapter
        // vedi commento in RichiesteDisponibiliFragment: query senza orderBy,
        // disabilitiamo l'item animator per evitare righe che appaiono vuote
        // durante il riordino tra uno snapshot e l'altro.
        binding.richiesteRecyclerView.itemAnimator = null

        osservaRichieste()
        osservaErrori()
    }

    // chiede conferma prima di rilasciare: rimette la richiesta a disposizione
    // di tutti, un tocco accidentale non deve far perdere l'incarico.
    private fun mostraConfermaRilascio(requestId: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Rilasciare la richiesta?")
            .setMessage("Tornerà visibile a tutti i volontari.")
            .setPositiveButton("Sì, rilascia") { _, _ ->
                viewModel.rilasciaRichiesta(requestId)
            }
            .setNegativeButton("No", null)
            .show()
    }

    // apre la chat sulla richiesta. Se non è più presa in carico la chat
    // è in sola lettura (storico), coerente con le security rules.
    private fun apriChat(richiesta: com.careconnect.model.Request) {
        val soloLettura = richiesta.stato != com.careconnect.model.RequestStatus.PRESA_IN_CARICO
        val argomenti = androidx.core.os.bundleOf(
            com.careconnect.ui.chat.ChatFragment.ARG_REQUEST_ID to richiesta.id,
            com.careconnect.ui.chat.ChatFragment.ARG_ANZIANO_ID to richiesta.autoreId,
            com.careconnect.ui.chat.ChatFragment.ARG_VOLONTARIO_ID to (richiesta.volontarioId ?: ""),
            com.careconnect.ui.chat.ChatFragment.ARG_ALTRO_NOME to richiesta.autoreNome,
            com.careconnect.ui.chat.ChatFragment.ARG_MOSTRA_ASCOLTO to false,
            com.careconnect.ui.chat.ChatFragment.ARG_SOLO_LETTURA to soloLettura
        )
        findNavController().navigate(R.id.chatFragment, argomenti)
    }

    // funzione per osservare la lista di richieste esposta dal ViewModel e aggiornare la RecyclerView.
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

    // funzione per osservare eventuali errori esposti dal ViewModel e mostrarli con un Toast.
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
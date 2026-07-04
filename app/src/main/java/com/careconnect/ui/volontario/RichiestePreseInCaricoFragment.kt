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
import androidx.recyclerview.widget.LinearLayoutManager
import com.careconnect.R
import com.careconnect.databinding.FragmentRichiestePreseInCaricoBinding
import com.careconnect.repository.AuthRepositoryImpl
import com.careconnect.repository.RequestRepositoryImpl
import com.careconnect.viewmodel.volontario.RichiestePreseInCaricoViewModel
import com.careconnect.viewmodel.volontario.RichiestePreseInCaricoViewModelFactory
import kotlinx.coroutines.launch

/**
 * Schermata "Le mie richieste prese in carico": lista in tempo reale delle
 * richieste attive del volontario, con azioni "Segna come completata" e
 * "Rilascia" (quest'ultima con conferma, è un'azione che toglie la
 * richiesta al volontario stesso).
 */
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

        osservaRichieste()
        osservaErrori()
    }

    /** Conferma prima di rilasciare: rimette la richiesta a disposizione di
     *  tutti, un tocco accidentale non deve far perdere l'incarico. */
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
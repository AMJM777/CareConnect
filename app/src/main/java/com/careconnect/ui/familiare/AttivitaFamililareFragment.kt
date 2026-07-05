package com.careconnect.ui.familiare

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.RatingBar
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
import com.careconnect.databinding.FragmentAttivitaFamiliareBinding
import com.careconnect.model.Request
import com.careconnect.repository.AuthRepositoryImpl
import com.careconnect.repository.RatingRepositoryImpl
import com.careconnect.repository.RequestRepositoryImpl
import com.careconnect.repository.SosRepositoryImpl
import com.careconnect.repository.UserRepositoryImpl
import com.careconnect.ui.common.mostraProfiloVolontario
import com.careconnect.viewmodel.familiare.AttivitaFamiliareViewModel
import com.careconnect.viewmodel.familiare.AttivitaFamiliareViewModelFactory
import kotlinx.coroutines.launch

class AttivitaFamiliareFragment : Fragment() {

    private var _binding: FragmentAttivitaFamiliareBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AttivitaFamiliareViewModel by viewModels {
        AttivitaFamiliareViewModelFactory(
            RequestRepositoryImpl(),
            RatingRepositoryImpl(),
            SosRepositoryImpl(),
            UserRepositoryImpl(),
            AuthRepositoryImpl()
        )
    }

    // FASE 7: onVolontarioClick apre il profilo di sola lettura del
    // volontario (dialog condiviso in ui/common/). Mancava in questo file
    // dopo l'ultima modifica per il banner SOS — corretto qui.
    private val adapter = AttivitaFamiliareAdapter(
        onConfermaClick = { richiesta -> mostraDialogValutazione(richiesta) },
        onVolontarioClick = { volontarioId -> mostraProfiloVolontario(volontarioId) }
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DataBindingUtil.inflate(inflater, R.layout.fragment_attivita_familiare, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.richiesteRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.richiesteRecyclerView.adapter = adapter

        osservaRichieste()
        osservaErrori()
        osservaSos()
    }

    private fun osservaSos() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.sosAttivo.collect { alert ->
                    binding.sosBanner.visibility = if (alert != null) View.VISIBLE else View.GONE
                    binding.chiudiSosButton.setOnClickListener {
                        alert?.let { viewModel.chiudiSos(it.id) }
                    }
                }
            }
        }
    }

    private fun mostraDialogValutazione(richiesta: Request) {
        val vistaDialog = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_conferma_valutazione, null)
        val ratingBar = vistaDialog.findViewById<RatingBar>(R.id.ratingBar)
        val commentoEditText = vistaDialog.findViewById<EditText>(R.id.commentoEditText)

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Conferma completamento")
            .setView(vistaDialog)
            .setPositiveButton("Conferma", null)
            .setNegativeButton("Annulla", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val stelle = ratingBar.rating.toInt()
                if (stelle == 0) {
                    Toast.makeText(requireContext(), "Seleziona almeno una stella", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                viewModel.confermaEValuta(richiesta, stelle, commentoEditText.text.toString())
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun osservaRichieste() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.richieste.collect { lista ->
                    adapter.aggiornaLista(lista)
                    binding.emptyStateText.visibility = if (lista.isEmpty()) View.VISIBLE else View.GONE
                    binding.richiesteRecyclerView.visibility = if (lista.isEmpty()) View.GONE else View.VISIBLE
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
package com.careconnect.ui.familiare

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.RatingBar
import android.widget.TextView
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
import com.careconnect.databinding.FragmentAttivitaFamiliareBinding
import com.careconnect.model.Request
import com.careconnect.model.User
import com.google.android.material.chip.Chip
import com.careconnect.repository.AuthRepositoryImpl
import com.careconnect.repository.RatingRepositoryImpl
import com.careconnect.repository.RequestRepositoryImpl
import com.careconnect.repository.SosRepositoryImpl
import com.careconnect.repository.UserRepositoryImpl
import com.careconnect.ui.common.mostraProfiloVolontario
import com.careconnect.viewmodel.familiare.AttivitaFamiliareViewModel
import com.careconnect.viewmodel.familiare.AttivitaFamiliareViewModelFactory
import kotlinx.coroutines.launch

// schermata "Attività" del familiare: lista delle richieste dell'anziano
// seguito, dialog di conferma con valutazione, e banner SOS
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

    private val adapter = AttivitaFamiliareAdapter(
        onConfermaClick = { richiesta -> mostraDialogValutazione(richiesta) },
        onVolontarioClick = { volontarioId -> mostraProfiloVolontario(volontarioId) },
        onChatClick = { richiesta -> apriChat(richiesta) }
    )

    // apre la chat dell'assistito in sola lettura, il garante
    // legge la conversazione tra il suo assistito e il volontario, non scrive
    private fun apriChat(richiesta: Request) {
        val argomenti = androidx.core.os.bundleOf(
            com.careconnect.ui.chat.ChatFragment.ARG_REQUEST_ID to richiesta.id,
            com.careconnect.ui.chat.ChatFragment.ARG_ANZIANO_ID to richiesta.autoreId,
            com.careconnect.ui.chat.ChatFragment.ARG_VOLONTARIO_ID to (richiesta.volontarioId ?: ""),
            com.careconnect.ui.chat.ChatFragment.ARG_ANZIANO_NOME to richiesta.autoreNome,
            com.careconnect.ui.chat.ChatFragment.ARG_VOLONTARIO_NOME to (richiesta.volontarioNome ?: "Volontario"),
            com.careconnect.ui.chat.ChatFragment.ARG_TITOLO to
                "Chat: ${richiesta.autoreNome} · ${richiesta.volontarioNome ?: "Volontario"}",
            com.careconnect.ui.chat.ChatFragment.ARG_MOSTRA_ASCOLTO to false,
            com.careconnect.ui.chat.ChatFragment.ARG_SOLO_LETTURA to true
        )
        findNavController().navigate(R.id.chatFragment, argomenti)
    }

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
        // query senza orderBy, spengo l'item animator per evitare righe che
        // appaiono vuote durante il riordino tra uno snapshot e l'altro
        binding.richiesteRecyclerView.itemAnimator = null

        osservaAnziani()
        osservaRichieste()
        osservaErrori()
        osservaSos()
    }

    // costruisce il selettore degli anziani seguiti, visibile solo con più di uno
    private fun osservaAnziani() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.anzianiSeguiti.collect { anziani ->
                    popolaSelettore(anziani)
                }
            }
        }
    }

    private fun popolaSelettore(anziani: List<User>) {
        val gruppo = binding.anzianiChipGroup
        val mostraSelettore = if (anziani.size > 1) View.VISIBLE else View.GONE
        binding.selettoreTitolo.visibility = mostraSelettore
        binding.selettoreAnziani.visibility = mostraSelettore
        // evito di ricostruire, e resettare la scelta, se è già popolato
        if (gruppo.childCount == anziani.size) return
        gruppo.removeAllViews()
        val inflater = LayoutInflater.from(requireContext())
        anziani.forEachIndexed { index, anziano ->
            val chip = inflater.inflate(R.layout.item_chip_anziano, gruppo, false) as Chip
            chip.text = anziano.nome
            chip.id = View.generateViewId()
            chip.tag = anziano.uid
            if (index == 0) chip.isChecked = true
            gruppo.addView(chip)
        }
        gruppo.setOnCheckedStateChangeListener { group, checkedIds ->
            val id = checkedIds.firstOrNull() ?: return@setOnCheckedStateChangeListener
            val anzianoId = group.findViewById<Chip>(id)?.tag as? String ?: return@setOnCheckedStateChangeListener
            viewModel.selezionaAnziano(anzianoId)
        }
    }

    // funzione per osservare eventuali alert SOS attivi e mostrare/nascondere il banner
    private fun osservaSos() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.sosAttivo.collect { alert ->
                    binding.sosBanner.visibility = if (alert != null) View.VISIBLE else View.GONE
                    if (alert != null) {
                        // nome dell'anziano che ha lanciato l'allarme, se è tra quelli seguiti
                        val nome = viewModel.anzianiSeguiti.value
                            .firstOrNull { it.uid == alert.anzianoId }?.nome
                        binding.sosText.text = if (nome != null) {
                            "$nome ha lanciato un allarme SOS"
                        } else {
                            "Il tuo assistito ha lanciato un allarme SOS"
                        }
                    }
                    binding.chiudiSosButton.setOnClickListener {
                        alert?.let { viewModel.chiudiSos(it.id) }
                    }
                }
            }
        }
    }

    // funzione che mostra il dialog per confermare il completamento e assegnare la valutazione
    private fun mostraDialogValutazione(richiesta: Request) {
        val vistaDialog = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_conferma_valutazione, null)
        val ratingBar = vistaDialog.findViewById<RatingBar>(R.id.ratingBar)
        val commentoEditText = vistaDialog.findViewById<EditText>(R.id.commentoEditText)

        vistaDialog.findViewById<TextView>(R.id.valutaSottotitolo).text =
            "Valuta l'aiuto di ${richiesta.volontarioNome ?: "questo volontario"}"

        // dialog senza titolo/pulsanti di default: header e pulsanti sono nel layout
        val dialog = AlertDialog.Builder(requireContext())
            .setView(vistaDialog)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        vistaDialog.findViewById<View>(R.id.confermaButton).setOnClickListener {
            val stelle = ratingBar.rating.toInt()
            if (stelle == 0) {
                Toast.makeText(requireContext(), "Seleziona almeno una stella", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            viewModel.confermaEValuta(richiesta, stelle, commentoEditText.text.toString())
            dialog.dismiss()
        }
        vistaDialog.findViewById<View>(R.id.annullaButton).setOnClickListener { dialog.dismiss() }

        dialog.show()
    }

    // funzione per osservare la lista di richieste e aggiornare la RecyclerView
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

    // funzione per osservare eventuali errori esposti dal ViewModel e mostrarli con un Toas
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
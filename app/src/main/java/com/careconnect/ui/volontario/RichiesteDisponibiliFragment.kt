package com.careconnect.ui.volontario

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
import com.careconnect.databinding.FragmentRichiesteDisponibiliBinding
import com.careconnect.repository.AuthRepositoryImpl
import com.careconnect.repository.RequestRepositoryImpl
import com.careconnect.viewmodel.volontario.RichiesteDisponibiliViewModel
import com.careconnect.viewmodel.volontario.RichiesteDisponibiliViewModelFactory
import kotlinx.coroutines.launch
import com.careconnect.repository.UserRepositoryImpl

// schermata "Richieste disponibili": lista in tempo reale di tutte le
// richieste APERTA, con azione "Prendi in carico" su ciascuna riga.
class RichiesteDisponibiliFragment : Fragment() {

    private var _binding: FragmentRichiesteDisponibiliBinding? = null
    private val binding get() = _binding!!

    private val viewModel: RichiesteDisponibiliViewModel by viewModels {
        RichiesteDisponibiliViewModelFactory(RequestRepositoryImpl(), UserRepositoryImpl(), AuthRepositoryImpl())
    }
    private val adapter = RichiesteDisponibiliAdapter(
        onPrendiInCaricoClick = { richiesta ->
            viewModel.prendiInCarico(richiesta.id)
        }
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_richieste_disponibili, container, false
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

    // funzione per osservare eventuali errori nella presa in carico e mostrarli con un Toast.
    private fun osservaErrori() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.errorePresaInCarico.collect { errore ->
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
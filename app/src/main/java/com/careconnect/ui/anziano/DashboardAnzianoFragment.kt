package com.careconnect.ui.anziano

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
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
import com.careconnect.R
import com.careconnect.databinding.FragmentDashboardAnzianoBinding
import com.careconnect.repository.AuthRepositoryImpl
import com.careconnect.repository.SosRepositoryImpl
import com.careconnect.repository.UserRepositoryImpl
import com.careconnect.viewmodel.anziano.DashboardAnzianoViewModel
import com.careconnect.viewmodel.anziano.DashboardAnzianoViewModelFactory
import kotlinx.coroutines.launch

// home dell'Anziano: solo due pulsanti, "Nuova richiesta" e "SOS"
class DashboardAnzianoFragment : Fragment() {

    private var _binding: FragmentDashboardAnzianoBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DashboardAnzianoViewModel by viewModels {
        DashboardAnzianoViewModelFactory(SosRepositoryImpl(), UserRepositoryImpl(), AuthRepositoryImpl())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_dashboard_anziano, container, false
        )
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.nuovaRichiestaButton.setOnClickListener {
            findNavController().navigate(R.id.nuovaRichiestaFragment)
        }
        binding.sosButton.setOnClickListener { mostraConfermaSos() }

        osservaErrori()
        osservaSosInviato()
    }

    // chiede conferma prima di inviare: un tap accidentale non deve scattare l'SOS.
    private fun mostraConfermaSos() {
        AlertDialog.Builder(requireContext())
            .setTitle("Contattare i soccorsi?")
            .setMessage("Si aprirà la chiamata al 112 e i tuoi familiari saranno avvisati subito.")
            .setPositiveButton("Sì, SOS") { _, _ -> avviaSos() }
            .setNegativeButton("Annulla", null)
            .show()
    }

    // funzione che avvisa i familiari e apre il compositore telefonico verso il 112
    private fun avviaSos() {
        //avvisa i familiari subito, senza aspettare la chiamata
        viewModel.inviaSos()

        // ACTION_DIAL apre il compositore, l'utente conferma la chiamata:
        // nessun permesso runtime richiesto
        try {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:112"))
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            // caso limite (es. alcuni emulatori senza app telefono): non
            // fa crashare l'app, i familiari sono comunque avvisati
            Toast.makeText(requireContext(), "Nessuna app per chiamare trovata sul dispositivo", Toast.LENGTH_LONG).show()
        }
    }

    // funzione per osservare eventuali errori esposti dal ViewModel e mostrarli con un Toast
    private fun osservaErrori() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.errore.collect { errore ->
                    if (errore != null) {
                        Toast.makeText(requireContext(), errore, Toast.LENGTH_LONG).show()
                        viewModel.erroreMostrato()
                    }
                }
            }
        }
    }

    // funzione per osservare la conferma di invio SOS e mostrare un Toast
    private fun osservaSosInviato() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.sosInviato.collect { inviato ->
                    if (inviato) {
                        Toast.makeText(requireContext(), "Familiari avvisati", Toast.LENGTH_SHORT).show()
                        viewModel.sosInviatoMostrato()
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
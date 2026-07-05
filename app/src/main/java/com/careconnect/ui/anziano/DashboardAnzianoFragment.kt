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

/**
 * Home dell'Anziano (FASE 8, ridisegnata): solo 2 pulsanti — "Nuova
 * richiesta" (naviga al Fragment esistente, invariato) e "SOS" (avvisa i
 * familiari collegati e apre il compositore verso il 112).
 */
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

    /**
     * Conferma prima di inviare: un tap accidentale non deve avvisare i
     * familiari né aprire il compositore verso il 112. Costa un secondo
     * in una vera emergenza, ma evita falsi allarmi — stesso principio già
     * usato per logout/annulla/rilascia in questo progetto.
     */
    private fun mostraConfermaSos() {
        AlertDialog.Builder(requireContext())
            .setTitle("Contattare i soccorsi?")
            .setMessage("Si aprirà la chiamata al 112 e i tuoi familiari saranno avvisati subito.")
            .setPositiveButton("Sì, SOS") { _, _ -> avviaSos() }
            .setNegativeButton("Annulla", null)
            .show()
    }

    private fun avviaSos() {
        // Avvisiamo i familiari SUBITO, senza aspettare che l'utente chiuda
        // il compositore: in un'emergenza, prima lo sanno, meglio è.
        viewModel.inviaSos()

        // ACTION_DIAL, non ACTION_CALL: apre il compositore, l'utente
        // conferma la chiamata. Nessun permesso runtime richiesto.
        try {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:112"))
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            // Caso limite (es. alcuni emulatori senza app telefono): non
            // deve far crashare l'app, i familiari sono comunque avvisati.
            Toast.makeText(requireContext(), "Nessuna app per chiamare trovata sul dispositivo", Toast.LENGTH_LONG).show()
        }
    }

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
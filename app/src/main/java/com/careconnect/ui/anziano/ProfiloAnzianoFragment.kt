package com.careconnect.ui.anziano

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.navOptions
import com.careconnect.R
import com.careconnect.databinding.FragmentProfiloAnzianoBinding
import com.careconnect.repository.AuthRepositoryImpl
import com.careconnect.repository.UserRepositoryImpl
import com.careconnect.sos.SosShakeService
import com.careconnect.util.OverlaySosPermesso
import com.careconnect.util.ProtezioneSosPrefs
import com.careconnect.util.SessionCache
import com.careconnect.viewmodel.anziano.ProfiloAnzianoViewModel
import com.careconnect.viewmodel.anziano.ProfiloAnzianoViewModelFactory
import com.careconnect.viewmodel.auth.AuthViewModel
import com.careconnect.viewmodel.auth.AuthViewModelFactory
import kotlinx.coroutines.launch
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding

// profilo dell'Anziano: nome/email/ruolo, codice invito, indirizzo, logout.
// nome/email/codice invito sono legati dall'XML con data binding; l'indirizzo
// è editabile e resta gestito a mano
class ProfiloAnzianoFragment : Fragment() {

    private var _binding: FragmentProfiloAnzianoBinding? = null
    private val binding get() = _binding!!

    private val authViewModel: AuthViewModel by activityViewModels {
        AuthViewModelFactory(
            AuthRepositoryImpl(),
            UserRepositoryImpl(),
            SessionCache(requireContext())
        )
    }

    private val viewModel: ProfiloAnzianoViewModel by viewModels {
        ProfiloAnzianoViewModelFactory(UserRepositoryImpl(), AuthRepositoryImpl())
    }

    // preferenza opt-out della protezione SOS in background (T4)
    private val protezionePrefs by lazy { ProtezioneSosPrefs(requireContext()) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DataBindingUtil.inflate(inflater, R.layout.fragment_profilo_anziano, container, false)
        // collega il ViewModel al layout: da qui le View legate a LiveData si aggiornano da sole
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // padding in basso pari all'altezza della tastiera, così il campo
        // indirizzo può salire sopra di essa.
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val tastiera = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            v.updatePadding(bottom = tastiera)
            insets
        }

        binding.logoutButton.setOnClickListener { mostraConfermaLogout() }
        binding.copiaCodiceButton.setOnClickListener { copiaCodiceNegliAppunti() }
        binding.salvaIndirizzoButton.setOnClickListener {
            viewModel.salvaIndirizzo(binding.indirizzoEditText.text.toString())
        }

        preRiempiIndirizzo()
        osservaErrori()
        osservaIndirizzoSalvato()
        osservaGaranti()
        configuraProtezioneSwitch()
    }

    // mostra i familiari collegati: una riga per nome, o un messaggio se la lista è vuota
    private fun osservaGaranti() {
        viewModel.garanti.observe(viewLifecycleOwner) { nomi ->
            val container = binding.garantiContainer
            container.removeAllViews()
            binding.garantiVuotoText.visibility = if (nomi.isEmpty()) View.VISIBLE else View.GONE

            val inflater = LayoutInflater.from(requireContext())
            nomi.forEach { nome ->
                val riga = inflater.inflate(R.layout.item_garante_collegato, container, false)
                riga.findViewById<TextView>(R.id.garanteNomeText).text = nome
                container.addView(riga)
            }
        }
    }

    // interruttore della protezione SOS in background (T4): stato iniziale dalla
    // preferenza (default acceso) e, a ogni cambio, avvia/ferma il Foreground Service.
    private fun configuraProtezioneSwitch() {
        binding.protezioneSosSwitch.isChecked = protezionePrefs.isAttiva()
        binding.protezioneSosSwitch.setOnCheckedChangeListener { _, attiva ->
            protezionePrefs.setAttiva(attiva)
            if (attiva) {
                SosShakeService.avvia(requireContext())
                Toast.makeText(
                    requireContext(),
                    "Protezione attiva: scuoti il telefono per chiedere aiuto",
                    Toast.LENGTH_SHORT
                ).show()
                chiediPermessoFullScreenSeManca()
            } else {
                SosShakeService.ferma(requireContext())
                Toast.makeText(requireContext(), "Protezione disattivata", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Se manca il permesso "Compari sopra le altre app", porta l'utente alle
    // impostazioni per concederlo. Con il permesso, lo scuotimento apre subito
    // l'overlay in ogni situazione, senza una notifica intermedia da toccare.
    private fun chiediPermessoFullScreenSeManca() {
        if (OverlaySosPermesso.concesso(requireContext())) return

        AlertDialog.Builder(requireContext())
            .setTitle("Attiva l'apertura automatica")
            .setMessage(
                "Per far comparire subito la richiesta di aiuto quando scuoti il " +
                    "telefono — anche fuori dall'app o a schermo bloccato — concedi a " +
                    "CareConnect il permesso \"Compari sopra le altre app\"."
            )
            .setPositiveButton("Vai alle impostazioni") { _, _ ->
                try {
                    startActivity(OverlaySosPermesso.intentImpostazioni(requireContext()))
                } catch (e: android.content.ActivityNotFoundException) {
                    // alcuni dispositivi non hanno questa schermata: si ignora
                }
            }
            .setNegativeButton("Più tardi", null)
            .show()
    }

    // l'indirizzo è un campo editabile: viene scritto nell'EditText una sola volta, al caricamento
    private fun preRiempiIndirizzo() {
        viewModel.indirizzoIniziale.observe(viewLifecycleOwner) { indirizzo ->
            if (binding.indirizzoEditText.text.isNullOrBlank()) {
                binding.indirizzoEditText.setText(indirizzo)
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

    // funzione per osservare la conferma di salvataggio dell'indirizzo e mostrare un Toast.
    private fun osservaIndirizzoSalvato() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.indirizzoSalvato.collect { salvato ->
                    if (salvato) {
                        Toast.makeText(requireContext(), "Indirizzo salvato", Toast.LENGTH_SHORT).show()
                        viewModel.indirizzoSalvatoMostrato()
                    }
                }
            }
        }
    }

    // funzione per copiare il codice invito negli appunti del dispositivo
    private fun copiaCodiceNegliAppunti() {
        val codice = viewModel.codicePerCopia() ?: return
        val clipboardManager =
            requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboardManager.setPrimaryClip(ClipData.newPlainText("Codice invito CareConnect", codice))
        Toast.makeText(requireContext(), "Codice copiato", Toast.LENGTH_SHORT).show()
    }

    private fun mostraConfermaLogout() {
        AlertDialog.Builder(requireContext())
            .setTitle("Vuoi uscire?")
            .setMessage("Dovrai effettuare di nuovo l'accesso per tornare in CareConnect.")
            .setPositiveButton("Sì, esci") { _, _ -> eseguiLogout() }
            .setNegativeButton("Annulla", null)
            .show()
    }
    // funzione che esegue il logout e riporta l'utente al flusso di autenticazione
    private fun eseguiLogout() {
        // uscendo dall'account si ferma anche la protezione SOS in background:
        // non ha senso tenere attivo il sensore per un utente non più loggato.
        SosShakeService.ferma(requireContext())

        // passa dal condiviso AuthViewModel: resetta anche sessionCache e AuthUiState.
        authViewModel.logout()

        val navHostFragmentPrincipale = requireActivity().supportFragmentManager
            .findFragmentById(R.id.navHostFragment) as NavHostFragment
        val navControllerPrincipale = navHostFragmentPrincipale.navController
        // svuota lo stack fino alla radice e riparte dal logi
        val opzioni = navOptions {
            popUpTo(navControllerPrincipale.graph.id) { inclusive = true }
        }
        navControllerPrincipale.navigate(R.id.nav_graph_auth, null, opzioni)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
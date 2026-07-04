package com.careconnect.ui.anziano

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.databinding.DataBindingUtil
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.navOptions
import com.careconnect.R
import com.careconnect.databinding.FragmentDashboardAnzianoBinding
import com.careconnect.repository.AuthRepositoryImpl
import com.careconnect.repository.UserRepositoryImpl
import com.careconnect.util.SessionCache
import com.careconnect.viewmodel.auth.AuthViewModel
import com.careconnect.viewmodel.auth.AuthViewModelFactory

/**
 * Vera "home" dell'Anziano, dentro il grafo annidato. Per ora mostra solo
 * il benvenuto e il bottone di logout: il resto (SOS, riepilogo richieste)
 * arriva in un passaggio successivo.
 */
class DashboardAnzianoFragment : Fragment() {

    private var _binding: FragmentDashboardAnzianoBinding? = null
    private val binding get() = _binding!!

    // Stesso AuthViewModel condiviso da Login/Registrazione: activityViewModels
    // lo scopa all'Activity, quindi qui otteniamo la stessa istanza (o ne
    // viene creata una nuova con gli stessi repository, se non esisteva già).
    private val authViewModel: AuthViewModel by activityViewModels {
        AuthViewModelFactory(
            AuthRepositoryImpl(),
            UserRepositoryImpl(),
            SessionCache(requireContext())
        )
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

        binding.logoutButton.setOnClickListener { mostraConfermaLogout() }
    }

    /** Conferma prima di uscire: un tocco accidentale non deve disconnettere l'utente. */
    private fun mostraConfermaLogout() {
        AlertDialog.Builder(requireContext())
            .setTitle("Vuoi uscire?")
            .setMessage("Dovrai effettuare di nuovo l'accesso per tornare in CareConnect.")
            .setPositiveButton("Sì, esci") { _, _ -> eseguiLogout() }
            .setNegativeButton("Annulla", null)
            .show()
    }

    private fun eseguiLogout() {
        authViewModel.logout()

        // Serve il NavController PRINCIPALE (quello di activity_main.xml,
        // scoped a nav_graph_main), non quello annidato dell'Anziano: solo
        // lui conosce sia homeAnzianoFragment sia nav_graph_auth, per poter
        // ripulire l'intero back stack della sezione Anziano.
        // Stesso pattern già usato in SplashFragment per lo stesso scenario
        // (nessuna sessione -> vai al login).
        val navHostFragmentPrincipale = requireActivity().supportFragmentManager
            .findFragmentById(R.id.navHostFragment) as NavHostFragment
        val navControllerPrincipale = navHostFragmentPrincipale.navController

        val opzioni = navOptions {
            popUpTo(R.id.homeAnzianoFragment) { inclusive = true }
        }
        navControllerPrincipale.navigate(R.id.nav_graph_auth, null, opzioni)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
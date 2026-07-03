package com.careconnect.ui.anziano

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.careconnect.R

/**
 * Home dell'Anziano. Contenuto ancora minimo: il bottone di test verrà
 * sostituito dalla BottomNavigation nel prossimo task della Fase 4.
 */
class HomeAnzianoFragment : Fragment(R.layout.fragment_home_anziano) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<View>(R.id.nuovaRichiestaButtonTemp).setOnClickListener {
            findNavController().navigate(R.id.action_homeAnziano_to_nuovaRichiesta)
        }
    }
}
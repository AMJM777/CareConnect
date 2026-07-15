package com.careconnect.ui.common

import android.view.LayoutInflater
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.careconnect.R
import com.careconnect.repository.UserRepositoryImpl
import kotlinx.coroutines.launch

// funzione di estensione su Fragment: mostra un dialog di sola lettura con
// nome, descrizione e valutazione di un volontario.
// condivisa tra anziano e faamiliare
fun Fragment.mostraProfiloVolontario(volontarioId: String) {
    viewLifecycleOwner.lifecycleScope.launch {
        val volontario = UserRepositoryImpl().getUtente(volontarioId).getOrNull() ?: return@launch

        val vistaDialog = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_profilo_volontario, null)

        vistaDialog.findViewById<TextView>(R.id.profiloNomeText).text = volontario.nome
        vistaDialog.findViewById<TextView>(R.id.profiloRatingText).text =
            volontario.ratingMedio?.let { media -> "Valutazione: %.1f / 5".format(media) }
                ?: "Valutazione: non ancora valutato"
        vistaDialog.findViewById<TextView>(R.id.profiloBioText).text =
            volontario.bio?.takeIf { it.isNotBlank() } ?: "Nessuna descrizione"

        AlertDialog.Builder(requireContext())
            .setTitle("Profilo volontario")
            .setView(vistaDialog)
            .setPositiveButton("Chiudi", null)
            .show()
    }
}
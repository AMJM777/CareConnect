package com.careconnect.ui.common

import android.view.LayoutInflater
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.careconnect.R
import com.careconnect.repository.UserRepositoryImpl
import kotlinx.coroutines.launch
import android.view.View
import android.widget.RatingBar

// funzione di estensione su Fragment: mostra un dialog di sola lettura con
// nome, descrizione e valutazione di un volontario.
// condivisa tra anziano e faamiliare
fun Fragment.mostraProfiloVolontario(volontarioId: String) {
    viewLifecycleOwner.lifecycleScope.launch {
        val volontario = UserRepositoryImpl().getUtente(volontarioId).getOrNull() ?: return@launch

        val vistaDialog = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_profilo_volontario, null)

        val media = volontario.ratingMedio
        vistaDialog.findViewById<TextView>(R.id.profiloNomeText).text = volontario.nome

        // con un voto restano solo le stelle; senza, il testo "non ancora valutato"
        val ratingText = vistaDialog.findViewById<TextView>(R.id.profiloRatingText)
        val ratingBar = vistaDialog.findViewById<RatingBar>(R.id.profiloRatingBar)
        if (media != null) {
            ratingBar.rating = media.toFloat()
            ratingBar.contentDescription = "Valutazione %.1f su 5".format(media).replace('.', ',')
            ratingBar.visibility = View.VISIBLE
            ratingText.visibility = View.GONE
        } else {
            ratingBar.visibility = View.GONE
            ratingText.text = "Non ancora valutato"
            ratingText.visibility = View.VISIBLE
        }

        vistaDialog.findViewById<TextView>(R.id.profiloBioText).text =
            volontario.bio?.takeIf { it.isNotBlank() } ?: "Nessuna descrizione"

        AlertDialog.Builder(requireContext())
            .setTitle("Profilo volontario")
            .setView(vistaDialog)
            .setPositiveButton("Chiudi", null)
            .show()
    }
}
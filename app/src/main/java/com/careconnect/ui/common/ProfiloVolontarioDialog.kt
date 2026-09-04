package com.careconnect.ui.common

import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.RatingBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.careconnect.R
import com.careconnect.repository.RatingRepositoryImpl
import com.careconnect.repository.RequestRepositoryImpl
import com.careconnect.repository.UserRepositoryImpl
import com.careconnect.util.RecensioneFormat
import kotlinx.coroutines.launch

// funzione di estensione sul Fragmentì che mostra un dialog di sola lettura
// con le info del volontario affichè possano essere lette da anziano e familaire
fun Fragment.mostraProfiloVolontario(volontarioId: String) {
    viewLifecycleOwner.lifecycleScope.launch {
        val volontario = UserRepositoryImpl().getUtente(volontarioId).getOrNull() ?: return@launch

        val vistaDialog = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_profilo_volontario, null)

        vistaDialog.findViewById<TextView>(R.id.profiloNomeText).text = volontario.nome
        vistaDialog.findViewById<TextView>(R.id.profiloBioText).text =
            volontario.bio?.takeIf { it.isNotBlank() } ?: "Nessuna descrizione"

        // tutte le valutazioni, servono per il conteggio e per le recensioni con testo
        val tutte = RatingRepositoryImpl().getRatingsPerVolontario(volontarioId).getOrNull() ?: emptyList()

        // con almeno un voto mostro numero, stelle e conteggio; altrimenti solo un testo
        val media = volontario.ratingMedio
        val ratingText = vistaDialog.findViewById<TextView>(R.id.profiloRatingText)
        val ratingRow = vistaDialog.findViewById<View>(R.id.ratingRow)
        if (media != null) {
            ratingRow.visibility = View.VISIBLE
            ratingText.visibility = View.GONE
            vistaDialog.findViewById<TextView>(R.id.profiloRatingNumero).text =
                "%.1f".format(media).replace('.', ',')
            val ratingBar = vistaDialog.findViewById<RatingBar>(R.id.profiloRatingBar)
            ratingBar.rating = media.toFloat()
            ratingBar.contentDescription = "Valutazione %.1f su 5".format(media).replace('.', ',')
            vistaDialog.findViewById<TextView>(R.id.profiloNumeroValutazioni).text =
                if (tutte.size == 1) "su 1 valutazione" else "su ${tutte.size} valutazioni"
        } else {
            ratingRow.visibility = View.GONE
            ratingText.text = "Non ancora valutato"
            ratingText.visibility = View.VISIBLE
        }

        // recensioni con commento, in riquadri lavanda + "Nome R. · Tipo"
        val recensioniTitolo = vistaDialog.findViewById<TextView>(R.id.recensioniTitolo)
        val recensioniContainer = vistaDialog.findViewById<LinearLayout>(R.id.recensioniContainer)
        val recensioni = tutte.filter { !it.commento.isNullOrBlank() }
        if (recensioni.isNotEmpty()) {
            recensioniTitolo.visibility = View.VISIBLE
            val inflater = LayoutInflater.from(requireContext())
            val requestRepo = RequestRepositoryImpl()
            recensioni.forEach { recensione ->
                val riga = inflater.inflate(R.layout.item_recensione, recensioniContainer, false)
                riga.findViewById<TextView>(R.id.recensioneCommentoText).text = "“${recensione.commento}”"
                val richiesta = requestRepo.getRichiesta(recensione.requestId).getOrNull()
                riga.findViewById<TextView>(R.id.recensioneAutoreText).text =
                    RecensioneFormat.etichetta(richiesta?.autoreNome ?: "", richiesta?.tipo ?: "")
                recensioniContainer.addView(riga)
            }
        }

        // dialog senza titolo/pulsanti di default, header e "Chiudi" sono nel layout
        val dialog = AlertDialog.Builder(requireContext())
            .setView(vistaDialog)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        vistaDialog.findViewById<View>(R.id.closeButton).setOnClickListener { dialog.dismiss() }
        vistaDialog.findViewById<View>(R.id.chiudiButton).setOnClickListener { dialog.dismiss() }
        dialog.show()
    }
}

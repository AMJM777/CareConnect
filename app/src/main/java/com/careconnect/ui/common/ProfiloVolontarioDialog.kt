package com.careconnect.ui.common

import android.view.LayoutInflater
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.careconnect.R
import com.careconnect.repository.UserRepositoryImpl
import kotlinx.coroutines.launch

/**
 * FASE 7 — Direzione 1: mostra un dialog di sola lettura con nome,
 * descrizione e valutazione di un volontario. Condiviso tra Anziano
 * ("Le mie richieste") e Familiare ("Attività"): stesso contenuto in
 * entrambi i ruoli, quindi vive in un package comune invece di essere
 * duplicato due volte.
 */
fun Fragment.mostraProfiloVolontario(volontarioId: String) {
    viewLifecycleOwner.lifecycleScope.launch {
        // Lettura singola, non realtime: il profilo di un volontario non
        // cambia mentre stai guardando questo dialog, non serve un listener.
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
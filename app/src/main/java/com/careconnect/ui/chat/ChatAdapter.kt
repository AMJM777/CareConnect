package com.careconnect.ui.chat

import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.careconnect.R
import com.careconnect.databinding.ItemMessaggioBinding
import com.careconnect.model.Message
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * adapter della chat. Per i partecipanti distingue i messaggi "miei" (a destra)
 * da quelli dell'altro (a sinistra) confrontando mittenteId con l'uid corrente.
 * Nella vista osservatore del garante (mostraNomi=true) nessuno e' "mio":
 * distingue anziano e volontario per nome, colore e allineamento.
 * mostraAscolto e onAscolta servono al lato Anziano (lettura vocale TTS).
 */
class ChatAdapter(
    private val uidCorrente: String,
    private val anzianoId: String,
    private val volontarioId: String,
    private val anzianoNome: String,
    private val volontarioNome: String,
    private val mostraNomi: Boolean,
    private val mostraAscolto: Boolean,
    private val onAscolta: (String) -> Unit
) : ListAdapter<Message, ChatAdapter.MessaggioViewHolder>(MessaggioDiffCallback) {

    inner class MessaggioViewHolder(val binding: ItemMessaggioBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessaggioViewHolder {
        val binding = ItemMessaggioBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return MessaggioViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MessaggioViewHolder, position: Int) {
        val messaggio = getItem(position)
        val ctx = holder.binding.root.context
        val mio = messaggio.mittenteId == uidCorrente
        val delVolontario = messaggio.mittenteId == volontarioId
        val delAnziano = messaggio.mittenteId == anzianoId

        holder.binding.testoMessaggio.text = messaggio.testo
        holder.binding.oraMessaggio.text = formattaOra(messaggio.timestamp.toDate())

        // bolla "riempita" (prugna, testo bianco): i miei messaggi; nella vista
        // osservatore del garante, quelli dell'anziano assistito. Le altre sono
        // bianche con testo prugna. Allineamento coerente: riempite a destra.
        val riempita = if (mostraNomi) delAnziano else mio

        // nome del mittente: solo nella vista osservatore (garante), come "Nome · ruolo"
        if (mostraNomi) {
            holder.binding.mittenteNomeText.visibility = View.VISIBLE
            val nome = if (delVolontario) volontarioNome else anzianoNome
            val ruolo = if (delVolontario) "Volontario" else "Anziano"
            holder.binding.mittenteNomeText.text = "$nome · $ruolo"
        } else {
            holder.binding.mittenteNomeText.visibility = View.GONE
        }

        // allineamento (impostato in entrambi i rami per via del riciclo)
        val params = holder.binding.bollaCard.layoutParams as LinearLayout.LayoutParams
        params.gravity = if (riempita) Gravity.END else Gravity.START
        holder.binding.bollaCard.layoutParams = params

        // colori della bolla: riempita = prugna + testo bianco; altrimenti bianca + testo prugna
        val bgRes = if (riempita) R.color.care_primary else R.color.care_surface
        val txtRes = if (riempita) R.color.care_on_primary else R.color.care_primary
        val oraRes = if (riempita) R.color.care_on_primary else R.color.care_on_surface_variant
        val strokeRes = if (riempita) R.color.care_primary else R.color.care_outline
        holder.binding.bollaCard.setCardBackgroundColor(ContextCompat.getColor(ctx, bgRes))
        holder.binding.bollaCard.strokeColor = ContextCompat.getColor(ctx, strokeRes)
        holder.binding.testoMessaggio.setTextColor(ContextCompat.getColor(ctx, txtRes))
        holder.binding.oraMessaggio.setTextColor(ContextCompat.getColor(ctx, oraRes))
        holder.binding.mittenteNomeText.setTextColor(ContextCompat.getColor(ctx, txtRes))

        // pulsante di lettura vocale: solo lato Anziano e solo sui messaggi
        // ricevuti (non ha senso farsi rileggere cio' che ho scritto io)
        val mostra = mostraAscolto && !mio
        holder.binding.ascoltaButton.visibility = if (mostra) View.VISIBLE else View.GONE
        holder.binding.ascoltaButton.setOnClickListener { onAscolta(messaggio.testo) }
    }

    fun aggiornaLista(nuovaLista: List<Message>) {
        submitList(nuovaLista)
    }

    private fun formattaOra(data: java.util.Date): String {
        val formato = SimpleDateFormat("HH:mm", Locale.ITALY)
        return formato.format(data)
    }
}
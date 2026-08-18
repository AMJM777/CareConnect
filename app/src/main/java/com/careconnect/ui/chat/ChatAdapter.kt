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

        // nome del mittente: solo nella vista osservatore (garante), per capire
        // chi ha scritto (l'anziano o il volontario)
        if (mostraNomi) {
            holder.binding.mittenteNomeText.visibility = View.VISIBLE
            holder.binding.mittenteNomeText.text = if (delVolontario) volontarioNome else anzianoNome
        } else {
            holder.binding.mittenteNomeText.visibility = View.GONE
        }

        // allineamento della bolla (impostato in entrambi i rami per via del
        // riciclo): se osservo, per ruolo (anziano a destra, volontario a
        // sinistra); altrimenti i miei a destra, gli altri a sinistra
        val allineaDestra = if (mostraNomi) delAnziano else mio
        val params = holder.binding.bollaCard.layoutParams as LinearLayout.LayoutParams
        params.gravity = if (allineaDestra) Gravity.END else Gravity.START
        holder.binding.bollaCard.layoutParams = params

        // colore: nella vista osservatore distinguo per ruolo, altrimenti
        // i miei messaggi dagli altrui
        val coloreSfondo = if (mostraNomi) {
            if (delAnziano) R.color.care_primary_container else R.color.care_accent_container
        } else {
            if (mio) R.color.care_primary_container else R.color.care_surface
        }
        holder.binding.bollaCard.setCardBackgroundColor(ContextCompat.getColor(ctx, coloreSfondo))

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
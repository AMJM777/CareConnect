package com.careconnect.ui.volontario

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.careconnect.databinding.ItemRichiestaDisponibileBinding
import com.careconnect.model.Request
import com.careconnect.ui.common.RichiestaDiffCallback
import java.text.SimpleDateFormat
import java.util.Locale

// adapter della lista "Richieste disponibili": mostra solo tipo, descrizione,
// data e il bottone "Prendi in carico". niente nome o indirizzo dell'anziano,
// che qui sarebbero visibili a tutti i volontari prima dell'accettazione (è un
// problema di privacy)
class RichiesteDisponibiliAdapter(
    private val onPrendiInCaricoClick: (Request) -> Unit
) : ListAdapter<Request, RichiesteDisponibiliAdapter.RichiestaViewHolder>(RichiestaDiffCallback) {

    inner class RichiestaViewHolder(val binding: ItemRichiestaDisponibileBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RichiestaViewHolder {
        val binding = ItemRichiestaDisponibileBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return RichiestaViewHolder(binding)
    }

    // funzione che collega i dati di una richiesta alla riga corrispondente della lista
    override fun onBindViewHolder(holder: RichiestaViewHolder, position: Int) {
        val richiesta = getItem(position)

        holder.binding.tipoText.text = richiesta.tipo.replaceFirstChar { it.uppercase() }
        holder.binding.descrizioneText.text = richiesta.descrizione
        holder.binding.dataText.text = formattaData(richiesta.timestampCreazione.toDate())

        holder.binding.prendiInCaricoButton.setOnClickListener { onPrendiInCaricoClick(richiesta) }
    }

    // funzione per sostituire la lista mostrata: submitList() calcola il diff
    // in background e aggiorna la RecyclerView solo dove serve
    fun aggiornaLista(nuovaLista: List<Request>) {
        submitList(nuovaLista)
    }

    private fun formattaData(data: java.util.Date): String {
        val formato = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.ITALY)
        return formato.format(data)
    }
}
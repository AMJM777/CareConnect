package com.careconnect.ui.volontario

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.careconnect.databinding.ItemRichiestaDisponibileBinding
import com.careconnect.model.Request
import java.text.SimpleDateFormat
import java.util.Locale

// adapter della lista "Richieste disponibili": solo tipo/descrizione/data
// e il bottone "Prendi in carico". niente autoreNome/autoreIndirizzo: sono
// visibili a tutti i volontari prima dell'accettazione, mostrarli sarebbe
// un problema di privacy
class RichiesteDisponibiliAdapter(
    private val onPrendiInCaricoClick: (Request) -> Unit
) : RecyclerView.Adapter<RichiesteDisponibiliAdapter.RichiestaViewHolder>() {

    private var richieste: List<Request> = emptyList()

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
        val richiesta = richieste[position]

        holder.binding.tipoText.text = richiesta.tipo.replaceFirstChar { it.uppercase() }
        holder.binding.descrizioneText.text = richiesta.descrizione
        holder.binding.dataText.text = formattaData(richiesta.timestampCreazione.toDate())

        holder.binding.prendiInCaricoButton.setOnClickListener { onPrendiInCaricoClick(richiesta) }
    }

    override fun getItemCount(): Int = richieste.size

    // funzione per sostituire la lista mostrata e aggiornare la RecyclerView
    fun aggiornaLista(nuovaLista: List<Request>) {
        richieste = nuovaLista
        notifyDataSetChanged()
    }

    private fun formattaData(data: java.util.Date): String {
        val formato = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.ITALY)
        return formato.format(data)
    }
}
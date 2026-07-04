package com.careconnect.ui.volontario

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.careconnect.databinding.ItemRichiestaDisponibileBinding
import com.careconnect.model.Request
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Adapter della lista "Richieste disponibili". Stesso schema di
 * RichiesteAdapter (Anziano): RecyclerView.Adapter classico + ViewHolder con
 * Data Binding, notifyDataSetChanged() invece di ListAdapter/DiffUtil.
 *
 * Un solo tipo di azione possibile qui ("Prendi in carico"), passata come
 * lambda dal Fragment: l'Adapter non parla mai con ViewModel/Repository.
 */
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

    override fun onBindViewHolder(holder: RichiestaViewHolder, position: Int) {
        val richiesta = richieste[position]

        holder.binding.tipoText.text = richiesta.tipo.replaceFirstChar { it.uppercase() }
        holder.binding.descrizioneText.text = richiesta.descrizione
        holder.binding.dataText.text = formattaData(richiesta.timestampCreazione.toDate())

        holder.binding.prendiInCaricoButton.setOnClickListener { onPrendiInCaricoClick(richiesta) }
    }

    override fun getItemCount(): Int = richieste.size

    fun aggiornaLista(nuovaLista: List<Request>) {
        richieste = nuovaLista
        notifyDataSetChanged()
    }

    private fun formattaData(data: java.util.Date): String {
        val formato = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.ITALY)
        return formato.format(data)
    }
}
package com.careconnect.ui.volontario

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.careconnect.databinding.ItemRichiestaIncaricoBinding
import com.careconnect.model.Request
import com.careconnect.model.RequestStatus
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Adapter della lista "Le mie richieste prese in carico". Due azioni per
 * riga ("Segna come completata" e "Rilascia"), entrambe passate come lambda
 * dal Fragment: l'Adapter non parla mai con ViewModel/Repository.
 */
class RichiestePreseInCaricoAdapter(
    private val onCompletaClick: (Request) -> Unit,
    private val onRilasciaClick: (Request) -> Unit
) : RecyclerView.Adapter<RichiestePreseInCaricoAdapter.RichiestaViewHolder>() {

    private var richieste: List<Request> = emptyList()

    inner class RichiestaViewHolder(val binding: ItemRichiestaIncaricoBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RichiestaViewHolder {
        val binding = ItemRichiestaIncaricoBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return RichiestaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RichiestaViewHolder, position: Int) {
        val richiesta = richieste[position]

        holder.binding.tipoText.text = richiesta.tipo.replaceFirstChar { it.uppercase() }
        holder.binding.descrizioneText.text = richiesta.descrizione
        holder.binding.statoText.text = etichettaStato(richiesta.stato)
        holder.binding.dataText.text = formattaData(richiesta.timestampCreazione.toDate())

        // Entrambi i bottoni hanno senso solo mentre la richiesta è ancora
        // PRESA_IN_CARICO: una volta COMPLETATA_DAL_VOLONTARIO, tocca al
        // garante confermare, il volontario non agisce più su di essa.
        val puoAgire = richiesta.stato == RequestStatus.PRESA_IN_CARICO
        holder.binding.completaButton.visibility = if (puoAgire) View.VISIBLE else View.GONE
        holder.binding.rilasciaButton.visibility = if (puoAgire) View.VISIBLE else View.GONE

        holder.binding.completaButton.setOnClickListener { onCompletaClick(richiesta) }
        holder.binding.rilasciaButton.setOnClickListener { onRilasciaClick(richiesta) }
    }

    override fun getItemCount(): Int = richieste.size

    fun aggiornaLista(nuovaLista: List<Request>) {
        richieste = nuovaLista
        notifyDataSetChanged()
    }

    private fun etichettaStato(stato: RequestStatus): String = when (stato) {
        RequestStatus.APERTA -> "Aperta"
        RequestStatus.PRESA_IN_CARICO -> "Presa in carico"
        RequestStatus.COMPLETATA_DAL_VOLONTARIO -> "Completata, in attesa di conferma"
        RequestStatus.CONFERMATA -> "Confermata"
        RequestStatus.ANNULLATA -> "Annullata"
    }

    private fun formattaData(data: java.util.Date): String {
        val formato = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.ITALY)
        return formato.format(data)
    }
}
package com.careconnect.ui.familiare

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.careconnect.databinding.ItemRichiestaFamiliareBinding
import com.careconnect.model.Request
import com.careconnect.model.RequestStatus
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.core.content.ContextCompat
import com.careconnect.ui.common.StatoRichiestaColori

class AttivitaFamiliareAdapter(
    private val onConfermaClick: (Request) -> Unit,
    private val onVolontarioClick: (String) -> Unit
) : RecyclerView.Adapter<AttivitaFamiliareAdapter.RichiestaViewHolder>() {

    private var richieste: List<Request> = emptyList()

    inner class RichiestaViewHolder(val binding: ItemRichiestaFamiliareBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RichiestaViewHolder {
        val binding = ItemRichiestaFamiliareBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return RichiestaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RichiestaViewHolder, position: Int) {
        val richiesta = richieste[position]

        holder.binding.tipoText.text = richiesta.tipo.replaceFirstChar { it.uppercase() }
        holder.binding.descrizioneText.text = richiesta.descrizione
        holder.binding.statoText.text = etichettaStato(richiesta.stato)
        // Colora la pillola in base allo stato (sfondo tenue + testo intenso).
        val ctxStato = holder.binding.statoText.context
        holder.binding.statoText.backgroundTintList =
            ContextCompat.getColorStateList(ctxStato, StatoRichiestaColori.sfondo(richiesta.stato))
        holder.binding.statoText.setTextColor(
            ContextCompat.getColor(ctxStato, StatoRichiestaColori.testo(richiesta.stato)))
        holder.binding.dataText.text = formattaData(richiesta.timestampCreazione.toDate())

        val nomeVolontario = richiesta.volontarioNome
        if (nomeVolontario != null) {
            holder.binding.volontarioNomeText.visibility = View.VISIBLE
            holder.binding.volontarioNomeText.text = "Volontario: $nomeVolontario"
            holder.binding.volontarioNomeText.setOnClickListener {
                richiesta.volontarioId?.let { onVolontarioClick(it) }
            }
        } else {
            holder.binding.volontarioNomeText.visibility = View.GONE
        }

        val daConfermare = richiesta.stato == RequestStatus.COMPLETATA_DAL_VOLONTARIO
        holder.binding.confermaButton.visibility = if (daConfermare) View.VISIBLE else View.GONE
        holder.binding.confermaButton.setOnClickListener { onConfermaClick(richiesta) }
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
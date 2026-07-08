package com.careconnect.ui.anziano

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.careconnect.databinding.ItemRichiestaBinding
import com.careconnect.model.Request
import com.careconnect.model.RequestStatus
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.core.content.ContextCompat
import com.careconnect.ui.common.StatoRichiestaColori

class RichiesteAdapter(
    private val onModificaClick: (Request) -> Unit,
    private val onAnnullaClick: (Request) -> Unit,
    private val onVolontarioClick: (String) -> Unit
) : RecyclerView.Adapter<RichiesteAdapter.RichiestaViewHolder>() {

    private var richieste: List<Request> = emptyList()

    inner class RichiestaViewHolder(val binding: ItemRichiestaBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RichiestaViewHolder {
        val binding = ItemRichiestaBinding.inflate(
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

        // FASE 7: nome del volontario, solo se presente (richiesta già
        // accettata). Cliccabile: apre il profilo di sola lettura.
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

        holder.binding.modificaButton.visibility =
            if (richiesta.stato == RequestStatus.APERTA) View.VISIBLE else View.GONE

        val puoAnnullare = richiesta.stato.canTransitionTo(RequestStatus.ANNULLATA)
        holder.binding.annullaButton.visibility =
            if (puoAnnullare) View.VISIBLE else View.GONE

        holder.binding.modificaButton.setOnClickListener { onModificaClick(richiesta) }
        holder.binding.annullaButton.setOnClickListener { onAnnullaClick(richiesta) }
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
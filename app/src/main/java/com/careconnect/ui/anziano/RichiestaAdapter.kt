package com.careconnect.ui.anziano

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.careconnect.databinding.ItemRichiestaBinding
import com.careconnect.model.Request
import com.careconnect.model.RequestStatus
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.core.content.ContextCompat
import androidx.core.widget.TextViewCompat
import com.careconnect.ui.common.RichiestaDiffCallback
import com.careconnect.ui.common.StatoRichiestaColori

// adapter della RecyclerView che mostra le richieste dell'anziano, con i
// bottoni "Modifica" e "Annulla" e il nome del volontario, se presente.
// ListAdapter + DiffUtil (vedi RichiesteDisponibiliAdapter per il motivo).
class RichiesteAdapter(
private val onModificaClick: (Request) -> Unit,
private val onAnnullaClick: (Request) -> Unit,
private val onVolontarioClick: (String) -> Unit,
private val onChatClick: (Request) -> Unit
) : ListAdapter<Request, RichiesteAdapter.RichiestaViewHolder>(RichiestaDiffCallback) {

    inner class RichiestaViewHolder(val binding: ItemRichiestaBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RichiestaViewHolder {
        val binding = ItemRichiestaBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return RichiestaViewHolder(binding)
    }

    // funzione che collega i dati di una richiesta alla riga corrispondente della lista.
    override fun onBindViewHolder(holder: RichiestaViewHolder, position: Int) {
        val richiesta = getItem(position)

        holder.binding.tipoText.text = richiesta.tipo.replaceFirstChar { it.uppercase() }
        holder.binding.descrizioneText.text = richiesta.descrizione
        holder.binding.statoText.text = etichettaStato(richiesta.stato)
        // Colora la pillola in base allo stato (sfondo tenue + testo intenso).
        val ctxStato = holder.binding.statoText.context
        holder.binding.statoText.backgroundTintList =
            ContextCompat.getColorStateList(ctxStato, StatoRichiestaColori.sfondo(richiesta.stato))
        holder.binding.statoText.setTextColor(
            ContextCompat.getColor(ctxStato, StatoRichiestaColori.testo(richiesta.stato)))
        // Tinta del pallino (drawableStart) in base allo stato: mai solo colore
        TextViewCompat.setCompoundDrawableTintList(
            holder.binding.statoText,
            ContextCompat.getColorStateList(ctxStato, StatoRichiestaColori.pallino(richiesta.stato)))
        holder.binding.dataText.text = formattaData(richiesta.timestampCreazione.toDate())

        // nome del volontario, mostrato solo se presente. cliccabile: apre il profilo di sola lettura
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


        // chat col volontario: attiva mentre presa in carico (si scrive) e
        // dopo, completata, in sola lettura (storico).
        val mostraChat = richiesta.stato == RequestStatus.PRESA_IN_CARICO ||
                richiesta.stato == RequestStatus.COMPLETATA_DAL_VOLONTARIO
        holder.binding.chatButton.visibility = if (mostraChat) View.VISIBLE else View.GONE
        holder.binding.chatButton.setOnClickListener { onChatClick(richiesta) }
        holder.binding.modificaButton.setOnClickListener { onModificaClick(richiesta) }
        holder.binding.annullaButton.setOnClickListener { onAnnullaClick(richiesta) }
    }

    // funzione per sostituire la lista mostrata: submitList() calcola il diff
    // in background e aggiorna la RecyclerView solo dove serve
    fun aggiornaLista(nuovaLista: List<Request>) {
        submitList(nuovaLista)
    }

    // funzione per tradurre lo stato della richiesta in un'etichetta leggibile
    private fun etichettaStato(stato: RequestStatus): String = when (stato) {
        RequestStatus.APERTA -> "Aperta"
        RequestStatus.PRESA_IN_CARICO -> "Presa in carico"
        RequestStatus.COMPLETATA_DAL_VOLONTARIO -> "Completata, in attesa di conferma"
        RequestStatus.CONFERMATA -> "Confermata"
        RequestStatus.ANNULLATA -> "Annullata"
    }

    // funzione per formattare una data nel formato gg/mm/aaaa hh:mm
    private fun formattaData(data: java.util.Date): String {
        val formato = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.ITALY)
        return formato.format(data)
    }
}
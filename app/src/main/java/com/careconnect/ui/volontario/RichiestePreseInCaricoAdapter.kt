package com.careconnect.ui.volontario

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.careconnect.databinding.ItemRichiestaIncaricoBinding
import com.careconnect.model.Request
import com.careconnect.model.RequestStatus
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.core.content.ContextCompat
import com.careconnect.ui.common.RichiestaDiffCallback
import com.careconnect.ui.common.StatoRichiestaColori

/**
 * adapter della lista "Le mie richieste prese in carico". due azioni per
 * riga ("Segna come completata" e "Rilascia"), entrambe passate come lambda
 * dal Fragment: l'Adapter non parla mai con ViewModel/Repository.
 * ListAdapter + DiffUtil (vedi RichiesteDisponibiliAdapter per il motivo).
 */
class RichiestePreseInCaricoAdapter(
    private val onCompletaClick: (Request) -> Unit,
    private val onRilasciaClick: (Request) -> Unit,
    private val onChatClick: (Request) -> Unit
) : ListAdapter<Request, RichiestePreseInCaricoAdapter.RichiestaViewHolder>(RichiestaDiffCallback) {

    inner class RichiestaViewHolder(val binding: ItemRichiestaIncaricoBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RichiestaViewHolder {
        val binding = ItemRichiestaIncaricoBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return RichiestaViewHolder(binding)
    }

    // funzione che collega i dati di una richiesta alla riga corrispondente della lista.
    override fun onBindViewHolder(holder: RichiestaViewHolder, position: Int) {
        val richiesta = getItem(position)

        holder.binding.tipoText.text = richiesta.tipo.replaceFirstChar { it.uppercase() }
        holder.binding.descrizioneText.text = richiesta.descrizione
        holder.binding.autoreNomeText.text = "Da: ${richiesta.autoreNome}"
        holder.binding.autoreIndirizzoText.text = richiesta.autoreIndirizzo
        holder.binding.statoText.text = etichettaStato(richiesta.stato)
        // colora la pillola in base allo stato (sfondo tenue + testo intenso).
        val ctxStato = holder.binding.statoText.context
        holder.binding.statoText.backgroundTintList =
            ContextCompat.getColorStateList(ctxStato, StatoRichiestaColori.sfondo(richiesta.stato))
        holder.binding.statoText.setTextColor(
            ContextCompat.getColor(ctxStato, StatoRichiestaColori.testo(richiesta.stato)))
        holder.binding.dataText.text = formattaData(richiesta.timestampCreazione.toDate())

        // entrambi i bottoni hanno senso solo mentre la richiesta è ancora
        // PRESA_IN_CARICO: una volta COMPLETATA_DAL_VOLONTARIO, tocca al
        // garante confermare, il volontario non agisce più su di essa.
        val puoAgire = richiesta.stato == RequestStatus.PRESA_IN_CARICO
        holder.binding.completaButton.visibility = if (puoAgire) View.VISIBLE else View.GONE
        holder.binding.rilasciaButton.visibility = if (puoAgire) View.VISIBLE else View.GONE

        // la chat è raggiungibile mentre la richiesta è presa in carico
        // (si scrive) e anche dopo, completata, in sola lettura (storico).
        val mostraChat = richiesta.stato == RequestStatus.PRESA_IN_CARICO ||
            richiesta.stato == RequestStatus.COMPLETATA_DAL_VOLONTARIO
        holder.binding.chatButton.visibility = if (mostraChat) View.VISIBLE else View.GONE
        holder.binding.chatButton.setOnClickListener { onChatClick(richiesta) }

        holder.binding.completaButton.setOnClickListener { onCompletaClick(richiesta) }
        holder.binding.rilasciaButton.setOnClickListener { onRilasciaClick(richiesta) }
    }

    // funzione per sostituire la lista mostrata: submitList() calcola il diff
    // in background e aggiorna la RecyclerView solo dove serve
    fun aggiornaLista(nuovaLista: List<Request>) {
        submitList(nuovaLista)
    }

    // funzione per tradurre lo stato della richiesta in un'etichetta leggibile per l'utente.
    private fun etichettaStato(stato: RequestStatus): String = when (stato) {
        RequestStatus.APERTA -> "Aperta"
        RequestStatus.PRESA_IN_CARICO -> "Presa in carico"
        RequestStatus.COMPLETATA_DAL_VOLONTARIO -> "Completata, in attesa di conferma"
        RequestStatus.CONFERMATA -> "Confermata"
        RequestStatus.ANNULLATA -> "Annullata"
    }

    // funzione per formattare una data nel formato gg/mm/aaaa hh:mm.
    private fun formattaData(data: java.util.Date): String {
        val formato = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.ITALY)
        return formato.format(data)
    }
}
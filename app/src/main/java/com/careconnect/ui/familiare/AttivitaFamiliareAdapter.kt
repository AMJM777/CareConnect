package com.careconnect.ui.familiare

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.careconnect.databinding.ItemRichiestaFamiliareBinding
import com.careconnect.model.Request
import com.careconnect.model.RequestStatus
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.core.content.ContextCompat
import com.careconnect.ui.common.RichiestaDiffCallback
import com.careconnect.ui.common.StatoRichiestaColori

// adapter della lista "Attività" del familiare: mostra le richieste
// dell'anziano seguito, con "Conferma" visibile solo su quelle completate
// dal volontario e in attesa di conferma.
// ListAdapter + DiffUtil (vedi RichiesteDisponibiliAdapter per il motivo).
class AttivitaFamiliareAdapter(
    private val onConfermaClick: (Request) -> Unit,
    private val onVolontarioClick: (String) -> Unit,
    private val onChatClick: (Request) -> Unit
) : ListAdapter<Request, AttivitaFamiliareAdapter.RichiestaViewHolder>(RichiestaDiffCallback) {

    inner class RichiestaViewHolder(val binding: ItemRichiestaFamiliareBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RichiestaViewHolder {
        val binding = ItemRichiestaFamiliareBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return RichiestaViewHolder(binding)
    }

    // funzione che collega i dati di una richiesta alla riga corrispondente della lista
    override fun onBindViewHolder(holder: RichiestaViewHolder, position: Int) {
        val richiesta = getItem(position)

        holder.binding.tipoText.text = richiesta.tipo.replaceFirstChar { it.uppercase() }
        holder.binding.descrizioneText.text = richiesta.descrizione
        holder.binding.statoText.text = etichettaStato(richiesta.stato)
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

        // "Vedi chat" (sola lettura) c'è quando esiste una conversazione:
        // dalla presa in carico in poi, anche a lavoro completato/confermato
        val esisteChat = richiesta.stato == RequestStatus.PRESA_IN_CARICO ||
            richiesta.stato == RequestStatus.COMPLETATA_DAL_VOLONTARIO ||
            richiesta.stato == RequestStatus.CONFERMATA
        holder.binding.chatButton.visibility = if (esisteChat) View.VISIBLE else View.GONE
        holder.binding.chatButton.setOnClickListener { onChatClick(richiesta) }

        // "Conferma" ha senso solo quando il volontario ha già segnato la
        // richiesta come completata: tocca al familiare confermarla e valutare
        val daConfermare = richiesta.stato == RequestStatus.COMPLETATA_DAL_VOLONTARIO
        holder.binding.confermaButton.visibility = if (daConfermare) View.VISIBLE else View.GONE
        holder.binding.confermaButton.setOnClickListener { onConfermaClick(richiesta) }
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
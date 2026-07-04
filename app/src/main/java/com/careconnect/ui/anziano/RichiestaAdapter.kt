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

/**
 * Adapter della lista "Le mie richieste". Segue lo schema visto a lezione
 * (RecyclerView.Adapter + ViewHolder con Data Binding), non ListAdapter/DiffUtil.
 *
 * onModificaClick e onAnnullaClick sono lambda passate dal Fragment: l'Adapter
 * non parla mai direttamente con ViewModel o Repository, si limita a
 * notificare "l'utente ha toccato questo bottone su questa richiesta".
 */
class RichiesteAdapter(
    private val onModificaClick: (Request) -> Unit,
    private val onAnnullaClick: (Request) -> Unit
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
        holder.binding.dataText.text = formattaData(richiesta.timestampCreazione.toDate())

        // Modifica: solo se ancora APERTA (nessun volontario coinvolto).
        holder.binding.modificaButton.visibility =
            if (richiesta.stato == RequestStatus.APERTA) View.VISIBLE else View.GONE

        // Annulla: permesso da APERTA o PRESA_IN_CARICO (vedi
        // RequestStatus.canTransitionTo), non dagli stati terminali.
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
package com.careconnect.ui.common

import androidx.recyclerview.widget.DiffUtil
import com.careconnect.model.Request

/**
 * DiffUtil condiviso dagli adapter che mostrano liste di Request.
 * confronta le righe per id e i contenuti campo per campo, così la
 * RecyclerView aggiorna e "anima" solo le righe che cambiano davvero
 */
object RichiestaDiffCallback : DiffUtil.ItemCallback<Request>() {

    // stessa richiesta se ha lo stesso id, indipendentemente da cosa è cambiato
    override fun areItemsTheSame(oldItem: Request, newItem: Request): Boolean =
        oldItem.id == newItem.id

    // Request è una data class, quindi == confronta tutti i campi automaticamente
    override fun areContentsTheSame(oldItem: Request, newItem: Request): Boolean =
        oldItem == newItem
}

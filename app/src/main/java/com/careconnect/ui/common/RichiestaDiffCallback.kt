package com.careconnect.ui.common

import androidx.recyclerview.widget.DiffUtil
import com.careconnect.model.Request

/**
 * DiffUtil condiviso da tutti gli adapter che mostrano liste di Request
 * (disponibili, prese in carico, mie richieste, attività familiare).
 *
 * Prima questi adapter usavano notifyDataSetChanged() ad ogni aggiornamento:
 * dice a RecyclerView "è cambiato tutto", quindi ogni riga viene animata
 * come se fosse cambiata anche quando in realtà è identica. Combinato con
 * query Firestore senza orderBy (il cui ordine può cambiare tra uno
 * snapshot e l'altro), questo causava righe che per un istante apparivano
 * vuote durante l'animazione di transizione.
 *
 * Con ListAdapter + questo DiffUtil, RecyclerView sa esattamente quali righe
 * sono state aggiunte/rimosse/spostate e anima solo quelle, eliminando il
 * problema alla radice invece di limitarsi a disattivare le animazioni.
 */
object RichiestaDiffCallback : DiffUtil.ItemCallback<Request>() {

    // stessa richiesta se ha lo stesso id, indipendentemente da cosa è cambiato
    override fun areItemsTheSame(oldItem: Request, newItem: Request): Boolean =
        oldItem.id == newItem.id

    // Request è una data class: == confronta tutti i campi automaticamente
    override fun areContentsTheSame(oldItem: Request, newItem: Request): Boolean =
        oldItem == newItem
}

package com.careconnect.ui.chat

import androidx.recyclerview.widget.DiffUtil
import com.careconnect.model.Message

// DiffUtil per la lista dei messaggi (stesso schema di RichiestaDiffCallback)
object MessaggioDiffCallback : DiffUtil.ItemCallback<Message>() {
    override fun areItemsTheSame(oldItem: Message, newItem: Message): Boolean =
        oldItem.id == newItem.id

    override fun areContentsTheSame(oldItem: Message, newItem: Message): Boolean =
        oldItem == newItem
}
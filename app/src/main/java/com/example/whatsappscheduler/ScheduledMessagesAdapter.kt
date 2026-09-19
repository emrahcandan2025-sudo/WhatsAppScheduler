package com.example.whatsappscheduler

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ScheduledMessagesAdapter(
    private val items: MutableList<ScheduledMessage>,
    private val onDelete: (ScheduledMessage) -> Unit
) : RecyclerView.Adapter<ScheduledMessagesAdapter.ViewHolder>() {

    private val dateFormat = SimpleDateFormat("dd MMM yyyy HH:mm", Locale("tr"))

    class ViewHolder(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
        val phone: android.widget.TextView = itemView.findViewById(R.id.textItemPhone)
        val message: android.widget.TextView = itemView.findViewById(R.id.textItemMessage)
        val time: android.widget.TextView = itemView.findViewById(R.id.textItemTime)
        val deleteButton: android.widget.Button = itemView.findViewById(R.id.btnItemDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_scheduled_message, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.phone.text = item.phoneNumber
        holder.message.text = item.message
        val status = if (item.sent) "Gönderildi" else "Bekliyor"
        holder.time.text = "${dateFormat.format(Date(item.timestampMillis))} • $status"
        holder.deleteButton.setOnClickListener { onDelete(item) }
    }

    override fun getItemCount(): Int = items.size

    fun updateData(newItems: List<ScheduledMessage>) {
        items.clear()
        items.addAll(newItems.sortedBy { it.timestampMillis })
        notifyDataSetChanged()
    }
}

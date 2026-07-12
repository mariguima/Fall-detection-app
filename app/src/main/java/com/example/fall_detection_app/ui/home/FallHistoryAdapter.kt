package com.example.fall_detection_app.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.example.fall_detection_app.R
import java.text.SimpleDateFormat
import java.util.Locale

class FallHistoryAdapter(
    private val events: MutableList<FallEvent>
) : RecyclerView.Adapter<FallHistoryAdapter.FallViewHolder>() {

    inner class FallViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDate: TextView = view.findViewById(R.id.tvDate)
        val tvTimeStatus: TextView = view.findViewById(R.id.tvTimeStatus)
        val card: CardView = view.findViewById(R.id.cardFallItem)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FallViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_fall_history, parent, false)
        return FallViewHolder(view)
    }

    override fun onBindViewHolder(holder: FallViewHolder, position: Int) {
        val event = events[position]

        // format timestamp
        val timestamp = event.espTimestamp?.toDate()
        val dateFormat = SimpleDateFormat("dd/MM", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

        holder.tvDate.text = if (timestamp != null) dateFormat.format(timestamp) else "--/--"

        val timeStr = if (timestamp != null) timeFormat.format(timestamp) else "--:--"
        val statusStr = when (event.status) {
            "warning" -> "awaiting confirmation"
            "fall" -> "marked as real"
            "fake_alarm" -> "false alarm"
            else -> event.status
        }
        holder.tvTimeStatus.text = "$timeStr - $statusStr"

        // color code by status
        val bgColor = when (event.status) {
            "warning" -> 0xFFFFF3CD.toInt()  // yellow
            "fall" -> 0xFFFFE0E0.toInt()     // red tint
            "fake_alarm" -> 0xFFFFFFFF.toInt() // white
            else -> 0xFFFFFFFF.toInt()
        }
        holder.card.setCardBackgroundColor(bgColor)
    }

    override fun getItemCount() = events.size

    fun updateEvents(newEvents: List<FallEvent>) {
        events.clear()
        events.addAll(newEvents)
        notifyDataSetChanged()
    }
}
package com.example.fall_detection_app.ui.home

import android.graphics.Color
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.fall_detection_app.R
import com.google.android.material.card.MaterialCardView
import java.text.SimpleDateFormat
import java.util.Locale

class FallHistoryAdapter(
    private val events: MutableList<FallEvent>,
    private val onEventClick: (FallEvent) -> Unit
) : RecyclerView.Adapter<FallHistoryAdapter.FallViewHolder>() {

    inner class FallViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDate: TextView = view.findViewById(R.id.tvDate)
        val tvTimeStatus: TextView = view.findViewById(R.id.tvTimeStatus)
        val card: MaterialCardView = view.findViewById(R.id.cardFallItem)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FallViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_fall_history, parent, false)
        return FallViewHolder(view)
    }

    override fun onBindViewHolder(holder: FallViewHolder, position: Int) {
        val event = events[position]

        // format timestamp
        val timestamp = (event.serverTimestamp ?: event.espTimestamp)?.toDate()
        val dateFormat = SimpleDateFormat("dd/MM", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

        holder.tvDate.text = if (timestamp != null) dateFormat.format(timestamp) else "--/--"

        val timeStr = if (timestamp != null) timeFormat.format(timestamp) else "--:--"
        val statusStr = when (event.status) {
            "warning" -> "awaiting confirmation"
            "fall" -> "marked as real"
            "no_fall" -> "false alarm"
            "fake_alarm" -> "false alarm"
            else -> event.status
        }
        val context = holder.itemView.context
        val defaultTextColor = ContextCompat.getColor(context, R.color.text_dark)
        val accentColor = when (event.status) {
            "fall" -> ContextCompat.getColor(context, R.color.accent_pink)
            "no_fall", "fake_alarm" -> Color.parseColor("#69A297")
            else -> null
        }

        holder.card.setCardBackgroundColor(Color.parseColor("#FFF8E1"))
        holder.card.strokeColor = accentColor ?: Color.TRANSPARENT
        holder.tvDate.setTextColor(accentColor ?: defaultTextColor)
        holder.tvTimeStatus.setTextColor(defaultTextColor)
        holder.tvTimeStatus.text = formatTimeStatus(timeStr, statusStr, accentColor)
        holder.card.setOnClickListener { onEventClick(event) }
    }

    private fun formatTimeStatus(
        timeStr: String,
        statusStr: String,
        accentColor: Int?
    ): CharSequence {
        val text = "$timeStr - $statusStr"

        if (accentColor == null) {
            return text
        }

        return SpannableString(text).apply {
            val statusStart = text.indexOf(statusStr)
            if (statusStart >= 0) {
                setSpan(
                    ForegroundColorSpan(accentColor),
                    statusStart,
                    text.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        }
    }

    override fun getItemCount() = events.size

    fun updateEvents(newEvents: List<FallEvent>) {
        events.clear()
        events.addAll(newEvents)
        notifyDataSetChanged()
    }
}

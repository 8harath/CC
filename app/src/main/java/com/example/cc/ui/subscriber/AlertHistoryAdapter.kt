package com.example.cc.ui.subscriber

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.cc.data.model.Incident
import com.example.cc.databinding.ItemAlertCardBinding
import java.text.SimpleDateFormat
import java.util.*

class AlertHistoryAdapter(
    private val onItemClick: (Incident) -> Unit
) : ListAdapter<Incident, AlertHistoryAdapter.AlertViewHolder>(AlertDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlertViewHolder {
        val binding = ItemAlertCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return AlertViewHolder(binding, onItemClick)
    }

    override fun onBindViewHolder(holder: AlertViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class AlertViewHolder(
        private val binding: ItemAlertCardBinding,
        private val onItemClick: (Incident) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(incident: Incident) {
            binding.root.setOnClickListener { onItemClick(incident) }
            
            binding.tvAlertMessage.text = incident.message
            binding.tvAlertLocation.text = incident.location
            binding.tvAlertTimestamp.text = formatTimestamp(incident.timestamp)
            binding.tvAlertSeverity.text = incident.severity
            
            // Set severity color
            val severityColor = when (incident.severity.uppercase()) {
                "CRITICAL" -> android.graphics.Color.RED
                "HIGH" -> android.graphics.Color.parseColor("#FF6B35")
                "MEDIUM" -> android.graphics.Color.parseColor("#FFA500")
                "LOW" -> android.graphics.Color.parseColor("#4CAF50")
                else -> android.graphics.Color.GRAY
            }
            
            binding.tvAlertSeverity.setTextColor(severityColor)
        }
        
        private fun formatTimestamp(timestamp: Long): String {
            val dateFormat = SimpleDateFormat("MMM dd, HH:mm:ss", Locale.getDefault())
            return dateFormat.format(Date(timestamp))
        }
    }

    private class AlertDiffCallback : DiffUtil.ItemCallback<Incident>() {
        override fun areItemsTheSame(oldItem: Incident, newItem: Incident): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Incident, newItem: Incident): Boolean {
            return oldItem == newItem
        }
    }
}

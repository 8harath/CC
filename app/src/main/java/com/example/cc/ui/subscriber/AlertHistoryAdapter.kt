package com.example.cc.ui.subscriber

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.cc.R
import com.example.cc.util.EmergencyAlertMessage
import java.text.SimpleDateFormat
import java.util.*

class AlertHistoryAdapter : ListAdapter<EmergencyAlertMessage, AlertHistoryAdapter.AlertViewHolder>(DIFF) {
    
    var onIncidentClick: ((EmergencyAlertMessage) -> Unit)? = null
    var responseStatusMap: Map<String, String> = emptyMap()
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlertViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_alert_card, parent, false)
        return AlertViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: AlertViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    fun updateResponseStatus(statusMap: Map<String, String>) {
        responseStatusMap = statusMap
        notifyDataSetChanged()
    }
    
    inner class AlertViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val card: CardView = itemView.findViewById(R.id.cardAlert)
        private val tvName: TextView = itemView.findViewById(R.id.tvVictimName)
        private val tvTime: TextView = itemView.findViewById(R.id.tvTime)
        private val tvSeverity: TextView = itemView.findViewById(R.id.tvSeverity)
        private val tvLocation: TextView = itemView.findViewById(R.id.tvLocation)
        private val tvResponseStatus: TextView = itemView.findViewById(R.id.tvResponseStatus)
        
        init {
            card.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onIncidentClick?.invoke(getItem(position))
                }
            }
        }
        
        fun bind(alert: EmergencyAlertMessage) {
            tvName.text = alert.victimName
            tvSeverity.text = "Severity: ${alert.severity}"
            tvLocation.text = "Lat: %.4f, Lng: %.4f".format(alert.location.latitude, alert.location.longitude)
            
            val sdf = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
            tvTime.text = sdf.format(Date(alert.timestamp))
            
            // Show response status if available
            val responseStatus = responseStatusMap[alert.incidentId]
            if (responseStatus != null) {
                tvResponseStatus.text = responseStatus
                tvResponseStatus.visibility = View.VISIBLE
            } else {
                tvResponseStatus.visibility = View.GONE
            }
            
            // Set card background color based on severity
            when (alert.severity.uppercase()) {
                "HIGH" -> card.setCardBackgroundColor(itemView.context.getColor(R.color.red_light))
                "MEDIUM" -> card.setCardBackgroundColor(itemView.context.getColor(R.color.orange_light))
                "LOW" -> card.setCardBackgroundColor(itemView.context.getColor(R.color.yellow_light))
                else -> card.setCardBackgroundColor(itemView.context.getColor(R.color.white))
            }
        }
    }
    companion object {
        val DIFF = object : DiffUtil.ItemCallback<EmergencyAlertMessage>() {
            override fun areItemsTheSame(oldItem: EmergencyAlertMessage, newItem: EmergencyAlertMessage): Boolean =
                oldItem.incidentId == newItem.incidentId
            override fun areContentsTheSame(oldItem: EmergencyAlertMessage, newItem: EmergencyAlertMessage): Boolean =
                oldItem == newItem
        }
    }
}
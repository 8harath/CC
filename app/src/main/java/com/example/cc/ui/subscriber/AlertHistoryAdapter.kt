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
import com.example.cc.util.MqttMessageSchemas
import java.text.SimpleDateFormat
import java.util.*

class AlertHistoryAdapter : ListAdapter<MqttMessageSchemas.EmergencyAlertMessage, AlertHistoryAdapter.AlertViewHolder>(DIFF) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlertViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_alert_card, parent, false)
        return AlertViewHolder(view)
    }
    override fun onBindViewHolder(holder: AlertViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    class AlertViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val card: CardView = itemView.findViewById(R.id.cardAlert)
        private val tvName: TextView = itemView.findViewById(R.id.tvVictimName)
        private val tvTime: TextView = itemView.findViewById(R.id.tvTime)
        private val tvSeverity: TextView = itemView.findViewById(R.id.tvSeverity)
        private val tvLocation: TextView = itemView.findViewById(R.id.tvLocation)
        fun bind(alert: MqttMessageSchemas.EmergencyAlertMessage) {
            tvName.text = alert.victimName
            tvSeverity.text = "Severity: ${alert.severity}"
            tvLocation.text = "Lat: %.4f, Lng: %.4f".format(alert.location.latitude, alert.location.longitude)
            val sdf = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
            tvTime.text = sdf.format(Date(alert.timestamp))
        }
    }
    companion object {
        val DIFF = object : DiffUtil.ItemCallback<MqttMessageSchemas.EmergencyAlertMessage>() {
            override fun areItemsTheSame(oldItem: MqttMessageSchemas.EmergencyAlertMessage, newItem: MqttMessageSchemas.EmergencyAlertMessage): Boolean =
                oldItem.incidentId == newItem.incidentId
            override fun areContentsTheSame(oldItem: MqttMessageSchemas.EmergencyAlertMessage, newItem: MqttMessageSchemas.EmergencyAlertMessage): Boolean =
                oldItem == newItem
        }
    }
}
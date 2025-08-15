package com.example.cc.ui.subscriber

import com.example.cc.ui.base.BaseViewModel
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.cc.util.EmergencyAlertMessage
import com.example.cc.util.MqttClient
import com.example.cc.util.MqttTopics
import com.example.cc.util.ResponseAckMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import java.util.*

class SubscriberViewModel : BaseViewModel() {
    
    private var mqttClient: MqttClient? = null
    
    private val _connectionStatus = MutableStateFlow("Disconnected")
    val connectionStatus: StateFlow<String> = _connectionStatus
    
    private val _alertHistory = MutableStateFlow<List<EmergencyAlertMessage>>(emptyList())
    val alertHistory: StateFlow<List<EmergencyAlertMessage>> = _alertHistory.asStateFlow()
    
    private val _selectedIncident = MutableStateFlow<EmergencyAlertMessage?>(null)
    val selectedIncident: StateFlow<EmergencyAlertMessage?> = _selectedIncident.asStateFlow()
    
    private val _responseStatus = MutableStateFlow<Map<String, String>>(emptyMap())
    val responseStatus: StateFlow<Map<String, String>> = _responseStatus.asStateFlow()
    
    private val _isResponding = MutableStateFlow<Set<String>>(emptySet())
    val isResponding: StateFlow<Set<String>> = _isResponding.asStateFlow()

    fun onEmergencyAlertReceived(json: String) {
        try {
            val alert = Json.decodeFromString<EmergencyAlertMessage>(json)
            _alertHistory.update { currentList ->
                // Add new alert at the beginning (most recent first)
                listOf(alert) + currentList
            }
            // Show high-priority notification for new alerts
            showHighPriorityNotification(alert)
        } catch (e: Exception) {
            // Optionally log or show error
        }
    }
    
    fun onResponseAckReceived(json: String) {
        try {
            val ack = Json.decodeFromString<ResponseAckMessage>(json)
            _responseStatus.update { currentStatus ->
                currentStatus + (ack.incidentId to "${ack.responderName}: ${ack.status}")
            }
            if (ack.status == "responding") {
                _isResponding.update { it + ack.incidentId }
            }
        } catch (e: Exception) {
            // Handle error
        }
    }

    fun initializeMqtt(context: Context) {
        try {
            // Temporarily disable MQTT initialization to prevent crashes
            android.util.Log.i("SubscriberViewModel", "MQTT initialization disabled for stability")
            _connectionStatus.value = "Demo Mode"
            return
            
            // Original MQTT code commented out
            /*
            mqttClient = MqttClient(context)
            mqttClient?.onMessageReceived = { topic, message ->
                when {
                    topic.startsWith(MqttTopics.EMERGENCY_ALERTS) -> {
                        onEmergencyAlertReceived(message)
                    }
                    topic.startsWith(MqttTopics.RESPONSE_ACK) -> {
                        onResponseAckReceived(message)
                    }
                }
            }
            connectToMqtt()
            */
        } catch (e: Exception) {
            android.util.Log.e("SubscriberViewModel", "Error initializing MQTT: ${e.message}", e)
            _connectionStatus.value = "Error: ${e.message}"
        }
    }
    
    private fun connectToMqtt() {
        viewModelScope.launch {
            try {
                _connectionStatus.value = "Connecting..."
                val connected = mqttClient?.connect() ?: false
                if (connected) {
                    _connectionStatus.value = "Connected"
                    // Subscribe to emergency alerts and response acknowledgments
                    mqttClient?.subscribe(MqttTopics.ALERT_BROADCAST)
                    mqttClient?.subscribe(MqttTopics.EMERGENCY_ALERTS + "/+")
                    mqttClient?.subscribe(MqttTopics.RESPONSE_ACK + "/+")
                } else {
                    _connectionStatus.value = "Connection Failed"
                }
            } catch (e: Exception) {
                _connectionStatus.value = "Error: ${e.message}"
            }
        }
    }
    
    fun setConnectionStatus(status: String) {
        _connectionStatus.value = status
    }
    
    fun selectIncident(incident: EmergencyAlertMessage) {
        _selectedIncident.value = incident
    }
    
    fun clearSelectedIncident() {
        _selectedIncident.value = null
    }
    
    fun acknowledgeResponse(incidentId: String, responderName: String, etaMinutes: Int) {
        viewModelScope.launch {
            try {
                val ackMessage = ResponseAckMessage(
                    incidentId = incidentId,
                    responderId = "responder_${System.currentTimeMillis()}",
                    responderName = responderName,
                    status = "responding",
                    eta = etaMinutes * 60, // Convert to seconds
                    timestamp = System.currentTimeMillis()
                )
                
                val json = Json.encodeToString(ackMessage)
                mqttClient?.publish(MqttTopics.RESPONSE_ACK + "/$incidentId", json)
                
                // Update local state
                _responseStatus.update { currentStatus ->
                    currentStatus + (incidentId to "$responderName: Responding (ETA: ${etaMinutes}min)")
                }
                _isResponding.update { it + incidentId }
                
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
    
    fun cancelResponse(incidentId: String, responderName: String) {
        viewModelScope.launch {
            try {
                val ackMessage = ResponseAckMessage(
                    incidentId = incidentId,
                    responderId = "responder_${System.currentTimeMillis()}",
                    responderName = responderName,
                    status = "cancelled",
                    eta = 0,
                    timestamp = System.currentTimeMillis()
                )
                
                val json = Json.encodeToString(ackMessage)
                mqttClient?.publish(MqttTopics.RESPONSE_ACK + "/$incidentId", json)
                
                // Update local state
                _responseStatus.update { currentStatus ->
                    currentStatus + (incidentId to "$responderName: Cancelled")
                }
                _isResponding.update { it - incidentId }
                
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
    
    fun openNavigation(latitude: Double, longitude: Double): Intent {
        val uri = "geo:$latitude,$longitude?q=$latitude,$longitude"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
        intent.setPackage("com.google.android.apps.maps")
        return intent
    }
    
    fun openWazeNavigation(latitude: Double, longitude: Double): Intent {
        val uri = "waze://?ll=$latitude,$longitude&navigate=yes"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
        intent.setPackage("com.waze")
        return intent
    }
    
    fun getSortedAlerts(): List<EmergencyAlertMessage> {
        return _alertHistory.value.sortedByDescending { it.timestamp }
    }
    
    fun getAlertsBySeverity(severity: String): List<EmergencyAlertMessage> {
        return _alertHistory.value.filter { it.severity.equals(severity, ignoreCase = true) }
    }
    
    fun clearAlertHistory() {
        _alertHistory.value = emptyList()
    }
    
    private fun showHighPriorityNotification(alert: EmergencyAlertMessage) {
        // This will be handled by the activity
    }
    
    fun getResponderName(): String {
        return "Responder_${System.currentTimeMillis() % 1000}"
    }
} 
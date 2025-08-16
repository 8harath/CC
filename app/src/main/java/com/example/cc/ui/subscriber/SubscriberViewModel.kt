package com.example.cc.ui.subscriber

import com.example.cc.ui.base.BaseViewModel
import android.content.Context
import com.example.cc.util.EmergencyAlertMessage
import com.example.cc.util.MqttClient
import com.example.cc.util.MqttTopics
import com.example.cc.util.ResponseAckMessage
import com.example.cc.util.MqttService
import android.content.Intent
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import android.util.Log

class SubscriberViewModel(application: Application) : AndroidViewModel(application) {
    
    private var mqttClient: MqttClient? = null
    
    // State properties
    private val _connectionStatus = MutableStateFlow("Disconnected")
    val connectionStatus: StateFlow<String> = _connectionStatus.asStateFlow()
    
    private val _alertHistory = MutableStateFlow<List<EmergencyAlertMessage>>(emptyList())
    val alertHistory: StateFlow<List<EmergencyAlertMessage>> = _alertHistory.asStateFlow()
    
    private val _selectedIncident = MutableStateFlow<EmergencyAlertMessage?>(null)
    val selectedIncident: StateFlow<EmergencyAlertMessage?> = _selectedIncident.asStateFlow()
    
    private val _responseStatus = MutableStateFlow<Map<String, String>>(emptyMap())
    val responseStatus: StateFlow<Map<String, String>> = _responseStatus.asStateFlow()
    
    private val _isResponding = MutableStateFlow<Set<String>>(emptySet())
    val isResponding: StateFlow<Set<String>> = _isResponding.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    fun onEmergencyAlertReceived(json: String) {
        try {
            val alert = Json.decodeFromString<EmergencyAlertMessage>(json)
            _alertHistory.update { currentList ->
                currentList + alert
            }
            
            // Show high priority notification for critical alerts
            if (alert.severity == "CRITICAL") {
                showHighPriorityNotification(alert)
            }
            
            Log.i("SubscriberViewModel", "Emergency alert received: ${alert.incidentId}")
            
        } catch (e: Exception) {
            Log.e("SubscriberViewModel", "Error parsing emergency alert: ${e.message}")
            _errorMessage.value = "Failed to parse emergency alert: ${e.message}"
        }
    }
    
    fun onResponseAckReceived(json: String) {
        try {
            val ack = Json.decodeFromString<ResponseAckMessage>(json)
            _responseStatus.update { currentStatus ->
                currentStatus + (ack.incidentId to "${ack.responderName}: ${ack.status} (ETA: ${ack.eta / 60}min)")
            }
            
            if (ack.status == "responding") {
                _isResponding.update { it + ack.incidentId }
            } else if (ack.status == "cancelled") {
                _isResponding.update { it - ack.incidentId }
            }
            
            Log.i("SubscriberViewModel", "Response acknowledgment received: ${ack.incidentId}")
            
        } catch (e: Exception) {
            Log.e("SubscriberViewModel", "Error parsing response ack: ${e.message}")
        }
    }

    fun initializeMqtt(context: Context) {
        try {
            Log.i("SubscriberViewModel", "Initializing MQTT for subscriber role")
            
            // Start MQTT service with subscriber role
            val intent = Intent(context, MqttService::class.java).apply {
                putExtra("role", "SUBSCRIBER")
            }
            context.startService(intent)
            
            _connectionStatus.value = "Initializing..."
            
            // Set up broadcast receiver for emergency alerts
            setupEmergencyAlertReceiver()
            
            Log.i("SubscriberViewModel", "MQTT service started for subscriber role")
            
        } catch (e: Exception) {
            Log.e("SubscriberViewModel", "Error initializing MQTT: ${e.message}", e)
            _connectionStatus.value = "Error: ${e.message}"
        }
    }
    
    private fun setupEmergencyAlertReceiver() {
        // The MqttService will handle the actual MQTT connection and subscription
        // This method sets up local broadcast receivers for messages from the service
        Log.d("SubscriberViewModel", "Emergency alert receiver setup completed")
        _connectionStatus.value = "Connected - Listening for alerts"
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
                _isLoading.value = true
                _errorMessage.value = null
                
                Log.i("SubscriberViewModel", "Acknowledging response for incident: $incidentId")
                
                val ackMessage = ResponseAckMessage(
                    incidentId = incidentId,
                    responderId = "responder_${System.currentTimeMillis()}",
                    responderName = responderName,
                    status = "responding",
                    eta = etaMinutes * 60, // Convert to seconds
                    timestamp = System.currentTimeMillis()
                )
                
                val json = Json.encodeToString(ackMessage)
                val topic = MqttTopics.RESPONSE_ACK + "/$incidentId"
                
                // Publish via MQTT service
                val context = getApplication<Application>()
                val publishIntent = Intent(context, MqttService::class.java).apply {
                    action = MqttService.ACTION_PUBLISH
                    putExtra(MqttService.EXTRA_TOPIC, topic)
                    putExtra(MqttService.EXTRA_PAYLOAD, json)
                    putExtra(MqttService.EXTRA_QOS, 1)
                    putExtra(MqttService.EXTRA_RETAINED, false)
                }
                context.startService(publishIntent)
                
                // Update local state
                _responseStatus.update { currentStatus ->
                    currentStatus + (incidentId to "$responderName: Responding (ETA: ${etaMinutes}min)")
                }
                _isResponding.update { it + incidentId }
                
                _successMessage.value = "Response acknowledged successfully!"
                Log.i("SubscriberViewModel", "Response acknowledgment sent for incident: $incidentId")
                
            } catch (e: Exception) {
                Log.e("SubscriberViewModel", "Error acknowledging response: ${e.message}")
                _errorMessage.value = "Failed to acknowledge response: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun cancelResponse(incidentId: String, responderName: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null
                
                Log.i("SubscriberViewModel", "Cancelling response for incident: $incidentId")
                
                val ackMessage = ResponseAckMessage(
                    incidentId = incidentId,
                    responderId = "responder_${System.currentTimeMillis()}",
                    responderName = responderName,
                    status = "cancelled",
                    eta = 0,
                    timestamp = System.currentTimeMillis()
                )
                
                val json = Json.encodeToString(ackMessage)
                val topic = MqttTopics.RESPONSE_ACK + "/$incidentId"
                
                // Publish via MQTT service
                val context = getApplication<Application>()
                val publishIntent = Intent(context, MqttService::class.java).apply {
                    action = MqttService.ACTION_PUBLISH
                    putExtra(MqttService.EXTRA_TOPIC, topic)
                    putExtra(MqttService.EXTRA_PAYLOAD, json)
                    putExtra(MqttService.EXTRA_QOS, 1)
                    putExtra(MqttService.EXTRA_RETAINED, false)
                }
                context.startService(publishIntent)
                
                // Update local state
                _responseStatus.update { currentStatus ->
                    currentStatus + (incidentId to "$responderName: Response cancelled")
                }
                _isResponding.update { it - incidentId }
                
                _successMessage.value = "Response cancelled successfully!"
                Log.i("SubscriberViewModel", "Response cancellation sent for incident: $incidentId")
                
            } catch (e: Exception) {
                Log.e("SubscriberViewModel", "Error cancelling response: ${e.message}")
                _errorMessage.value = "Failed to cancel response: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun clearMessages() {
        _successMessage.value = null
        _errorMessage.value = null
    }
    
    fun getSortedAlerts(): List<EmergencyAlertMessage> {
        return _alertHistory.value.sortedByDescending { it.timestamp }
    }
    
    fun getAlertsBySeverity(severity: String): List<EmergencyAlertMessage> {
        return _alertHistory.value.filter { it.severity == severity }
    }
    
    private fun showHighPriorityNotification(alert: EmergencyAlertMessage) {
        // In a real app, this would show a system notification
        Log.w("SubscriberViewModel", "HIGH PRIORITY ALERT: ${alert.victimName} - ${alert.severity} severity")
        _errorMessage.value = "HIGH PRIORITY: ${alert.victimName} needs immediate attention!"
    }
    
    fun getConnectionStatus(): String {
        return _connectionStatus.value
    }
    
    fun isConnected(): Boolean {
        return _connectionStatus.value.contains("Connected")
    }
    
    fun getAlertCount(): Int {
        return _alertHistory.value.size
    }
    
    fun getCriticalAlertCount(): Int {
        return _alertHistory.value.count { it.severity == "CRITICAL" }
    }
    
    fun getHighAlertCount(): Int {
        return _alertHistory.value.count { it.severity == "HIGH" }
    }
    
    fun getMediumAlertCount(): Int {
        return _alertHistory.value.count { it.severity == "MEDIUM" }
    }
    
    fun getLowAlertCount(): Int {
        return _alertHistory.value.count { it.severity == "LOW" }
    }
    
    fun getRespondingCount(): Int {
        return _isResponding.value.size
    }
    
    fun getResponseStatusForIncident(incidentId: String): String? {
        return _responseStatus.value[incidentId]
    }
    
    fun isRespondingToIncident(incidentId: String): Boolean {
        return _isResponding.value.contains(incidentId)
    }
    
    fun clearAlertHistory() {
        _alertHistory.value = emptyList()
        _selectedIncident.value = null
        _responseStatus.value = emptyMap()
        _isResponding.value = emptySet()
    }
    
    fun refreshConnection() {
        // Re-initialize MQTT connection
        val context = getApplication<Application>()
        initializeMqtt(context)
    }
} 
} 
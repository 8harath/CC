package com.example.cc.ui.subscriber

import com.example.cc.ui.base.BaseViewModel
import android.content.Context
import com.example.cc.util.EmergencyAlertMessage
import com.example.cc.util.MqttClient
import com.example.cc.util.MqttTopics
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class SubscriberViewModel : BaseViewModel() {
    
    private var mqttClient: MqttClient? = null
    
    private val _connectionStatus = MutableStateFlow("Disconnected")
    val connectionStatus: StateFlow<String> = _connectionStatus
    
    private val _alertHistory = MutableStateFlow<List<EmergencyAlertMessage>>(emptyList())
    val alertHistory: StateFlow<List<EmergencyAlertMessage>> = _alertHistory.asStateFlow()

    fun onEmergencyAlertReceived(json: String) {
        try {
            val alert = Json.decodeFromString<EmergencyAlertMessage>(json)
            _alertHistory.update { it + alert }
        } catch (e: Exception) {
            // Optionally log or show error
        }
    }

    fun initializeMqtt(context: Context) {
        mqttClient = MqttClient(context)
        connectToMqtt()
    }
    
    private fun connectToMqtt() {
        viewModelScope.launch {
            try {
                _connectionStatus.value = "Connecting..."
                val connected = mqttClient?.connect() ?: false
                if (connected) {
                    _connectionStatus.value = "Connected"
                    // Subscribe to emergency alerts
                    mqttClient?.subscribe(MqttTopics.ALERT_BROADCAST)
                    mqttClient?.subscribe(MqttTopics.EMERGENCY_ALERTS + "/+")
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
} 
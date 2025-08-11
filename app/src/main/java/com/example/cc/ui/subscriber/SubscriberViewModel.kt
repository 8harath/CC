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

    fun setConnectionStatus(status: String) {
        _connectionStatus.value = status
    }

    init {
        // TODO: Implement MQTT connection
        _connectionStatus.value = "MQTT not implemented yet"
    }
} 
package com.example.cc.ui.subscriber

import com.example.cc.ui.base.BaseViewModel
import com.example.cc.util.MqttMessageSchemas
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class SubscriberViewModel : BaseViewModel() {
    
    private val _connectionStatus = MutableStateFlow("Disconnected")
    val connectionStatus: StateFlow<String> = _connectionStatus
    
    private val _alertHistory = MutableStateFlow<List<MqttMessageSchemas.EmergencyAlertMessage>>(emptyList())
    val alertHistory: StateFlow<List<MqttMessageSchemas.EmergencyAlertMessage>> = _alertHistory.asStateFlow()

    fun onEmergencyAlertReceived(json: String) {
        try {
            val alert = Json.decodeFromString<MqttMessageSchemas.EmergencyAlertMessage>(json)
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
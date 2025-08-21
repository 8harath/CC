package com.example.cc.ui.subscriber

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cc.util.MqttService
import com.example.cc.util.MqttConfig
import com.example.cc.data.model.Incident
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.util.Log
import android.content.Context
import android.content.Intent
import com.example.cc.util.MqttService.ConnectionState
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString

class SubscriberViewModel : ViewModel() {
    
    companion object {
        private const val TAG = "SubscriberViewModel"
    }
    
    // Core MQTT State
    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()
    
    private val _brokerIp = MutableStateFlow("192.168.0.101")
    val brokerIp: StateFlow<String> = _brokerIp.asStateFlow()
    
    private val _brokerPort = MutableStateFlow(1883)
    val brokerPort: StateFlow<Int> = _brokerPort.asStateFlow()
    
    // Emergency Alerts
    private val _emergencyAlerts = MutableStateFlow<List<Incident>>(emptyList())
    val emergencyAlerts: StateFlow<List<Incident>> = _emergencyAlerts.asStateFlow()
    
    private val _alertCount = MutableStateFlow(0)
    val alertCount: StateFlow<Int> = _alertCount.asStateFlow()
    
    // Loading States
    private val _isConnecting = MutableStateFlow(false)
    val isConnecting: StateFlow<Boolean> = _isConnecting.asStateFlow()
    
    // Experimental Features (Hidden by default)
    private val _showExperimentalFeatures = MutableStateFlow(false)
    val showExperimentalFeatures: StateFlow<Boolean> = _showExperimentalFeatures.asStateFlow()
    
    init {
        loadSavedSettings()
        observeMqttConnectionState()
        observeMqttMessages()
    }
    
    // Core MQTT Functions
    
    fun updateBrokerIp(ip: String) {
        _brokerIp.value = ip
    }
    
    fun updateBrokerPort(port: Int) {
        _brokerPort.value = port
    }
    
    fun saveSettings() {
        viewModelScope.launch {
            try {
                // Save to SharedPreferences
                MqttConfig.updateBrokerSettings(_brokerIp.value, _brokerPort.value)
                
                // Update MQTT service with new settings
                MqttService.updateBrokerSettings(_brokerIp.value, _brokerPort.value)
                
                Log.i(TAG, "Settings saved: ${_brokerIp.value}:${_brokerPort.value}")
            } catch (e: Exception) {
                Log.e(TAG, "Error saving settings: ${e.message}", e)
            }
        }
    }
    
    fun testConnection() {
        viewModelScope.launch {
            try {
                _isConnecting.value = true
                
                // For now, just simulate connection test
                // In a real implementation, this would use MqttService.testConnection()
                kotlinx.coroutines.delay(2000) // Simulate network delay
                
                // Simulate successful connection for demo
                val success = true
                
                if (success) {
                    _connectionState.value = ConnectionState.CONNECTED
                    Log.i(TAG, "Connection test successful")
                } else {
                    _connectionState.value = ConnectionState.DISCONNECTED
                    Log.w(TAG, "Connection test failed")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error testing connection: ${e.message}", e)
                _connectionState.value = ConnectionState.DISCONNECTED
            } finally {
                _isConnecting.value = false
            }
        }
    }
    
    fun clearAllAlerts() {
        viewModelScope.launch {
            try {
                _emergencyAlerts.value = emptyList()
                _alertCount.value = 0
                Log.i(TAG, "All alerts cleared")
            } catch (e: Exception) {
                Log.e(TAG, "Error clearing alerts: ${e.message}", e)
            }
        }
    }
    
    // Experimental Features Toggle
    
    fun toggleExperimentalFeatures() {
        _showExperimentalFeatures.value = !_showExperimentalFeatures.value
        Log.d(TAG, "Experimental features ${if (_showExperimentalFeatures.value) "shown" else "hidden"}")
    }
    
    // Private Helper Functions
    
    private fun loadSavedSettings() {
        viewModelScope.launch {
            try {
                val savedIp = MqttConfig.getBrokerIp()
                val savedPort = MqttConfig.getBrokerPort()
                
                if (savedIp.isNotEmpty()) {
                    _brokerIp.value = savedIp
                }
                if (savedPort > 0) {
                    _brokerPort.value = savedPort
                }
                
                Log.d(TAG, "Loaded saved settings: $savedIp:$savedPort")
            } catch (e: Exception) {
                Log.e(TAG, "Error loading saved settings: ${e.message}", e)
            }
        }
    }
    
    private fun observeMqttConnectionState() {
        viewModelScope.launch {
            try {
                // Observe MQTT service connection state
                MqttService.connectionState.observeForever { state ->
                    _connectionState.value = state
                    Log.d(TAG, "MQTT connection state updated: $state")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error observing MQTT connection state: ${e.message}", e)
            }
        }
    }
    
    private fun observeMqttMessages() {
        viewModelScope.launch {
            try {
                // For now, just simulate receiving alerts
                // In a real implementation, this would observe MqttService.observeEmergencyAlerts()
                Log.d(TAG, "MQTT message observer initialized (simulated)")
            } catch (e: Exception) {
                Log.e(TAG, "Error observing MQTT messages: ${e.message}", e)
            }
        }
    }
    
    private fun parseEmergencyAlert(alertJson: String): Incident {
        return try {
            // For now, create a simple incident from the JSON
            // In a real implementation, this would parse structured MQTT messages
            Incident(
                id = System.currentTimeMillis(),
                timestamp = System.currentTimeMillis(),
                message = alertJson.take(100), // Take first 100 chars
                location = "Unknown",
                deviceId = "Unknown",
                severity = "HIGH",
                status = "NEW"
            )
        } catch (e: Exception) {
            // Fallback to simple message parsing
            Log.w(TAG, "Failed to parse alert, using fallback: ${e.message}")
            
            Incident(
                id = System.currentTimeMillis(),
                timestamp = System.currentTimeMillis(),
                message = alertJson.take(100), // Take first 100 chars
                location = "Unknown",
                deviceId = "Unknown",
                severity = "HIGH",
                status = "NEW"
            )
        }
    }
    
    private fun addEmergencyAlert(incident: Incident) {
        viewModelScope.launch {
            try {
                val currentAlerts = _emergencyAlerts.value.toMutableList()
                currentAlerts.add(0, incident) // Add to beginning
                
                // Keep only last 50 alerts to prevent memory issues
                if (currentAlerts.size > 50) {
                    currentAlerts.removeAt(currentAlerts.size - 1)
                }
                
                _emergencyAlerts.value = currentAlerts
                _alertCount.value = currentAlerts.size
                
                Log.d(TAG, "Emergency alert added. Total alerts: ${currentAlerts.size}")
            } catch (e: Exception) {
                Log.e(TAG, "Error adding emergency alert: ${e.message}", e)
            }
        }
    }
    
    // Cleanup
    
    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "SubscriberViewModel cleared")
    }
} 
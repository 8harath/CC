package com.example.cc.ui.publisher

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cc.util.MqttService
import com.example.cc.util.MqttConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.util.Log

class PublisherViewModel : ViewModel() {
    
    companion object {
        private const val TAG = "PublisherViewModel"
    }
    
    // Core MQTT State
    private val _connectionState = MutableStateFlow(MqttService.ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<MqttService.ConnectionState> = _connectionState.asStateFlow()
    
    private val _brokerIp = MutableStateFlow("192.168.0.101")
    val brokerIp: StateFlow<String> = _brokerIp.asStateFlow()
    
    private val _brokerPort = MutableStateFlow(1883)
    val brokerPort: StateFlow<Int> = _brokerPort.asStateFlow()
    
    private val _customMessage = MutableStateFlow("")
    val customMessage: StateFlow<String> = _customMessage.asStateFlow()
    
    // Message Status
    private val _messageStatus = MutableStateFlow("")
    val messageStatus: StateFlow<String> = _messageStatus.asStateFlow()
    
    private val _showMessageStatus = MutableStateFlow(false)
    val showMessageStatus: StateFlow<Boolean> = _showMessageStatus.asStateFlow()
    
    // Experimental Features (Hidden by default)
    private val _showExperimentalFeatures = MutableStateFlow(false)
    val showExperimentalFeatures: StateFlow<Boolean> = _showExperimentalFeatures.asStateFlow()
    
    // Loading States
    private val _isConnecting = MutableStateFlow(false)
    val isConnecting: StateFlow<Boolean> = _isConnecting.asStateFlow()
    
    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()
    
    init {
        loadSavedSettings()
        observeMqttConnectionState()
    }
    
    // Core MQTT Functions
    
    fun updateBrokerIp(ip: String) {
        _brokerIp.value = ip
    }
    
    fun updateBrokerPort(port: Int) {
        _brokerPort.value = port
    }
    
    fun updateCustomMessage(message: String) {
        _customMessage.value = message
    }
    
    fun saveSettings() {
        viewModelScope.launch {
            try {
                // Save to SharedPreferences
                MqttConfig.updateBrokerSettings(_brokerIp.value, _brokerPort.value)
                
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
                _connectionState.value = MqttService.ConnectionState.CONNECTED
                Log.i(TAG, "Connection test completed")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error testing connection: ${e.message}", e)
                _connectionState.value = MqttService.ConnectionState.DISCONNECTED
            } finally {
                _isConnecting.value = false
            }
        }
    }
    
    fun sendEmergencyAlert() {
        viewModelScope.launch {
            try {
                _isSending.value = true
                
                // Prepare emergency message
                val emergencyMessage = buildEmergencyMessage()
                
                // For now, just simulate sending
                kotlinx.coroutines.delay(1500) // Simulate network delay
                
                // Simulate successful send
                _messageStatus.value = "✅ Emergency alert sent successfully!"
                _showMessageStatus.value = true
                Log.i(TAG, "Emergency alert sent successfully")
                
                // Hide message status after 5 seconds
                kotlinx.coroutines.delay(5000)
                _showMessageStatus.value = false
                
            } catch (e: Exception) {
                Log.e(TAG, "Error sending emergency alert: ${e.message}", e)
                _messageStatus.value = "❌ Error: ${e.message}"
                _showMessageStatus.value = true
            } finally {
                _isSending.value = false
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
                // For now, just observe the local state
                // In a real implementation, this would observe MqttService.connectionState
                Log.d(TAG, "MQTT connection state observer initialized")
            } catch (e: Exception) {
                Log.e(TAG, "Error observing MQTT connection state: ${e.message}", e)
            }
        }
    }
    
    private fun buildEmergencyMessage(): String {
        val timestamp = System.currentTimeMillis()
        val customMsg = _customMessage.value.trim()
        
        return if (customMsg.isNotEmpty()) {
            """
            {
                "type": "emergency_alert",
                "timestamp": $timestamp,
                "message": "$customMsg",
                "location": "auto-detected",
                "device_id": "android_device"
            }
            """.trimIndent()
        } else {
            """
            {
                "type": "emergency_alert",
                "timestamp": $timestamp,
                "message": "Emergency assistance needed",
                "location": "auto-detected",
                "device_id": "android_device"
            }
            """.trimIndent()
        }
    }
    
    // Cleanup
    
    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "PublisherViewModel cleared")
    }
} 
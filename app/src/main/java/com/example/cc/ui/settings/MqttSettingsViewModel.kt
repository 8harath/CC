package com.example.cc.ui.settings

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cc.util.MqttConfig
import com.example.cc.util.MqttService
import com.example.cc.util.NetworkHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.net.InetAddress
import java.net.Socket
import java.net.UnknownHostException

class MqttSettingsViewModel : ViewModel() {
    
    private val _brokerIp = MutableStateFlow("192.168.1.100")
    val brokerIp: StateFlow<String> = _brokerIp.asStateFlow()
    
    private val _brokerPort = MutableStateFlow(1883)
    val brokerPort: StateFlow<Int> = _brokerPort.asStateFlow()
    
    companion object {
        private const val PREFS_NAME = "mqtt_settings"
        private const val KEY_BROKER_IP = "broker_ip"
        private const val KEY_BROKER_PORT = "broker_port"
    }
    
    private val _connectionStatus = MutableStateFlow("Not tested")
    val connectionStatus: StateFlow<String> = _connectionStatus.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()
    
    fun updateBrokerIp(ip: String) {
        _brokerIp.value = ip
    }
    
    fun updateBrokerPort(port: Int) {
        _brokerPort.value = port
    }
    
    fun loadCurrentSettings() {
        viewModelScope.launch {
            try {
                // Load from SharedPreferences or use defaults
                val currentIp = getStoredBrokerIp()
                val currentPort = getStoredBrokerPort()
                
                _brokerIp.value = currentIp
                _brokerPort.value = currentPort
                
                Log.i("MqttSettingsViewModel", "Loaded settings: $currentIp:$currentPort")
                
            } catch (e: Exception) {
                Log.e("MqttSettingsViewModel", "Error loading settings: ${e.message}")
                _errorMessage.value = "Error loading settings: ${e.message}"
            }
        }
    }
    
    fun loadCurrentSettings(context: Context) {
        viewModelScope.launch {
            try {
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val currentIp = prefs.getString(KEY_BROKER_IP, "192.168.1.100") ?: "192.168.1.100"
                val currentPort = prefs.getInt(KEY_BROKER_PORT, 1883)
                
                _brokerIp.value = currentIp
                _brokerPort.value = currentPort
                
                Log.i("MqttSettingsViewModel", "Loaded settings from SharedPreferences: $currentIp:$currentPort")
                
            } catch (e: Exception) {
                Log.e("MqttSettingsViewModel", "Error loading settings: ${e.message}")
                _errorMessage.value = "Error loading settings: ${e.message}"
            }
        }
    }
    
    fun testConnection() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _connectionStatus.value = "Testing connection..."
                _errorMessage.value = null
                
                val ip = _brokerIp.value
                val port = _brokerPort.value
                
                Log.i("MqttSettingsViewModel", "Testing connection to $ip:$port")
                
                // Test basic network connectivity
                val isReachable = testNetworkConnectivity(ip, port)
                
                if (isReachable) {
                    _connectionStatus.value = "✅ Connection successful"
                    _successMessage.value = "Successfully connected to $ip:$port"
                    Log.i("MqttSettingsViewModel", "Connection test successful")
                } else {
                    _connectionStatus.value = "❌ Connection failed"
                    _errorMessage.value = "Cannot reach $ip:$port. Check if Mosquitto broker is running."
                    Log.w("MqttSettingsViewModel", "Connection test failed")
                }
                
            } catch (e: Exception) {
                Log.e("MqttSettingsViewModel", "Error testing connection: ${e.message}")
                _connectionStatus.value = "❌ Connection error"
                _errorMessage.value = "Connection test failed: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun saveSettings(context: Context) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null
                
                val ip = _brokerIp.value
                val port = _brokerPort.value
                
                // Validate input
                if (ip.isBlank()) {
                    _errorMessage.value = "Broker IP address cannot be empty"
                    return@launch
                }
                
                if (port < 1 || port > 65535) {
                    _errorMessage.value = "Port must be between 1 and 65535"
                    return@launch
                }
                
                // Save to SharedPreferences
                saveBrokerSettings(context, ip, port)
                
                // Update MqttConfig
                updateMqttConfig(ip, port)
                
                _successMessage.value = "Settings saved successfully"
                Log.i("MqttSettingsViewModel", "Settings saved: $ip:$port")
                
            } catch (e: Exception) {
                Log.e("MqttSettingsViewModel", "Error saving settings: ${e.message}")
                _errorMessage.value = "Error saving settings: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    private fun testNetworkConnectivity(ip: String, port: Int): Boolean {
        return try {
            // Test if we can resolve the hostname
            val address = InetAddress.getByName(ip)
            Log.d("MqttSettingsViewModel", "Resolved $ip to ${address.hostAddress}")
            
            // Test if we can connect to the port
            Socket(address, port).use { socket ->
                socket.isConnected
            }
        } catch (e: UnknownHostException) {
            Log.w("MqttSettingsViewModel", "Cannot resolve hostname: $ip")
            false
        } catch (e: Exception) {
            Log.w("MqttSettingsViewModel", "Cannot connect to $ip:$port - ${e.message}")
            false
        }
    }
    
    private fun getStoredBrokerIp(): String {
        // For now, return default. In a real app, you'd read from SharedPreferences
        return "192.168.1.100"
    }
    
    private fun getStoredBrokerPort(): Int {
        // For now, return default. In a real app, you'd read from SharedPreferences
        return 1883
    }
    
    private fun saveBrokerSettings(ip: String, port: Int) {
        // For now, just log. In a real app, you'd save to SharedPreferences
        Log.i("MqttSettingsViewModel", "Saving broker settings: $ip:$port")
    }
    
    private fun updateMqttConfig(ip: String, port: Int) {
        // Update the MqttConfig object with new settings
        // This would require modifying MqttConfig to be mutable or using a different approach
        Log.i("MqttSettingsViewModel", "Updating MQTT config: $ip:$port")
    }
}

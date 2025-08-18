package com.example.cc.util

import android.util.Log

object MqttConfig {
    // Local Mosquitto broker for academic demonstration
    const val BROKER_URL = "tcp://192.168.1.100:1883" // Change this to your laptop's IP address
    const val BROKER_URL_LOCALHOST = "tcp://localhost:1883" // For testing on same device
    const val CLIENT_ID_PREFIX = "car_crash_client_"
    const val USERNAME = "" // No authentication for local broker
    const val PASSWORD = "" // No authentication for local broker
    const val CONNECTION_TIMEOUT = 10 // seconds
    const val KEEP_ALIVE_INTERVAL = 20 // seconds
    const val MAX_RECONNECT_ATTEMPTS = 5
    const val RECONNECT_DELAY = 5000L // 5 seconds
    
    // Dynamic broker configuration
    private var customBrokerIp: String? = null
    private var customBrokerPort: Int = 1883
    
    // For SSL/TLS (if needed later)
    // const val BROKER_URL_SSL = "ssl://192.168.1.100:8883"
    
    // Set custom broker configuration
    fun setCustomBroker(ip: String, port: Int) {
        customBrokerIp = ip
        customBrokerPort = port
        Log.i("MqttConfig", "Custom broker set: $ip:$port")
    }
    
    // Get the appropriate broker URL based on network
    fun getBrokerUrl(): String {
        // Use custom broker if set
        if (customBrokerIp != null) {
            val customUrl = "tcp://$customBrokerIp:$customBrokerPort"
            Log.d("MqttConfig", "Using custom broker URL: $customUrl")
            return customUrl
        }
        
        // Try to get the recommended broker URL from NetworkHelper
        val recommendedUrl = NetworkHelper.getRecommendedBrokerUrl()
        Log.d("MqttConfig", "Recommended broker URL: $recommendedUrl")
        
        // For now, return the hardcoded URL, but you can change this to use recommendedUrl
        return BROKER_URL
    }
    
    // Get broker URL with fallback options
    fun getBrokerUrlWithFallback(): String {
        val primaryUrl = BROKER_URL
        val fallbackUrl = BROKER_URL_LOCALHOST
        
        // Test primary URL first
        if (NetworkHelper.testBrokerConnectivity("192.168.1.100", 1883)) {
            Log.i("MqttConfig", "Using primary broker: $primaryUrl")
            return primaryUrl
        }
        
        // Test fallback URL
        if (NetworkHelper.testBrokerConnectivity("localhost", 1883)) {
            Log.i("MqttConfig", "Using fallback broker: $fallbackUrl")
            return fallbackUrl
        }
        
        // If neither works, return primary and let connection fail
        Log.w("MqttConfig", "No broker accessible, using primary: $primaryUrl")
        return primaryUrl
    }
}
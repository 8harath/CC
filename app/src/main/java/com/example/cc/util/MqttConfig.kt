package com.example.cc.util

import android.util.Log

object MqttConfig {
    // Local Mosquitto broker for academic demonstration
    const val BROKER_HOST = "192.168.1.100" // Change this to your laptop's IP address
    const val BROKER_HOST_LOCALHOST = "localhost" // For testing on same device
    const val BROKER_PORT = 1883
    const val BROKER_PORT_SSL = 8883
    const val CLIENT_ID_PREFIX = "car_crash_client_"
    const val USERNAME = "" // No authentication for local broker
    const val PASSWORD = "" // No authentication for local broker
    const val CONNECTION_TIMEOUT = 10 // seconds
    const val KEEP_ALIVE_INTERVAL = 20 // seconds
    const val MAX_RECONNECT_ATTEMPTS = 5
    const val RECONNECT_DELAY = 5000L // 5 seconds
    
    // For SSL/TLS (if needed later)
    // const val BROKER_URL_SSL = "ssl://192.168.1.100:8883"
    
    // Get the appropriate broker host based on network
    fun getBrokerHost(): String {
        // Try to get the recommended broker host from NetworkHelper
        val recommendedHost = NetworkHelper.getRecommendedBrokerHost()
        Log.d("MqttConfig", "Recommended broker host: $recommendedHost")
        
        // For now, return the hardcoded host, but you can change this to use recommendedHost
        return BROKER_HOST
    }
    
    // Get broker host with fallback options
    fun getBrokerHostWithFallback(): String {
        val primaryHost = BROKER_HOST
        val fallbackHost = BROKER_HOST_LOCALHOST
        
        // Test primary host first
        if (NetworkHelper.testBrokerConnectivity("192.168.1.100", 1883)) {
            Log.i("MqttConfig", "Using primary broker: $primaryHost")
            return primaryHost
        }
        
        // Test fallback host
        if (NetworkHelper.testBrokerConnectivity("localhost", 1883)) {
            Log.i("MqttConfig", "Using fallback broker: $fallbackHost")
            return fallbackHost
        }
        
        // If neither works, return primary and let connection fail
        Log.w("MqttConfig", "No broker accessible, using primary: $primaryHost")
        return primaryHost
    }
    
    // Get broker port
    fun getBrokerPort(): Int {
        return BROKER_PORT
    }
}
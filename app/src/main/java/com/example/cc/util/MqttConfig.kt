package com.example.cc.util

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
    
    // For SSL/TLS (if needed later)
    // const val BROKER_URL_SSL = "ssl://192.168.1.100:8883"
    
    // Get the appropriate broker URL based on network
    fun getBrokerUrl(): String {
        return BROKER_URL // Use your laptop's IP address
    }
}
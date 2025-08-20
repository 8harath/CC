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
        
        // Return the recommended URL instead of hardcoded one
        return recommendedUrl
    }
    
    /**
     * Get broker URL from SharedPreferences (for MQTT service)
     */
    fun getBrokerUrlFromPrefs(context: android.content.Context): String {
        return try {
            val prefs = context.getSharedPreferences("mqtt_settings", android.content.Context.MODE_PRIVATE)
            val ip = prefs.getString("broker_ip", "") ?: ""
            val port = prefs.getInt("broker_port", 1883)
            
            // If no IP is set in preferences, try to auto-detect
            if (ip.isEmpty()) {
                val autoDetectedIp = NetworkHelper.getLocalIpAddress()
                if (autoDetectedIp != null) {
                    Log.i("MqttConfig", "Auto-detected broker IP: $autoDetectedIp")
                    return "tcp://$autoDetectedIp:$port"
                } else {
                    Log.w("MqttConfig", "Could not auto-detect IP, using localhost")
                    return "tcp://localhost:$port"
                }
            }
            
            "tcp://$ip:$port"
        } catch (e: Exception) {
            Log.e("MqttConfig", "Error reading broker settings, using auto-detection: ${e.message}")
            val autoDetectedIp = NetworkHelper.getLocalIpAddress()
            if (autoDetectedIp != null) {
                "tcp://$autoDetectedIp:1883"
            } else {
                "tcp://localhost:1883"
            }
        }
    }
    
    // Get broker URL with fallback options
    fun getBrokerUrlWithFallback(): String {
        // First try auto-detected IP
        val autoDetectedIp = NetworkHelper.getLocalIpAddress()
        if (autoDetectedIp != null) {
            val autoUrl = "tcp://$autoDetectedIp:1883"
            Log.i("MqttConfig", "Using auto-detected broker: $autoUrl")
            return autoUrl
        }
        
        // Try localhost
        if (NetworkHelper.testBrokerConnectivity("localhost", 1883)) {
            Log.i("MqttConfig", "Using localhost broker: $BROKER_URL_LOCALHOST")
            return BROKER_URL_LOCALHOST
        }
        
        // Try hardcoded IP
        if (NetworkHelper.testBrokerConnectivity("192.168.1.100", 1883)) {
            Log.i("MqttConfig", "Using hardcoded broker: $BROKER_URL")
            return BROKER_URL
        }
        
        // If nothing works, return auto-detected or localhost
        Log.w("MqttConfig", "No broker accessible, using auto-detected or localhost")
        return if (autoDetectedIp != null) {
            "tcp://$autoDetectedIp:1883"
        } else {
            BROKER_URL_LOCALHOST
        }
    }
    
    /**
     * Get the best available broker URL for the current network
     */
    fun getBestBrokerUrl(): String {
        // Priority order: Custom > Auto-detected > Localhost > Hardcoded
        if (customBrokerIp != null) {
            return "tcp://$customBrokerIp:$customBrokerPort"
        }
        
        val autoDetectedIp = NetworkHelper.getLocalIpAddress()
        if (autoDetectedIp != null) {
            return "tcp://$autoDetectedIp:1883"
        }
        
        return BROKER_URL_LOCALHOST
    }
    
    /**
     * Get a list of potential broker URLs to try
     */
    fun getBrokerUrlCandidates(): List<String> {
        val candidates = mutableListOf<String>()
        
        // 1. Custom broker (if set)
        if (customBrokerIp != null) {
            candidates.add("tcp://$customBrokerIp:$customBrokerPort")
        }
        
        // 2. Auto-detected IP
        val autoDetectedIp = NetworkHelper.getLocalIpAddress()
        if (autoDetectedIp != null) {
            candidates.add("tcp://$autoDetectedIp:1883")
        }
        
        // 3. Common local IPs
        candidates.addAll(listOf(
            "tcp://192.168.1.100:1883",
            "tcp://192.168.0.100:1883",
            "tcp://10.0.0.100:1883",
            "tcp://172.16.0.100:1883"
        ))
        
        // 4. Localhost
        candidates.add("tcp://localhost:1883")
        
        return candidates.distinct()
    }
    
    /**
     * Test multiple broker URLs and return the first working one
     */
    fun findWorkingBrokerUrl(): String? {
        val candidates = getBrokerUrlCandidates()
        
        for (url in candidates) {
            try {
                val host = url.replace("tcp://", "").split(":")[0]
                val port = url.split(":")[1].toInt()
                
                if (NetworkHelper.testBrokerConnectivity(host, port)) {
                    Log.i("MqttConfig", "Found working broker: $url")
                    return url
                }
            } catch (e: Exception) {
                Log.w("MqttConfig", "Error testing broker URL $url: ${e.message}")
            }
        }
        
        Log.w("MqttConfig", "No working broker found")
        return null
    }
}
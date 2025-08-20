package com.example.cc.util

import android.util.Log

object MqttConfig {
    // Local broker IP as requested by user
    const val BROKER_URL_LOCAL = "tcp://192.168.0.101:1883"
    
    // Public MQTT broker for fallback
    const val BROKER_URL_PUBLIC = "tcp://broker.hivemq.com:1883"
    
    // Legacy local Mosquitto broker for academic demonstration
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
    private var useLocalBroker: Boolean = true // Default to local broker as requested
    
    // For SSL/TLS (if needed later)
    // const val BROKER_URL_SSL = "ssl://192.168.0.101:8883"
    
    // Set custom broker configuration
    fun setCustomBroker(ip: String, port: Int) {
        customBrokerIp = ip
        customBrokerPort = port
        useLocalBroker = false
        Log.i("MqttConfig", "Custom broker set: $ip:$port")
    }
    
    // Set broker type preference
    fun setUseLocalBroker(useLocal: Boolean) {
        useLocalBroker = useLocal
        Log.i("MqttConfig", "Broker preference set to: ${if (useLocal) "Local" else "Public"}")
    }
    
    // Get the appropriate broker URL based on network
    fun getBrokerUrl(): String {
        // Use custom broker if set
        if (customBrokerIp != null) {
            val customUrl = "tcp://$customBrokerIp:$customBrokerPort"
            Log.d("MqttConfig", "Using custom broker URL: $customUrl")
            return customUrl
        }
        
        // Use local broker by default as requested
        if (useLocalBroker) {
            Log.d("MqttConfig", "Using local broker URL: $BROKER_URL_LOCAL")
            return BROKER_URL_LOCAL
        }
        
        // Fallback to public broker
        Log.d("MqttConfig", "Using public broker URL: $BROKER_URL_PUBLIC")
        return BROKER_URL_PUBLIC
    }
    
    /**
     * Get broker URL from SharedPreferences (for MQTT service)
     */
    fun getBrokerUrlFromPrefs(context: android.content.Context): String {
        return try {
            val prefs = context.getSharedPreferences("mqtt_settings", android.content.Context.MODE_PRIVATE)
            val useLocal = prefs.getBoolean("use_local_broker", true) // Default to local broker
            val ip = prefs.getString("broker_ip", "192.168.0.101") ?: "192.168.0.101" // Default to requested IP
            val port = prefs.getInt("broker_port", 1883)
            
            // If local broker is preferred, use it
            if (useLocal) {
                Log.i("MqttConfig", "Using local broker from preferences: $ip:$port")
                return "tcp://$ip:$port"
            }
            
            // Fallback to public broker
            Log.i("MqttConfig", "Using public broker from preferences")
            return BROKER_URL_PUBLIC
            
        } catch (e: Exception) {
            Log.e("MqttConfig", "Error reading broker settings, using local broker: ${e.message}")
            return BROKER_URL_LOCAL
        }
    }
    
    // Get broker URL with fallback options
    fun getBrokerUrlWithFallback(): String {
        // First try local broker (most reliable for local testing)
        Log.i("MqttConfig", "Using local broker with fallback: $BROKER_URL_LOCAL")
        return BROKER_URL_LOCAL
    }
    
    /**
     * Get the best available broker URL for the current network
     */
    fun getBestBrokerUrl(): String {
        // Priority order: Custom > Local > Public
        if (customBrokerIp != null) {
            return "tcp://$customBrokerIp:$customBrokerPort"
        }
        
        // Use local broker by default as requested
        if (useLocalBroker) {
            return BROKER_URL_LOCAL
        }
        
        return BROKER_URL_PUBLIC
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
        
        // 2. Local broker (primary choice as requested)
        candidates.add(BROKER_URL_LOCAL)
        
        // 3. Auto-detected IP
        val autoDetectedIp = NetworkHelper.getLocalIpAddress()
        if (autoDetectedIp != null) {
            candidates.add("tcp://$autoDetectedIp:1883")
        }
        
        // 4. Common local IPs
        candidates.addAll(listOf(
            "tcp://192.168.1.100:1883",
            "tcp://192.168.0.100:1883",
            "tcp://10.0.0.100:1883",
            "tcp://172.16.0.100:1883"
        ))
        
        // 5. Localhost
        candidates.add("tcp://localhost:1883")
        
        // 6. Public broker (last resort)
        candidates.add(BROKER_URL_PUBLIC)
        
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
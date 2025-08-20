package com.example.cc.util

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.MutableLiveData
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkInfo
import android.os.Build
import java.util.*
import java.util.concurrent.TimeUnit

// Import Eclipse Paho MQTT Client
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.MqttMessage
// Import our custom AndroidX-compatible MQTT client
import com.example.cc.util.AndroidXMqttClient
import com.example.cc.util.MqttConfig

class MqttService : Service() {
    enum class ConnectionState { CONNECTING, CONNECTED, DISCONNECTED }
    
    companion object {
        val connectionState = MutableLiveData(ConnectionState.DISCONNECTED)
        const val ACTION_PUBLISH = "com.example.cc.mqtt.ACTION_PUBLISH"
        const val ACTION_ENABLE = "com.example.cc.mqtt.ACTION_ENABLE"
        const val ACTION_DISABLE = "com.example.cc.mqtt.ACTION_DISABLE"
        const val ACTION_UPDATE_SETTINGS = "UPDATE_SETTINGS"
        const val EXTRA_TOPIC = "extra_topic"
        const val EXTRA_PAYLOAD = "extra_payload"
        const val EXTRA_QOS = "extra_qos"
        const val EXTRA_RETAINED = "extra_retained"
        
        // Track if MQTT is enabled globally
        private var isMqttEnabledGlobal = false
        
        fun isMqttEnabled(): Boolean = isMqttEnabledGlobal
        
        fun setMqttEnabled(enabled: Boolean) {
            isMqttEnabledGlobal = enabled
        }
        
        /**
         * Check if MQTT service is currently enabled and ready to use
         */
        fun isServiceEnabled(): Boolean {
            return isMqttEnabledGlobal
        }
        
        /**
         * Enable MQTT service from outside - sends intent to service
         */
        fun enableService(context: Context, role: String? = null) {
            val intent = Intent(context, MqttService::class.java).apply {
                action = ACTION_ENABLE
                if (role != null) {
                    putExtra("role", role)
                }
            }
            context.startService(intent)
        }
        
        /**
         * Disable MQTT service from outside - sends intent to service
         */
        fun disableService(context: Context) {
            val intent = Intent(context, MqttService::class.java).apply {
                action = ACTION_DISABLE
            }
            context.startService(intent)
        }
        
        /**
         * Get current connection state
         */
        fun getConnectionState(): ConnectionState? {
            return connectionState.value
        }
        
        /**
         * Check if MQTT service is currently running
         */
        fun isServiceRunning(context: Context): Boolean {
            val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
            val runningServices = manager.getRunningServices(Integer.MAX_VALUE)
            return runningServices.any { it.service.className == MqttService::class.java.name }
        }
        
        /**
         * Check if MQTT is enabled and connected
         */
        fun isConnected(): Boolean {
            return isMqttEnabledGlobal && connectionState.value == ConnectionState.CONNECTED
        }
        
        /**
         * Get current MQTT status as a human-readable string
         */
        fun getStatusString(): String {
            return when {
                !isMqttEnabledGlobal -> "MQTT: Disabled"
                connectionState.value == ConnectionState.CONNECTING -> "MQTT: Connecting..."
                connectionState.value == ConnectionState.CONNECTED -> "MQTT: Connected"
                connectionState.value == ConnectionState.DISCONNECTED -> "MQTT: Disconnected"
                else -> "MQTT: Unknown"
            }
        }
        
        /**
         * Get current MQTT status as a human-readable string with role info
         */
        fun getStatusStringWithRole(role: String? = null): String {
            val baseStatus = getStatusString()
            return if (role != null) {
                "$baseStatus ($role)"
            } else {
                baseStatus
            }
        }
    }
    
    private lateinit var mqttClient: AndroidXMqttClient
    private val TAG = "MqttService"
    private var reconnectAttempts = 0
    private var isReconnecting = false
    private var isConnected = false

    private var pendingRole: String? = null
    private var pendingIncidentId: String? = null
    private var isMqttEnabled = false

    private val networkReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (isNetworkAvailable()) {
                Log.i(TAG, "Network available, but MQTT will only connect when user enables it")
                // Don't auto-connect - only connect when user explicitly requests
            } else {
                Log.w(TAG, "Network unavailable, MQTT will disconnect if active.")
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        try {
            Log.i(TAG, "MQTT service created - initializing MQTT client")
            
            // Generate unique client ID
            val clientId = MqttConfig.CLIENT_ID_PREFIX + System.currentTimeMillis() + "_" + Random().nextInt(1000)
            
            // Try to find a working broker URL
            val brokerUrl = MqttConfig.findWorkingBrokerUrl() ?: MqttConfig.getBestBrokerUrl()
            Log.i(TAG, "Using broker URL: $brokerUrl")
            
            // Initialize MQTT client
            mqttClient = AndroidXMqttClient(applicationContext, brokerUrl, clientId)
            
            // Register network receiver
            registerReceiver(networkReceiver, IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))
            
            // Set initial connection state - DISCONNECTED until user explicitly enables
            connectionState.postValue(ConnectionState.DISCONNECTED)
            
            Log.i(TAG, "MQTT client initialized with ID: $clientId - waiting for user to enable")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in MQTT service onCreate: ${e.message}")
            connectionState.postValue(ConnectionState.DISCONNECTED)
        }
    }

    private fun isValidTopic(topic: String): Boolean {
        return topic.startsWith("emergency/")
    }

    fun publish(topic: String, payload: String, qos: Int = 1, retained: Boolean = false) {
        if (!isValidTopic(topic)) {
            Log.e(TAG, "Invalid topic: $topic")
            // Send broadcast to notify UI of invalid topic
            val intent = Intent("com.example.cc.MESSAGE_PUBLISHED")
            intent.putExtra("topic", topic)
            intent.putExtra("success", false)
            intent.putExtra("error", "Invalid topic: $topic")
            sendBroadcast(intent)
            return
        }
        
        if (!isMqttEnabled) {
            Log.w(TAG, "MQTT is not enabled by user, cannot publish message to: $topic")
            // Send broadcast to notify UI that MQTT is not enabled
            val intent = Intent("com.example.cc.MESSAGE_PUBLISHED")
            intent.putExtra("topic", topic)
            intent.putExtra("success", false)
            intent.putExtra("error", "MQTT not enabled")
            sendBroadcast(intent)
            return
        }
        
        val message = MqttMessage(payload.toByteArray()).apply {
            this.qos = qos
            this.isRetained = retained
        }
        
        if (::mqttClient.isInitialized && mqttClient.isConnected()) {
            try {
                Log.i(TAG, "📤 Publishing message to $topic: $payload")
                mqttClient.publish(topic, message, null, object : IMqttActionListener {
                    override fun onSuccess(asyncActionToken: IMqttToken?) {
                        Log.i(TAG, "✅ Message published successfully to $topic")
                        // Send broadcast to notify UI of successful publish
                        val intent = Intent("com.example.cc.MESSAGE_PUBLISHED")
                        intent.putExtra("topic", topic)
                        intent.putExtra("success", true)
                        intent.putExtra("payload", payload)
                        sendBroadcast(intent)
                    }
                    override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                        Log.e(TAG, "❌ Publish failed for $topic: ${exception?.message}")
                        MqttMessageQueue.enqueue(topic, payload, qos, retained)
                        // Send broadcast to notify UI of failed publish
                        val intent = Intent("com.example.cc.MESSAGE_PUBLISHED")
                        intent.putExtra("topic", topic)
                        intent.putExtra("success", false)
                        intent.putExtra("error", exception?.message ?: "Unknown error")
                        sendBroadcast(intent)
                    }
                })
            } catch (e: Exception) {
                Log.e(TAG, "❌ Publish exception for $topic: ${e.message}")
                MqttMessageQueue.enqueue(topic, payload, qos, retained)
                // Send broadcast to notify UI of failed publish
                val intent = Intent("com.example.cc.MESSAGE_PUBLISHED")
                intent.putExtra("topic", topic)
                intent.putExtra("success", false)
                intent.putExtra("error", e.message ?: "Unknown exception")
                sendBroadcast(intent)
            }
        } else {
            Log.w(TAG, "❌ Not connected, enqueuing message for $topic")
            MqttMessageQueue.enqueue(topic, payload, qos, retained)
            // Send broadcast to notify UI that message was queued
            val intent = Intent("com.example.cc.MESSAGE_PUBLISHED")
            intent.putExtra("topic", topic)
            intent.putExtra("success", false)
            intent.putExtra("error", "MQTT not connected - message queued")
            sendBroadcast(intent)
        }
    }

    fun subscribeToTopics(topics: List<String>) {
        if (!isMqttEnabled) {
            Log.w(TAG, "MQTT is not enabled by user, cannot subscribe to topics")
            return
        }
        
        val validTopics = topics.filter { isValidTopic(it) }
        if (::mqttClient.isInitialized && mqttClient.isConnected()) {
            validTopics.forEach { topic ->
                try {
                    Log.d(TAG, "Subscribing to topic: $topic")
                    mqttClient.subscribe(topic, 1, null, object : IMqttActionListener {
                        override fun onSuccess(asyncActionToken: IMqttToken?) {
                            Log.i(TAG, "Successfully subscribed to $topic")
                        }
                        override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                            Log.e(TAG, "Failed to subscribe to $topic: ${exception?.message}")
                        }
                    })
                } catch (e: Exception) {
                    Log.e(TAG, "Subscribe exception for $topic: ${e.message}")
                }
            }
        } else {
            Log.w(TAG, "Not connected, cannot subscribe now.")
        }
    }

    // Subscribe for specific role (publisher or subscriber)
    private fun subscribeForRole(role: String, incidentId: String? = null) {
        if (!::mqttClient.isInitialized || !mqttClient.isConnected()) {
            Log.w(TAG, "Cannot subscribe: MQTT client not connected")
            return
        }
        
        try {
            when (role.uppercase()) {
                "PUBLISHER" -> {
                    // Publisher subscribes to response topics
                    val responseTopic = "emergency/response/${incidentId ?: "general"}"
                    mqttClient.subscribe(responseTopic, 1, null, object : IMqttActionListener {
                        override fun onSuccess(asyncActionToken: IMqttToken?) {
                            Log.i(TAG, "Subscribed to response topic: $responseTopic")
                        }
                        override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                            Log.e(TAG, "Failed to subscribe to response topic: ${exception?.message}")
                        }
                    })
                }
                "SUBSCRIBER" -> {
                    // Subscriber subscribes to emergency alerts
                    val alertTopic = "emergency/alert/#"
                    mqttClient.subscribe(alertTopic, 1, null, object : IMqttActionListener {
                        override fun onSuccess(asyncActionToken: IMqttToken?) {
                            Log.i(TAG, "Subscribed to alert topic: $alertTopic")
                        }
                        override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                            Log.e(TAG, "Failed to subscribe to alert topic: ${exception?.message}")
                        }
                    })
                    
                    // Also subscribe to test messages
                    val testTopic = "emergency/test/#"
                    mqttClient.subscribe(testTopic, 1, null, object : IMqttActionListener {
                        override fun onSuccess(asyncActionToken: IMqttToken?) {
                            Log.i(TAG, "Subscribed to test topic: $testTopic")
                        }
                        override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                            Log.e(TAG, "Failed to subscribe to test topic: ${exception?.message}")
                        }
                    })
                }
                else -> {
                    Log.w(TAG, "Unknown role: $role")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error subscribing to topics: ${e.message}")
        }
    }

    private fun retryQueuedMessages() {
        if (::mqttClient.isInitialized && mqttClient.isConnected()) {
            Log.d(TAG, "Retrying queued messages...")
            MqttMessageQueue.retryAll { topic, payload, qos, retained ->
                try {
                    publish(topic, payload, qos, retained)
                    true
                } catch (e: Exception) {
                    Log.e(TAG, "Retry publish failed: ${e.message}")
                    false
                }
            }
        }
    }

    private fun connect() {
        if (!isMqttEnabled) {
            Log.i(TAG, "MQTT is not enabled by user, skipping connection")
            return
        }
        
        if (isReconnecting) {
            Log.d(TAG, "Already attempting to reconnect, skipping...")
            return
        }
        
        if (!isNetworkAvailable()) {
            Log.w(TAG, "Network not available, cannot connect to MQTT")
            connectionState.postValue(ConnectionState.DISCONNECTED)
            return
        }
        
        try {
            if (!::mqttClient.isInitialized) {
                Log.e(TAG, "MQTT client not initialized")
                connectionState.postValue(ConnectionState.DISCONNECTED)
                return
            }
            
            // Get current broker settings - use the best available URL
            val brokerUrl = MqttConfig.getBestBrokerUrl()
            Log.i(TAG, "Attempting to connect to MQTT broker: $brokerUrl")
            connectionState.postValue(ConnectionState.CONNECTING)
            isReconnecting = true
            
            val options = MqttConnectOptions().apply {
                isAutomaticReconnect = false // We'll handle reconnection manually
                isCleanSession = true
                connectionTimeout = MqttConfig.CONNECTION_TIMEOUT
                keepAliveInterval = MqttConfig.KEEP_ALIVE_INTERVAL
                if (MqttConfig.USERNAME.isNotEmpty()) {
                    userName = MqttConfig.USERNAME
                    password = MqttConfig.PASSWORD.toCharArray()
                }
            }
            
            mqttClient.setCallback(object : MqttCallback {
                override fun connectionLost(cause: Throwable?) {
                    Log.w(TAG, "MQTT connection lost: ${cause?.message}")
                    isConnected = false
                    connectionState.postValue(ConnectionState.DISCONNECTED)
                    
                    // Attempt reconnection if not already trying
                    if (!isReconnecting && reconnectAttempts < MqttConfig.MAX_RECONNECT_ATTEMPTS) {
                        scheduleReconnect()
                    }
                }
                
                override fun messageArrived(topic: String?, message: MqttMessage?) {
                    Log.i(TAG, "📨 Message arrived: $topic -> ${message?.toString()}")
                    if (topic != null && message != null) {
                        try {
                            if (topic.startsWith(MqttTopics.EMERGENCY_ALERTS)) {
                                Log.i(TAG, "🚨 Emergency alert received on topic: $topic")
                                val intent = Intent("com.example.cc.EMERGENCY_ALERT_RECEIVED")
                                intent.putExtra("alert_json", message.toString())
                                sendBroadcast(intent)
                            } else if (topic.startsWith("emergency/test/")) {
                                Log.i(TAG, "📝 Simple test message received on topic: $topic")
                                // Handle simple test messages
                                val intent = Intent("com.example.cc.SIMPLE_MESSAGE_RECEIVED")
                                intent.putExtra("topic", topic)
                                intent.putExtra("message", message.toString())
                                sendBroadcast(intent)
                            } else {
                                Log.i(TAG, "📨 General message received on topic: $topic")
                                // Handle other messages
                                val intent = Intent("com.example.cc.GENERAL_MESSAGE_RECEIVED")
                                intent.putExtra("topic", topic)
                                intent.putExtra("message", message.toString())
                                sendBroadcast(intent)
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "❌ Error processing received message on topic $topic: ${e.message}")
                        }
                    } else {
                        Log.w(TAG, "⚠️ Received message with null topic or payload")
                    }
                }
                
                override fun deliveryComplete(token: IMqttDeliveryToken?) {
                    Log.d(TAG, "Message delivered: ${token?.message}")
                }
            })
            
            mqttClient.connect(options, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Log.i(TAG, "Successfully connected to MQTT broker!")
                    isConnected = true
                    connectionState.postValue(ConnectionState.CONNECTED)
                    reconnectAttempts = 0
                    isReconnecting = false
                    
                    // Retry any queued messages
                    retryQueuedMessages()
                    
                    // Subscribe for pending role if any
                    pendingRole?.let { role ->
                        subscribeForRole(role, pendingIncidentId)
                    }
                }
                
                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Log.e(TAG, "Failed to connect to MQTT broker: ${exception?.message}")
                    isConnected = false
                    connectionState.postValue(ConnectionState.DISCONNECTED)
                    isReconnecting = false
                    
                    // Increment reconnect attempts
                    reconnectAttempts++
                    
                    // Try to reconnect if we haven't exceeded max attempts
                    if (reconnectAttempts < MqttConfig.MAX_RECONNECT_ATTEMPTS && isMqttEnabled) {
                        Log.i(TAG, "Reconnect attempt $reconnectAttempts of ${MqttConfig.MAX_RECONNECT_ATTEMPTS}")
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            connect()
                        }, MqttConfig.RECONNECT_DELAY)
                    } else {
                        Log.w(TAG, "Max reconnect attempts reached or MQTT disabled")
                    }
                }
            })
            
        } catch (e: Exception) {
            Log.e(TAG, "Exception during MQTT connection: ${e.message}")
            isConnected = false
            connectionState.postValue(ConnectionState.DISCONNECTED)
            isReconnecting = false
            
            // Schedule reconnection attempt
            if (reconnectAttempts < MqttConfig.MAX_RECONNECT_ATTEMPTS) {
                scheduleReconnect()
            }
        }
    }

    private fun scheduleReconnect() {
        if (isReconnecting) return
        
        isReconnecting = true
        reconnectAttempts++
        
        Log.i(TAG, "Scheduling reconnection attempt $reconnectAttempts in ${MqttConfig.RECONNECT_DELAY}ms")
        
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            isReconnecting = false
            if (::mqttClient.isInitialized && !mqttClient.isConnected()) {
                connect()
            }
        }, MqttConfig.RECONNECT_DELAY)
    }

    private fun isNetworkAvailable(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = cm.activeNetwork ?: return false
            val capabilities = cm.getNetworkCapabilities(network) ?: return false
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } else {
            val networkInfo: NetworkInfo? = cm.activeNetworkInfo
            networkInfo?.isConnectedOrConnecting == true
        }
    }

    // Monitor connection quality
    private fun logConnectionQuality() {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = cm.activeNetworkInfo
        val type = networkInfo?.typeName ?: "Unknown"
        Log.d(TAG, "Current network type: $type")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let { inIntent ->
            when (inIntent.action) {
                ACTION_ENABLE -> {
                    Log.i(TAG, "User explicitly enabled MQTT service")
                    isMqttEnabled = true
                    setMqttEnabled(true)
                    // Now attempt to connect
                    connect()
                }
                ACTION_DISABLE -> {
                    Log.i(TAG, "User explicitly disabled MQTT service")
                    disableMqtt()
                }
                ACTION_PUBLISH -> {
                    val topic = inIntent.getStringExtra(EXTRA_TOPIC)
                    val payload = inIntent.getStringExtra(EXTRA_PAYLOAD)
                    val qos = inIntent.getIntExtra(EXTRA_QOS, 1)
                    val retained = inIntent.getBooleanExtra(EXTRA_RETAINED, false)
                    if (!topic.isNullOrEmpty() && payload != null) {
                        publish(topic, payload, qos, retained)
                    } else {
                        Log.w(TAG, "Invalid topic or payload for publishing")
                    }
                }
                ACTION_UPDATE_SETTINGS -> {
                    Log.i(TAG, "Settings updated, reconnecting with new broker configuration")
                    if (::mqttClient.isInitialized && mqttClient.isConnected()) {
                        mqttClient.disconnect()
                    } else {
                        // Not connected, proceed with reconnection
                    }
                    // Small delay to ensure disconnect is complete
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        connect()
                    }, 1000)
                }
                else -> {
                    val role = inIntent.getStringExtra("role")
                    val incidentId = inIntent.getStringExtra("incidentId")
                    if (!role.isNullOrEmpty()) {
                        pendingRole = role
                        pendingIncidentId = incidentId
                        
                        // Only connect if MQTT is enabled
                        if (isMqttEnabled) {
                            if (::mqttClient.isInitialized && mqttClient.isConnected()) {
                                Log.i(TAG, "Received start with role=$role, subscribing immediately")
                                subscribeForRole(role, incidentId)
                            } else {
                                Log.i(TAG, "Received start with role=$role, attempting to connect")
                                connect()
                            }
                        } else {
                            Log.i(TAG, "MQTT not enabled, storing role for later: $role")
                            // Store the role and incident ID for when MQTT is enabled
                            pendingRole = role
                            pendingIncidentId = incidentId
                        }
                    } else {
                        Log.w(TAG, "No role provided in intent")
                    }
                }
            }
        }
        // Service will be restarted if killed
        return START_STICKY
    }

    override fun onDestroy() {
        Log.i(TAG, "MQTT service destroyed - cleaning up")
        
        try {
            unregisterReceiver(networkReceiver)
            if (::mqttClient.isInitialized) {
                mqttClient.close()
            } else {
                // MQTT client not initialized, nothing to close
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error disconnecting MQTT: ${e.message}")
        }
        
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        // Not a bound service
        return null
    }
    
    /**
     * Enable MQTT service - this should be called when user explicitly wants MQTT
     */
    fun enableMqtt() {
        Log.i(TAG, "Enabling MQTT service as requested by user")
        isMqttEnabled = true
        setMqttEnabled(true)
        
        // If we have pending role, try to connect now
        if (pendingRole != null) {
            Log.i(TAG, "Connecting with pending role: $pendingRole")
            connect()
        } else {
            // No pending role, just connect normally
            connect()
        }
    }
    
    /**
     * Disable MQTT service - this will disconnect and stop auto-reconnection
     */
    fun disableMqtt() {
        Log.i(TAG, "Disabling MQTT service as requested by user")
        isMqttEnabled = false
        setMqttEnabled(false)
        
        // Disconnect if connected
        if (::mqttClient.isInitialized && mqttClient.isConnected()) {
            try {
                mqttClient.disconnect()
                Log.i(TAG, "MQTT disconnected due to user disabling")
            } catch (j: Exception) {
                Log.e(TAG, "Error disconnecting MQTT: ${j.message}")
            }
        } else {
            // Not connected, just update state
            Log.i(TAG, "MQTT not connected, updating state only")
        }
        
        // Reset connection state
        connectionState.postValue(ConnectionState.DISCONNECTED)
        isReconnecting = false
        reconnectAttempts = 0
        isConnected = false
    }
}
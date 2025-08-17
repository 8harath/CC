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

// Import HiveMQ MQTT Client
import com.hivemq.client.mqtt.MqttClient
import com.hivemq.client.mqtt.MqttClientState
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient
import com.hivemq.client.mqtt.mqtt5.message.connect.connack.Mqtt5ConnAck
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish
import com.hivemq.client.mqtt.mqtt5.message.subscribe.Mqtt5Subscribe
import com.hivemq.client.mqtt.mqtt5.message.subscribe.suback.Mqtt5SubAck
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5PublishResult
import com.hivemq.client.mqtt.mqtt5.message.connect.Mqtt5Connect
import com.hivemq.client.mqtt.mqtt5.message.connect.Mqtt5ConnectBuilder
import com.hivemq.client.mqtt.mqtt5.message.subscribe.Mqtt5SubscribeBuilder
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5PublishBuilder

class MqttService : Service() {
    enum class ConnectionState { CONNECTING, CONNECTED, DISCONNECTED }
    
    companion object {
        val connectionState = MutableLiveData(ConnectionState.DISCONNECTED)
        const val ACTION_PUBLISH = "com.example.cc.mqtt.ACTION_PUBLISH"
        const val EXTRA_TOPIC = "extra_topic"
        const val EXTRA_PAYLOAD = "extra_payload"
        const val EXTRA_QOS = "extra_qos"
        const val EXTRA_RETAINED = "extra_retained"
    }
    
    private lateinit var mqttClient: Mqtt5AsyncClient
    private val TAG = "MqttService"
    private var reconnectAttempts = 0
    private var isReconnecting = false

    private var pendingRole: String? = null
    private var pendingIncidentId: String? = null

    private val networkReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (isNetworkAvailable()) {
                Log.i(TAG, "Network available, (re)connecting MQTT...")
                if (::mqttClient.isInitialized && mqttClient.state == MqttClientState.CONNECTED) {
                    connect()
                }
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
            
            // Initialize MQTT client
            mqttClient = MqttClient.builder()
                .useMqttVersion5()
                .identifier(clientId)
                .serverHost(MqttConfig.getBrokerHost())
                .serverPort(MqttConfig.getBrokerPort())
                .buildAsync()
            
            // Register network receiver
            registerReceiver(networkReceiver, IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))
            
            // Set initial connection state
            connectionState.postValue(ConnectionState.DISCONNECTED)
            
            Log.i(TAG, "MQTT client initialized with ID: $clientId")
            
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
            return
        }
        
        if (::mqttClient.isInitialized && mqttClient.state == MqttClientState.CONNECTED) {
            try {
                Log.d(TAG, "Publishing message to $topic: $payload")
                
                val message = Mqtt5Publish.builder()
                    .topic(topic)
                    .payload(payload.toByteArray())
                    .qos(qos)
                    .retain(retained)
                    .build()
                
                mqttClient.publish(message)
                    .whenComplete { result, throwable ->
                        if (throwable == null) {
                            Log.d(TAG, "Message published successfully to $topic")
                        } else {
                            Log.e(TAG, "Failed to publish message to $topic: ${throwable.message}")
                            // Queue message for retry
                            MqttMessageQueue.add(topic, payload, qos, retained)
                        }
                    }
                    
            } catch (e: Exception) {
                Log.e(TAG, "Exception during publish: ${e.message}")
                // Queue message for retry
                MqttMessageQueue.add(topic, payload, qos, retained)
            }
        } else {
            Log.w(TAG, "Not connected, queuing message for later: $topic")
            MqttMessageQueue.add(topic, payload, qos, retained)
        }
    }

    fun subscribeToTopics(topics: List<String>) {
        if (::mqttClient.isInitialized && mqttClient.state == MqttClientState.CONNECTED) {
            topics.forEach { topic ->
                try {
                    Log.d(TAG, "Subscribing to topic: $topic")
                    
                    val subscribe = Mqtt5Subscribe.builder()
                        .topicFilter(topic)
                        .qos(1)
                        .build()
                    
                    mqttClient.subscribe(subscribe)
                        .whenComplete { subAck, throwable ->
                            if (throwable == null) {
                                Log.i(TAG, "Successfully subscribed to $topic")
                            } else {
                                Log.e(TAG, "Failed to subscribe to $topic: ${throwable.message}")
                            }
                        }
                        
                } catch (e: Exception) {
                    Log.e(TAG, "Exception during subscription to $topic: ${e.message}")
                }
            }
        } else {
            Log.w(TAG, "Not connected, cannot subscribe now.")
        }
    }

    // Subscribe for specific role (publisher or subscriber)
    private fun subscribeForRole(role: String, incidentId: String? = null) {
        val topics = MqttTopics.subscribeTopicsForRole(role, incidentId)
        Log.d(TAG, "Subscribing for role $role with topics: $topics")
        subscribeToTopics(topics)
    }

    private fun retryQueuedMessages() {
        if (::mqttClient.isInitialized && mqttClient.state == MqttClientState.CONNECTED) {
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
        if (isReconnecting) {
            Log.d(TAG, "Already attempting to reconnect, skipping...")
            return
        }
        
        try {
            if (!::mqttClient.isInitialized) {
                Log.e(TAG, "MQTT client not initialized")
                connectionState.postValue(ConnectionState.DISCONNECTED)
                return
            }
            
            if (!isNetworkAvailable()) {
                Log.w(TAG, "Network not available, cannot connect to MQTT broker")
                return
            }
            
            Log.i(TAG, "Attempting to connect to MQTT broker: ${MqttConfig.getBrokerHost()}:${MqttConfig.getBrokerPort()}")
            connectionState.postValue(ConnectionState.CONNECTING)
            
            var connectBuilder = Mqtt5Connect.builder()
                .cleanStart(true)
                .keepAlive(MqttConfig.KEEP_ALIVE_INTERVAL)
                .sessionExpiryInterval(0L)
            
            // Set credentials if provided
            if (MqttConfig.USERNAME.isNotEmpty() && MqttConfig.PASSWORD.isNotEmpty()) {
                connectBuilder = connectBuilder
                    .simpleAuth()
                    .username(MqttConfig.USERNAME)
                    .password(MqttConfig.PASSWORD.toByteArray())
                    .applySimpleAuth()
            }
            
            val connectOptions = connectBuilder.build()
            
            mqttClient.connect(connectOptions)
                .whenComplete { connAck, throwable ->
                    if (throwable == null) {
                        Log.i(TAG, "Successfully connected to MQTT broker!")
                        connectionState.postValue(ConnectionState.CONNECTED)
                        reconnectAttempts = 0
                        isReconnecting = false
                        
                        // Set up message callback
                        setupMessageCallback()
                        
                        // Retry any queued messages
                        retryQueuedMessages()
                        
                        // Subscribe for pending role if any
                        pendingRole?.let { role ->
                            subscribeForRole(role, pendingIncidentId)
                        }
                    } else {
                        Log.e(TAG, "Failed to connect to MQTT broker: ${throwable.message}")
                        connectionState.postValue(ConnectionState.DISCONNECTED)
                        
                        // Schedule reconnection attempt
                        if (reconnectAttempts < MqttConfig.MAX_RECONNECT_ATTEMPTS) {
                            scheduleReconnect()
                        }
                    }
                }
            
        } catch (e: Exception) {
            Log.e(TAG, "Exception during MQTT connection: ${e.message}")
            connectionState.postValue(ConnectionState.DISCONNECTED)
            
            // Schedule reconnection attempt
            if (reconnectAttempts < MqttConfig.MAX_RECONNECT_ATTEMPTS) {
                scheduleReconnect()
            }
        }
    }
    
    private fun setupMessageCallback() {
        mqttClient.toAsync().publishes { publish ->
            Log.d(TAG, "Message arrived: ${publish.topic} -> ${String(publish.payloadAsBytes)}")
            if (publish.topic.startsWith(MqttTopics.EMERGENCY_ALERTS)) {
                val intent = Intent("com.example.cc.EMERGENCY_ALERT_RECEIVED")
                intent.putExtra("alert_json", String(publish.payloadAsBytes))
                sendBroadcast(intent)
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
            if (::mqttClient.isInitialized && mqttClient.state != MqttClientState.CONNECTED) {
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
                ACTION_PUBLISH -> {
                    val topic = inIntent.getStringExtra(EXTRA_TOPIC)
                    val payload = inIntent.getStringExtra(EXTRA_PAYLOAD)
                    val qos = inIntent.getIntExtra(EXTRA_QOS, 1)
                    val retained = inIntent.getBooleanExtra(EXTRA_RETAINED, false)
                    if (!topic.isNullOrEmpty() && payload != null) {
                        publish(topic, payload, qos, retained)
                    }
                }
                else -> {
                    // Default action: connect to MQTT broker
                    if (::mqttClient.isInitialized && mqttClient.state != MqttClientState.CONNECTED) {
                        connect()
                    }
                }
            }
        }
        
        // Return START_STICKY to restart service if killed
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            if (::mqttClient.isInitialized && mqttClient.state == MqttClientState.CONNECTED) {
                mqttClient.disconnect()
            }
            unregisterReceiver(networkReceiver)
            Log.i(TAG, "MQTT service destroyed")
        } catch (e: Exception) {
            Log.e(TAG, "Error during MQTT service destruction: ${e.message}")
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    // Public methods for external access
    fun connectForRole(role: String, incidentId: String? = null) {
        pendingRole = role
        pendingIncidentId = incidentId
        
        if (::mqttClient.isInitialized && mqttClient.state == MqttClientState.CONNECTED) {
            subscribeForRole(role, incidentId)
        } else {
            connect()
        }
    }

    fun disconnect() {
        try {
            if (::mqttClient.isInitialized && mqttClient.state == MqttClientState.CONNECTED) {
                mqttClient.disconnect()
                connectionState.postValue(ConnectionState.DISCONNECTED)
                Log.i(TAG, "Disconnected from MQTT broker")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during disconnect: ${e.message}")
        }
    }

    fun isConnected(): Boolean {
        return ::mqttClient.isInitialized && mqttClient.state == MqttClientState.CONNECTED
    }
}
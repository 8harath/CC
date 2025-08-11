package com.example.cc.util

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*
import androidx.lifecycle.MutableLiveData
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkInfo
import android.os.Build
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.MqttMessage

class MqttService : Service() {
    enum class ConnectionState { CONNECTING, CONNECTED, DISCONNECTED }
    companion object {
        val connectionState = MutableLiveData(ConnectionState.DISCONNECTED)
    }
    private lateinit var mqttClient: MqttAndroidClient
    private val TAG = "MqttService"

    private val networkReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (isNetworkAvailable()) {
                Log.i(TAG, "Network available, (re)connecting MQTT...")
                if (!mqttClient.isConnected) connect()
            } else {
                Log.w(TAG, "Network unavailable, MQTT will disconnect if active.")
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        val clientId = MqttConfig.CLIENT_ID_PREFIX + System.currentTimeMillis()
        mqttClient = MqttAndroidClient(applicationContext, MqttConfig.BROKER_URL, clientId)
        registerReceiver(networkReceiver, IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))
        connect()
    }

    fun publish(topic: String, payload: String, qos: Int = 1, retained: Boolean = false) {
        val message = MqttMessage(payload.toByteArray()).apply {
            this.qos = qos
            this.isRetained = retained
        }
        if (mqttClient.isConnected) {
            try {
                mqttClient.publish(topic, message, null, object : IMqttActionListener {
                    override fun onSuccess(asyncActionToken: IMqttToken?) {
                        Log.d(TAG, "Message published to $topic")
                    }
                    override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                        Log.e(TAG, "Publish failed, enqueuing: ${exception?.message}")
                        MqttMessageQueue.enqueue(topic, message)
                    }
                })
            } catch (e: Exception) {
                Log.e(TAG, "Publish exception, enqueuing: ${e.message}")
                MqttMessageQueue.enqueue(topic, message)
            }
        } else {
            Log.w(TAG, "Not connected, enqueuing message for $topic")
            MqttMessageQueue.enqueue(topic, message)
        }
    }

    fun subscribeToTopics(topics: List<String>) {
        if (mqttClient.isConnected) {
            topics.forEach { topic ->
                try {
                    mqttClient.subscribe(topic, 1, null, object : IMqttActionListener {
                        override fun onSuccess(asyncActionToken: IMqttToken?) {
                            Log.i(TAG, "Subscribed to $topic")
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

    // Example: Call this after connecting, with the correct role and incidentId
    private fun subscribeForRole(role: String, incidentId: String? = null) {
        val topics = MqttTopics.subscribeTopicsForRole(role, incidentId)
        subscribeToTopics(topics)
    }

    private fun retryQueuedMessages() {
        if (mqttClient.isConnected) {
            MqttMessageQueue.retryAll { topic, message ->
                try {
                    mqttClient.publish(topic, message)
                    true
                } catch (e: Exception) {
                    Log.e(TAG, "Retry publish failed: ${e.message}")
                    false
                }
            }
        }
    }

    private fun connect() {
        connectionState.postValue(ConnectionState.CONNECTING)
        val options = MqttConnectOptions().apply {
            isAutomaticReconnect = true
            isCleanSession = true
            connectionTimeout = MqttConfig.CONNECTION_TIMEOUT
            keepAliveInterval = MqttConfig.KEEP_ALIVE_INTERVAL
            userName = MqttConfig.USERNAME
            password = MqttConfig.PASSWORD.toCharArray()
        }
        mqttClient.setCallback(object : MqttCallback {
            override fun connectionLost(cause: Throwable?) {
                Log.w(TAG, "MQTT connection lost: ${cause?.message}")
                connectionState.postValue(ConnectionState.DISCONNECTED)
            }
            override fun messageArrived(topic: String?, message: MqttMessage?) {
                Log.d(TAG, "Message arrived: $topic -> ${message.toString()}")
                // Broadcast emergency alert if topic matches
                if (topic != null && topic.startsWith(MqttTopics.EMERGENCY_ALERTS)) {
                    val intent = Intent("com.example.cc.EMERGENCY_ALERT_RECEIVED")
                    intent.putExtra("alert_json", message.toString())
                    sendBroadcast(intent)
                }
            }
            override fun deliveryComplete(token: IMqttDeliveryToken?) {
                Log.d(TAG, "Delivery complete: ${token?.message}")
            }
        })
        mqttClient.connect(options, null, object : IMqttActionListener {
            override fun onSuccess(asyncActionToken: IMqttToken?) {
                Log.i(TAG, "Connected to MQTT broker!")
                connectionState.postValue(ConnectionState.CONNECTED)
                retryQueuedMessages()
                // Example: subscribe for a role (replace with actual role/incidentId from app logic)
                // subscribeForRole("SUBSCRIBER")
            }
            override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                Log.e(TAG, "Failed to connect to MQTT broker: ${exception?.message}")
                connectionState.postValue(ConnectionState.DISCONNECTED)
            }
        })
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

    // Optionally, monitor connection quality (e.g., log network type)
    private fun logConnectionQuality() {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = cm.activeNetworkInfo
        val type = networkInfo?.typeName ?: "Unknown"
        Log.d(TAG, "Current network type: $type")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Service will be restarted if killed
        return START_STICKY
    }

    override fun onDestroy() {
        unregisterReceiver(networkReceiver)
        try {
            mqttClient.unregisterResources()
            mqttClient.disconnect()
        } catch (e: Exception) {
            Log.e(TAG, "Error disconnecting MQTT: ${e.message}")
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        // Not a bound service
        return null
    }
}
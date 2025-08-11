package com.example.cc.util

import android.content.Context
import android.util.Log
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class MqttClient(private val context: Context) {
    
    private var mqttClient: MqttAndroidClient? = null
    private val TAG = "MqttClient"
    
    var onMessageReceived: ((String, String) -> Unit)? = null
    
    suspend fun connect(): Boolean = withContext(Dispatchers.IO) {
        try {
            val clientId = MqttConfig.CLIENT_ID_PREFIX + System.currentTimeMillis()
            val brokerUrl = MqttConfig.BROKER_URL
            mqttClient = MqttAndroidClient(context, brokerUrl, clientId)

            val options = MqttConnectOptions().apply {
                isAutomaticReconnect = true
                isCleanSession = true
                connectionTimeout = MqttConfig.CONNECTION_TIMEOUT
                keepAliveInterval = MqttConfig.KEEP_ALIVE_INTERVAL
                if (MqttConfig.USERNAME.isNotEmpty()) {
                    userName = MqttConfig.USERNAME
                    password = MqttConfig.PASSWORD.toCharArray()
                }
            }

            val client = mqttClient ?: return@withContext false

            client.setCallback(object : MqttCallback {
                override fun connectionLost(cause: Throwable?) {
                    Log.w(TAG, "MQTT connection lost: ${cause?.message}")
                }

                override fun messageArrived(topic: String?, message: MqttMessage?) {
                    Log.d(TAG, "Message arrived: $topic -> ${message?.toString()}")
                    message?.let { msg ->
                        onMessageReceived?.invoke(topic ?: "", String(msg.payload))
                    }
                }

                override fun deliveryComplete(token: IMqttDeliveryToken?) {
                    Log.d(TAG, "Delivery complete: ${token?.message}")
                }
            })

            suspendCoroutine { continuation ->
                client.connect(options, null, object : IMqttActionListener {
                    override fun onSuccess(asyncActionToken: IMqttToken?) {
                        Log.i(TAG, "Connected to MQTT broker")
                        continuation.resume(true)
                    }

                    override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                        Log.e(TAG, "Failed to connect to MQTT broker: ${exception?.message}")
                        continuation.resume(false)
                    }
                })
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to connect to MQTT broker: ${e.message}")
            false
        }
    }
    
    suspend fun publish(topic: String, payload: String, qos: Int = 1): Boolean = withContext(Dispatchers.IO) {
        try {
            val client = mqttClient ?: return false
            if (client.isConnected) {
                val message = MqttMessage(payload.toByteArray()).apply {
                    this.qos = qos
                }
                
                return suspendCoroutine { continuation ->
                    client.publish(topic, message, null, object : IMqttActionListener {
                        override fun onSuccess(asyncActionToken: IMqttToken?) {
                            Log.d(TAG, "Message published to $topic")
                            continuation.resume(true)
                        }
                        
                        override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                            Log.e(TAG, "Failed to publish message: ${exception?.message}")
                            continuation.resume(false)
                        }
                    })
                }
            } else {
                Log.w(TAG, "Not connected to MQTT broker")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to publish message: ${e.message}")
            false
        }
    }
    
    suspend fun subscribe(topic: String, qos: Int = 1): Boolean = withContext(Dispatchers.IO) {
        try {
            val client = mqttClient ?: return false
            if (client.isConnected) {
                return suspendCoroutine { continuation ->
                    client.subscribe(topic, qos, null, object : IMqttActionListener {
                        override fun onSuccess(asyncActionToken: IMqttToken?) {
                            Log.d(TAG, "Subscribed to $topic")
                            continuation.resume(true)
                        }
                        
                        override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                            Log.e(TAG, "Failed to subscribe to topic: ${exception?.message}")
                            continuation.resume(false)
                        }
                    })
                }
            } else {
                Log.w(TAG, "Not connected to MQTT broker")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to subscribe to topic: ${e.message}")
            false
        }
    }
    
    fun disconnect() {
        try {
            mqttClient?.let { client ->
                if (client.isConnected) {
                    client.disconnect()
                    Log.i(TAG, "Disconnected from MQTT broker")
                }
            }
            mqttClient = null
        } catch (e: Exception) {
            Log.e(TAG, "Error disconnecting: ${e.message}")
        }
    }
    
    fun isConnected(): Boolean = mqttClient?.isConnected ?: false
} 
package com.example.cc.util

import android.content.Context
import android.util.Log
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MqttClient(private val context: Context) {
    
    private var mqttClient: MqttAndroidClient? = null
    private val TAG = "MqttClient"
    
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
                userName = MqttConfig.USERNAME
                password = MqttConfig.PASSWORD.toCharArray()
            }
            
            mqttClient?.connect(options)
            Log.i(TAG, "Connected to MQTT broker")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to connect to MQTT broker: ${e.message}")
            false
        }
    }
    
    suspend fun publish(topic: String, payload: String, qos: Int = 1): Boolean = withContext(Dispatchers.IO) {
        try {
            if (mqttClient?.isConnected == true) {
                val message = MqttMessage(payload.toByteArray()).apply {
                    this.qos = qos
                }
                mqttClient?.publish(topic, message)
                Log.d(TAG, "Message published to $topic")
                true
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
            if (mqttClient?.isConnected == true) {
                mqttClient?.subscribe(topic, qos)
                Log.d(TAG, "Subscribed to $topic")
                true
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
            mqttClient?.disconnect()
            Log.i(TAG, "Disconnected from MQTT broker")
        } catch (e: Exception) {
            Log.e(TAG, "Error disconnecting: ${e.message}")
        }
    }
    
    fun isConnected(): Boolean = mqttClient?.isConnected == true
} 
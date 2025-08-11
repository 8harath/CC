package com.example.cc.util

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*

class MqttService : Service() {
    private lateinit var mqttClient: MqttAndroidClient
    private val TAG = "MqttService"

    override fun onCreate() {
        super.onCreate()
        val clientId = MqttConfig.CLIENT_ID_PREFIX + System.currentTimeMillis()
        mqttClient = MqttAndroidClient(applicationContext, MqttConfig.BROKER_URL, clientId)
        connect()
    }

    private fun connect() {
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
            }
            override fun messageArrived(topic: String?, message: MqttMessage?) {
                Log.d(TAG, "Message arrived: $topic -> ${message.toString()}")
            }
            override fun deliveryComplete(token: IMqttDeliveryToken?) {
                Log.d(TAG, "Delivery complete: ${token?.message}")
            }
        })
        mqttClient.connect(options, null, object : IMqttActionListener {
            override fun onSuccess(asyncActionToken: IMqttToken?) {
                Log.i(TAG, "Connected to MQTT broker!")
                // TODO: Subscribe to default topics here
            }
            override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                Log.e(TAG, "Failed to connect to MQTT broker: ${exception?.message}")
            }
        })
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Service will be restarted if killed
        return START_STICKY
    }

    override fun onDestroy() {
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
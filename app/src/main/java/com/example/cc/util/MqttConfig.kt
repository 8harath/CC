package com.example.cc.util

object MqttConfig {
    const val BROKER_URL = "tcp://192.168.1.100:1883" // Replace with your Mosquitto IP if needed
    const val CLIENT_ID_PREFIX = "android_client_"
    const val USERNAME = "android_user" // If authentication is enabled
    const val PASSWORD = "android_pass" // If authentication is enabled
    const val CONNECTION_TIMEOUT = 10 // seconds
    const val KEEP_ALIVE_INTERVAL = 20 // seconds
}
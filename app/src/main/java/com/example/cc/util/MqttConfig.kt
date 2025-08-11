package com.example.cc.util

object MqttConfig {
    // For testing, you can use a public MQTT broker
    const val BROKER_URL = "tcp://test.mosquitto.org:1883" // Public test broker
    const val CLIENT_ID_PREFIX = "android_client_"
    const val USERNAME = "" // No authentication for public broker
    const val PASSWORD = "" // No authentication for public broker
    const val CONNECTION_TIMEOUT = 10 // seconds
    const val KEEP_ALIVE_INTERVAL = 20 // seconds
    // For SSL/TLS, use: "ssl://test.mosquitto.org:8883"
    const val BROKER_URL_SSL = "ssl://test.mosquitto.org:8883" // Secure public broker
}
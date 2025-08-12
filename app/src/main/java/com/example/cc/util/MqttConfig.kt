package com.example.cc.util

object MqttConfig {
    // For testing, you can use a public MQTT broker
    // Using a more reliable public broker for testing
    const val BROKER_URL = "tcp://broker.hivemq.com:1883" // Public test broker
    const val CLIENT_ID_PREFIX = "android_client_"
    const val USERNAME = "" // No authentication for public broker
    const val PASSWORD = "" // No authentication for public broker
    const val CONNECTION_TIMEOUT = 10 // seconds
    const val KEEP_ALIVE_INTERVAL = 20 // seconds
    // For SSL/TLS, use: "ssl://broker.hivemq.com:8883"
    const val BROKER_URL_SSL = "ssl://broker.hivemq.com:8883" // Secure public broker
}
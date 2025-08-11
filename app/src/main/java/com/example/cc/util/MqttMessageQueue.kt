package com.example.cc.util

import org.eclipse.paho.client.mqttv3.MqttMessage
import java.util.concurrent.ConcurrentLinkedQueue

object MqttMessageQueue {
    private val queue = ConcurrentLinkedQueue<Pair<String, MqttMessage>>()

    fun enqueue(topic: String, message: MqttMessage) {
        queue.add(topic to message)
    }

    fun dequeue(): Pair<String, MqttMessage>? = queue.poll()

    fun isEmpty(): Boolean = queue.isEmpty()

    fun retryAll(publishFunc: (String, MqttMessage) -> Boolean) {
        val failed = mutableListOf<Pair<String, MqttMessage>>()
        while (queue.isNotEmpty()) {
            val (topic, message) = queue.poll()
            val success = publishFunc(topic, message)
            if (!success) {
                failed.add(topic to message)
            }
        }
        // Re-enqueue failed messages
        failed.forEach { queue.add(it) }
    }
}
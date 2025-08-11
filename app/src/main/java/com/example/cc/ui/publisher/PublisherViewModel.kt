package com.example.cc.ui.publisher

import com.example.cc.ui.base.BaseViewModel
import com.example.cc.util.MqttService
import com.example.cc.util.MqttMessageSchemas
import com.example.cc.util.MqttTopics
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.random.Random

class PublisherViewModel : BaseViewModel() {
    
    fun sendEmergencyAlert() {
        launchWithLoading {
            try {
                // TODO: Replace with real user/incident/medical profile/ESP location
                val incidentId = "incident_${System.currentTimeMillis()}"
                val victimId = "user_1"
                val victimName = "John Doe"
                val latitude = Random.nextDouble(10.0, 50.0)
                val longitude = Random.nextDouble(10.0, 50.0)
                val timestamp = System.currentTimeMillis()
                val severity = "HIGH"
                val medicalInfo = MqttMessageSchemas.EmergencyAlertMessage.MedicalInfo(
                    bloodType = "O+",
                    allergies = listOf("penicillin"),
                    medications = listOf("insulin")
                )
                val location = MqttMessageSchemas.EmergencyAlertMessage.Location(latitude, longitude)
                val message = MqttMessageSchemas.EmergencyAlertMessage(
                    incidentId = incidentId,
                    victimId = victimId,
                    victimName = victimName,
                    location = location,
                    timestamp = timestamp,
                    severity = severity,
                    medicalInfo = medicalInfo
                )
                val json = Json.encodeToString(message)
                val topic = MqttTopics.alertIncident(incidentId)
                MqttService().publish(topic, json)
                showSuccess("Emergency alert sent!")
            } catch (e: Exception) {
                showError("Failed to send alert: ${e.message}")
            }
        }
    }
} 
package com.example.cc.ui.publisher

import com.example.cc.ui.base.BaseViewModel
import android.content.Context
import com.example.cc.util.MqttClient
import com.example.cc.util.MqttTopics
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.random.Random
import com.example.cc.util.EmergencyAlertMessage
import com.example.cc.util.ResponseAckMessage
import android.content.Intent
import com.example.cc.util.MqttService
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class PublisherViewModel(application: Application) : AndroidViewModel(application) {
    
    private var mqttClient: MqttClient? = null
    
    fun initializeMqtt(context: Context) {
        mqttClient = MqttClient(context)
    }
    
    fun sendEmergencyAlert() {
        // Use viewModelScope for coroutines
        viewModelScope.launch {
            try {
                // TODO: Replace with real user/incident/medical profile/ESP location
                val incidentId = "incident_${System.currentTimeMillis()}"
                val victimId = "user_1"
                val victimName = "John Doe"
                val latitude = Random.nextDouble(10.0, 50.0)
                val longitude = Random.nextDouble(10.0, 50.0)
                val timestamp = System.currentTimeMillis()
                val severity = "HIGH"
                val medicalInfo = EmergencyAlertMessage.MedicalInfo(
                    bloodType = "O+",
                    allergies = listOf("penicillin"),
                    medications = listOf("insulin")
                )
                val location = EmergencyAlertMessage.Location(latitude, longitude)
                val message = EmergencyAlertMessage(
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

                // Prefer publishing via background service to leverage retry queue
                val ctx = getApplication<Application>()
                val publishIntent = Intent(ctx, MqttService::class.java).apply {
                    action = MqttService.ACTION_PUBLISH
                    putExtra(MqttService.EXTRA_TOPIC, topic)
                    putExtra(MqttService.EXTRA_PAYLOAD, json)
                    putExtra(MqttService.EXTRA_QOS, 1)
                    putExtra(MqttService.EXTRA_RETAINED, false)
                }
                ctx.startService(publishIntent)
                // Note: Success/error handling would need to be implemented separately
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
} 
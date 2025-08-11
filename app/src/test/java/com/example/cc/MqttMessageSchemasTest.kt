package com.example.cc

import com.example.cc.util.MqttMessageSchemas
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class MqttMessageSchemasTest {
    @Test
    fun testEmergencyAlertMessageSerialization() {
        val message = MqttMessageSchemas.EmergencyAlertMessage(
            incidentId = "incident_123",
            victimId = "user_1",
            victimName = "John Doe",
            location = MqttMessageSchemas.EmergencyAlertMessage.Location(12.34, 56.78),
            timestamp = 1234567890L,
            severity = "HIGH",
            medicalInfo = MqttMessageSchemas.EmergencyAlertMessage.MedicalInfo(
                bloodType = "O+",
                allergies = listOf("penicillin"),
                medications = listOf("insulin")
            )
        )
        val json = Json.encodeToString(message)
        val decoded = Json.decodeFromString<MqttMessageSchemas.EmergencyAlertMessage>(json)
        assertEquals(message, decoded)
    }
}
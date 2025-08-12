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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.example.cc.util.Esp32Manager
import com.example.cc.util.Esp32BluetoothService
import com.example.cc.data.model.MedicalProfile
import com.example.cc.data.model.EmergencyContact
import android.util.Log

class PublisherViewModel(application: Application) : AndroidViewModel(application) {
    
    private var mqttClient: MqttClient? = null
    private var esp32Manager: Esp32Manager? = null
    
    // State properties
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()
    
    // ESP32 and medical profile states
    private val _esp32ConnectionState = MutableStateFlow(Esp32Manager.ConnectionState.DISCONNECTED)
    val esp32ConnectionState: StateFlow<Esp32Manager.ConnectionState> = _esp32ConnectionState.asStateFlow()
    
    private val _esp32ConnectionType = MutableStateFlow(Esp32Manager.ConnectionType.NONE)
    val esp32ConnectionType: StateFlow<Esp32Manager.ConnectionType> = _esp32ConnectionType.asStateFlow()
    
    private val _sensorData = MutableStateFlow<Esp32BluetoothService.SensorData?>(null)
    val sensorData: StateFlow<Esp32BluetoothService.SensorData?> = _sensorData.asStateFlow()
    
    private val _medicalProfile = MutableStateFlow<MedicalProfile?>(null)
    val medicalProfile: StateFlow<MedicalProfile?> = _medicalProfile.asStateFlow()
    
    private val _isEmergencyMode = MutableStateFlow(false)
    val isEmergencyMode: StateFlow<Boolean> = _isEmergencyMode.asStateFlow()
    
    private val _emergencyCountdown = MutableStateFlow(30) // 30 second countdown
    val emergencyCountdown: StateFlow<Int> = _emergencyCountdown.asStateFlow()
    
    fun initializeMqtt(context: Context) {
        mqttClient = MqttClient(context)
        esp32Manager = Esp32Manager(context)
        
        // Initialize ESP32 monitoring
        initializeEsp32Monitoring()
    }
    
    private fun initializeEsp32Monitoring() {
        viewModelScope.launch {
            esp32Manager?.sensorData?.collect { sensorData ->
                _sensorData.value = sensorData
                
                // Check for impact detection
                if (esp32Manager?.isImpactDetected() == true && !_isEmergencyMode.value) {
                    Log.i("PublisherViewModel", "Impact detected! Starting emergency mode")
                    startEmergencyMode()
                }
            }
        }
        
        viewModelScope.launch {
            esp32Manager?.connectionState?.collect { state ->
                _esp32ConnectionState.value = state
            }
        }
        
        viewModelScope.launch {
            esp32Manager?.connectionType?.collect { type ->
                _esp32ConnectionType.value = type
            }
        }
    }
    
    /**
     * Start ESP32 device discovery
     */
    fun startEsp32Discovery() {
        esp32Manager?.startDiscovery()
    }
    
    /**
     * Stop ESP32 device discovery
     */
    fun stopEsp32Discovery() {
        esp32Manager?.stopDiscovery()
    }
    
    /**
     * Connect to ESP32 device
     */
    fun connectToEsp32(device: Any) {
        esp32Manager?.connectToDevice(device)
    }
    
    /**
     * Disconnect from ESP32
     */
    fun disconnectFromEsp32() {
        esp32Manager?.disconnect()
    }
    
    /**
     * Start emergency mode (manual or automatic)
     */
    fun startEmergencyMode() {
        _isEmergencyMode.value = true
        _emergencyCountdown.value = 30
        
        // Start countdown timer
        viewModelScope.launch {
            while (_emergencyCountdown.value > 0 && _isEmergencyMode.value) {
                kotlinx.coroutines.delay(1000)
                _emergencyCountdown.value = _emergencyCountdown.value - 1
            }
            
            // If countdown reaches 0, automatically send emergency alert
            if (_emergencyCountdown.value <= 0 && _isEmergencyMode.value) {
                sendEmergencyAlert()
            }
        }
    }
    
    /**
     * Cancel emergency mode
     */
    fun cancelEmergencyMode() {
        _isEmergencyMode.value = false
        _emergencyCountdown.value = 30
    }
    
    /**
     * Load medical profile
     */
    fun loadMedicalProfile() {
        // TODO: Load from database
        // For now, create a sample profile
        val sampleProfile = MedicalProfile(
            userId = 1,
            fullName = "John Doe",
            dateOfBirth = "1990-01-01",
            bloodType = "O+",
            height = "175",
            weight = "70",
            allergies = "Penicillin, Peanuts",
            medications = "Insulin, Metformin",
            medicalConditions = "Diabetes Type 2",
            emergencyContacts = Json.encodeToString(listOf(
                EmergencyContact("Jane Doe", "Spouse", "+1234567890", "jane@example.com", true),
                EmergencyContact("Dr. Smith", "Primary Care", "+1234567891", "dr.smith@hospital.com")
            )),
            insuranceInfo = "Blue Cross Blue Shield - Policy #123456789",
            organDonor = true
        )
        _medicalProfile.value = sampleProfile
    }
    
    fun sendEmergencyAlert() {
        // Use viewModelScope for coroutines
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null
                
                // Get real data from ESP32 and medical profile
                val incidentId = "incident_${System.currentTimeMillis()}"
                val victimId = "user_1"
                val victimName = _medicalProfile.value?.fullName ?: "Unknown"
                
                // Use ESP32 GPS data if available, otherwise use simulated data
                val sensorData = _sensorData.value
                val latitude = sensorData?.latitude ?: Random.nextDouble(10.0, 50.0)
                val longitude = sensorData?.longitude ?: Random.nextDouble(10.0, 50.0)
                
                val timestamp = System.currentTimeMillis()
                val severity = if (sensorData?.impactForce ?: 0f > 10.0f) "CRITICAL" else "HIGH"
                
                // Enhanced medical info from profile
                val medicalInfo = EmergencyAlertMessage.MedicalInfo(
                    bloodType = _medicalProfile.value?.bloodType ?: "Unknown",
                    allergies = _medicalProfile.value?.allergies?.split(",")?.map { it.trim() } ?: emptyList(),
                    medications = _medicalProfile.value?.medications?.split(",")?.map { it.trim() } ?: emptyList(),
                    conditions = _medicalProfile.value?.medicalConditions?.split(",")?.map { it.trim() } ?: emptyList()
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
                
                // Exit emergency mode after sending alert
                _isEmergencyMode.value = false
                _successMessage.value = "Emergency alert sent successfully!"
                
                Log.i("PublisherViewModel", "Emergency alert sent with ESP32 data: $json")
                
            } catch (e: Exception) {
                _errorMessage.value = "Failed to send emergency alert: ${e.message}"
                Log.e("PublisherViewModel", "Failed to send emergency alert", e)
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        esp32Manager?.cleanup()
    }
} 
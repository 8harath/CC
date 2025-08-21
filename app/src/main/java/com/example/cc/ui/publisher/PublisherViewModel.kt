package com.example.cc.ui.publisher

import com.example.cc.ui.base.BaseViewModel
import android.content.Context
import android.content.Intent
import com.example.cc.util.MqttTopics
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.random.Random
import com.example.cc.util.EmergencyAlertMessage
import com.example.cc.util.ResponseAckMessage
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
import com.example.cc.util.GpsService
import com.example.cc.data.model.MedicalProfile
import com.example.cc.data.model.EmergencyContact
import com.example.cc.ui.publisher.Device
import android.util.Log

class PublisherViewModel(application: Application) : AndroidViewModel(application) {
    
    private var esp32Manager: Esp32Manager? = null
    private var gpsService: GpsService? = null
    
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
    
    private val _discoveredDevices = MutableStateFlow<List<Device>>(emptyList())
    val discoveredDevices: StateFlow<List<Device>> = _discoveredDevices.asStateFlow()
    
    private val _currentLocation = MutableStateFlow<android.location.Location?>(null)
    val currentLocation: StateFlow<android.location.Location?> = _currentLocation.asStateFlow()
    
    private val _gpsStatus = MutableStateFlow("GPS: Not available")
    val gpsStatus: StateFlow<String> = _gpsStatus.asStateFlow()
    
    fun initializeMqtt(context: Context) {
        try {
            Log.i("PublisherViewModel", "Initializing MQTT for publisher role")
            
            // Start MQTT service with publisher role and explicit enable
            val intent = Intent(context, MqttService::class.java).apply {
                action = MqttService.ACTION_ENABLE
                putExtra("role", "PUBLISHER")
            }
            context.startService(intent)
            
            // Initialize ESP32 and GPS services
            esp32Manager = Esp32Manager(context)
            gpsService = GpsService(context)
            
            // Initialize ESP32 monitoring
            initializeEsp32Monitoring()
            
            // Initialize GPS monitoring
            initializeGpsMonitoring()
            
            Log.i("PublisherViewModel", "MQTT service started for publisher role")
            
        } catch (e: Exception) {
            Log.e("PublisherViewModel", "Error initializing MQTT: ${e.message}", e)
            _errorMessage.value = "Failed to initialize MQTT: ${e.message}"
        }
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
        
        viewModelScope.launch {
            esp32Manager?.discoveredDevices?.collect { devices ->
                // Convert discovered devices to Device objects
                val deviceList = devices.mapNotNull { device ->
                    when (device) {
                        is android.bluetooth.BluetoothDevice -> {
                            Device(
                                name = device.name ?: "Unknown Bluetooth Device",
                                address = device.address,
                                deviceType = Esp32Manager.ConnectionType.BLUETOOTH_CLASSIC,
                                signalStrength = 75 // Default signal strength
                            )
                        }
                        is android.net.wifi.p2p.WifiP2pDevice -> {
                            Device(
                                name = device.deviceName,
                                address = device.deviceAddress,
                                deviceType = Esp32Manager.ConnectionType.WIFI_DIRECT,
                                signalStrength = 80 // Default signal strength
                            )
                        }
                        else -> null
                    }
                }
                _discoveredDevices.value = deviceList
            }
        }
    }
    
    /**
     * Initialize GPS monitoring
     */
    private fun initializeGpsMonitoring() {
        viewModelScope.launch {
            gpsService?.currentLocation?.collect { location ->
                _currentLocation.value = location
                _gpsStatus.value = if (location != null) {
                    "GPS: ${String.format("%.6f", location.latitude)}, ${String.format("%.6f", location.longitude)}"
                } else {
                    "GPS: Not available"
                }
            }
        }
        
        viewModelScope.launch {
            gpsService?.isGpsEnabled?.collect { isEnabled ->
                if (!isEnabled) {
                    _gpsStatus.value = "GPS: Disabled"
                }
            }
        }
    }
    
    /**
     * Start GPS location updates
     */
    fun startGpsUpdates() {
        gpsService?.startLocationUpdates()
    }
    
    /**
     * Stop GPS location updates
     */
    fun stopGpsUpdates() {
        gpsService?.stopLocationUpdates()
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
     * Connect to ESP32 device using Device object
     */
    fun connectToEsp32(device: Device) {
        // Convert Device back to the appropriate native device type
        // This is a simplified implementation - in a real app, you'd store the original device reference
        when (device.deviceType) {
            Esp32Manager.ConnectionType.BLUETOOTH_CLASSIC, 
            Esp32Manager.ConnectionType.BLUETOOTH_BLE -> {
                // For now, we'll use a mock Bluetooth device
                // In a real implementation, you'd store the original BluetoothDevice reference
                Log.i("PublisherViewModel", "Connecting to Bluetooth device: ${device.name}")
                // esp32Manager?.connectToDevice(originalBluetoothDevice)
            }
            Esp32Manager.ConnectionType.WIFI_DIRECT -> {
                // For now, we'll use a mock WiFi Direct device
                Log.i("PublisherViewModel", "Connecting to WiFi Direct device: ${device.name}")
                // esp32Manager?.connectToDevice(originalWifiP2pDevice)
            }
            else -> {
                Log.e("PublisherViewModel", "Unknown device type: ${device.deviceType}")
            }
        }
        
        // For demo purposes, simulate successful connection
        _esp32ConnectionState.value = Esp32Manager.ConnectionState.CONNECTING
        viewModelScope.launch {
            kotlinx.coroutines.delay(2000) // Simulate connection time
            _esp32ConnectionState.value = Esp32Manager.ConnectionState.CONNECTED
            _esp32ConnectionType.value = device.deviceType
        }
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
                
                // Use real GPS data if available, then ESP32 GPS data, otherwise use simulated data
                val gpsLocation = _currentLocation.value
                val sensorData = _sensorData.value
                val latitude = gpsLocation?.latitude ?: sensorData?.latitude ?: Random.nextDouble(10.0, 50.0)
                val longitude = gpsLocation?.longitude ?: sensorData?.longitude ?: Random.nextDouble(10.0, 50.0)
                
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
                val topic = "emergency/alerts/$incidentId"

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
    
    /**
     * Send a custom test message for testing publisher-subscriber communication
     */
    fun sendTestMessage(customMessage: String = "Test emergency alert") {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null
                
                val incidentId = "test_incident_${System.currentTimeMillis()}"
                val timestamp = System.currentTimeMillis()
                
                // Create a test message
                val message = EmergencyAlertMessage(
                    incidentId = incidentId,
                    victimId = "test_user",
                    victimName = "Test User",
                    location = EmergencyAlertMessage.Location(
                        latitude = 40.7128, // New York coordinates for testing
                        longitude = -74.0060
                    ),
                    timestamp = timestamp,
                    severity = "TEST",
                    medicalInfo = EmergencyAlertMessage.MedicalInfo(
                        bloodType = "O+",
                        allergies = listOf("None"),
                        medications = listOf("None"),
                        conditions = listOf("None")
                    )
                )
                
                val json = Json.encodeToString(message)
                val topic = "emergency/alerts/$incidentId"
                
                Log.i("PublisherViewModel", "Sending test message: $json")
                
                // Send via MQTT service
                val ctx = getApplication<Application>()
                val publishIntent = Intent(ctx, MqttService::class.java).apply {
                    action = MqttService.ACTION_PUBLISH
                    putExtra(MqttService.EXTRA_TOPIC, topic)
                    putExtra(MqttService.EXTRA_PAYLOAD, json)
                    putExtra(MqttService.EXTRA_QOS, 1)
                    putExtra(MqttService.EXTRA_RETAINED, false)
                }
                ctx.startService(publishIntent)
                
                _successMessage.value = "Test message sent successfully!"
                
            } catch (e: Exception) {
                _errorMessage.value = "Failed to send test message: ${e.message}"
                Log.e("PublisherViewModel", "Failed to send test message: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Send a simple text message to a specific topic
     */
    fun sendSimpleMessage(topic: String, message: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null
                
                Log.i("PublisherViewModel", "Sending simple message to $topic: $message")
                
                // Send via MQTT service
                val ctx = getApplication<Application>()
                val publishIntent = Intent(ctx, MqttService::class.java).apply {
                    action = MqttService.ACTION_PUBLISH
                    putExtra(MqttService.EXTRA_TOPIC, topic)
                    putExtra(MqttService.EXTRA_PAYLOAD, message)
                    putExtra(MqttService.EXTRA_QOS, 1)
                    putExtra(MqttService.EXTRA_RETAINED, false)
                }
                ctx.startService(publishIntent)
                
                _successMessage.value = "Simple message sent to $topic"
                
            } catch (e: Exception) {
                _errorMessage.value = "Failed to send simple message: ${e.message}"
                Log.e("PublisherViewModel", "Failed to send simple message", e)
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Send a custom message entered by the user
     */
    fun sendCustomMessage(message: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null
                
                Log.i("PublisherViewModel", "Sending custom message: $message")
                
                // Send via MQTT service to custom topic that subscribers are listening to
                val topic = "emergency/custom/message"
                val timestamp = System.currentTimeMillis()
                val formattedMessage = "[$timestamp] $message"
                
                val ctx = getApplication<Application>()
                val publishIntent = Intent(ctx, MqttService::class.java).apply {
                    action = MqttService.ACTION_PUBLISH
                    putExtra(MqttService.EXTRA_TOPIC, topic)
                    putExtra(MqttService.EXTRA_PAYLOAD, formattedMessage)
                    putExtra(MqttService.EXTRA_QOS, 1)
                    putExtra(MqttService.EXTRA_RETAINED, false)
                }
                ctx.startService(publishIntent)
                
                _successMessage.value = "Custom message sent successfully!"
                Log.i("PublisherViewModel", "Custom message sent to topic $topic: $formattedMessage")
                
            } catch (e: Exception) {
                _errorMessage.value = "Failed to send custom message: ${e.message}"
                Log.e("PublisherViewModel", "Failed to send custom message: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Send a simple test message for publisher-subscriber communication
     */
    fun sendSimpleTestMessage() {
        val topic = "emergency/test/message"
        val timestamp = System.currentTimeMillis()
        val message = "Hello from Publisher! Test message at $timestamp"
        sendSimpleMessage(topic, message)
    }
    
    override fun onCleared() {
        super.onCleared()
        esp32Manager?.cleanup()
        gpsService?.cleanup()
    }
} 
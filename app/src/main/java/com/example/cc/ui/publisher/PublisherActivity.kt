package com.example.cc.ui.publisher

import android.os.Bundle
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.cc.R
import com.example.cc.ui.base.BaseActivity
import kotlinx.coroutines.launch
import com.airbnb.lottie.LottieAnimationView
import android.os.Handler
import android.os.Looper
import android.view.View
import android.content.Intent
import com.example.cc.util.MqttService
import androidx.lifecycle.Observer
import com.example.cc.util.MqttService.ConnectionState
import com.example.cc.util.Esp32Manager
import com.example.cc.util.PermissionManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.example.cc.ui.publisher.DeviceAdapter
import android.util.Log
import com.example.cc.databinding.ActivityPublisherBinding
import android.content.res.ColorStateList
import androidx.core.content.ContextCompat
import android.content.Context
import android.graphics.Color
import android.content.IntentFilter
import android.content.BroadcastReceiver
import android.view.LayoutInflater
import android.widget.EditText

class PublisherActivity : BaseActivity<ActivityPublisherBinding>() {
    
    private val viewModel: PublisherViewModel by viewModels()
    
    private val messagePublishReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            try {
                val topic = intent?.getStringExtra("topic") ?: return
                val success = intent.getBooleanExtra("success", false)
                val error = intent.getStringExtra("error")
                val payload = intent.getStringExtra("payload")
                
                if (success) {
                    Log.i("PublisherActivity", "✅ Message published successfully to $topic")
                    showToast("✅ Message sent successfully to $topic")
                } else {
                    Log.e("PublisherActivity", "❌ Failed to publish message to $topic: $error")
                    showToast("❌ Failed to send message: $error")
                }
            } catch (e: Exception) {
                Log.e("PublisherActivity", "Error in message publish receiver: ${e.message}")
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            // Register broadcast receiver for message publish feedback
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(messagePublishReceiver, IntentFilter("com.example.cc.MESSAGE_PUBLISHED"), Context.RECEIVER_NOT_EXPORTED)
            } else {
                registerReceiver(messagePublishReceiver, IntentFilter("com.example.cc.MESSAGE_PUBLISHED"))
            }
        } catch (e: Exception) {
            Log.e("PublisherActivity", "Error in onCreate: ${e.message}", e)
        }
    }
    
    override fun onDestroy() {
        try {
            unregisterReceiver(messagePublishReceiver)
        } catch (e: Exception) {
            Log.e("PublisherActivity", "Error in onDestroy: ${e.message}")
        }
        super.onDestroy()
    }
    
    override fun getViewBinding(): ActivityPublisherBinding = ActivityPublisherBinding.inflate(layoutInflater)
    
    override fun setupViews() {
        try {
            setupToolbar()
            setupEmergencyButton()
            setupEsp32Buttons()
            setupMedicalProfileButton()
            setupEmergencyModeButtons()
            setupMqttTestButtons()
            
            // Initialize MQTT service immediately
            Log.i("PublisherActivity", "Initializing MQTT service for publisher")
            val serviceIntent = Intent(this, MqttService::class.java).apply {
                action = MqttService.ACTION_ENABLE
                putExtra("role", "PUBLISHER")
            }
            startService(serviceIntent)
            
            // Start GPS updates if permissions are granted
            if (PermissionManager.hasRequiredPermissions(this)) {
                viewModel.startGpsUpdates()
            }
            
            Log.i("PublisherActivity", "MQTT service started for publisher role")
        } catch (e: Exception) {
            Log.e("PublisherActivity", "Error setting up views: ${e.message}", e)
            showToast("Error setting up app: ${e.message}")
        }
    }
    
    override fun setupObservers() {
        try {
            lifecycleScope.launch {
                viewModel.isLoading.collect { isLoading ->
                    try {
                        binding.btnEmergency.isEnabled = !isLoading
                    } catch (e: Exception) {
                        Log.e("PublisherActivity", "Error updating emergency button: ${e.message}")
                    }
                }
            }
            
            lifecycleScope.launch {
                viewModel.errorMessage.collect { error ->
                    try {
                        error?.let { showToast(it) }
                    } catch (e: Exception) {
                        Log.e("PublisherActivity", "Error showing error message: ${e.message}")
                    }
                }
            }
            
            lifecycleScope.launch {
                viewModel.successMessage.collect { message ->
                    try {
                        message?.let { 
                            showAnimatedConfirmation()
                            showToast(it)
                        }
                    } catch (e: Exception) {
                        Log.e("PublisherActivity", "Error showing success message: ${e.message}")
                    }
                }
            }

            // MQTT status observation disabled since service is not auto-started
            binding.tvStatus.text = "MQTT: Disabled"
            
            // Setup MQTT enable button
            binding.btnEnableMqtt.setOnClickListener {
                enableMqttService()
            }
            
            // Observe ESP32 states
            lifecycleScope.launch {
                viewModel.esp32ConnectionState.collect { state ->
                    try {
                        val esp32Status = when (state) {
                            Esp32Manager.ConnectionState.DISCONNECTED -> "Not Connected"
                            Esp32Manager.ConnectionState.DISCOVERING -> "Discovering..."
                            Esp32Manager.ConnectionState.CONNECTING -> "Connecting..."
                            Esp32Manager.ConnectionState.CONNECTED -> "Connected"
                            Esp32Manager.ConnectionState.ERROR -> "Error"
                        }
                        binding.tvEsp32Status.text = "ESP32: $esp32Status"
                    } catch (e: Exception) {
                        Log.e("PublisherActivity", "Error updating ESP32 status: ${e.message}")
                    }
                }
            }
        
            lifecycleScope.launch {
                viewModel.esp32ConnectionType.collect { type ->
                    try {
                        val connectionType = when (type) {
                            Esp32Manager.ConnectionType.NONE -> ""
                            Esp32Manager.ConnectionType.BLUETOOTH_CLASSIC -> " (Bluetooth Classic)"
                            Esp32Manager.ConnectionType.BLUETOOTH_BLE -> " (Bluetooth BLE)"
                            Esp32Manager.ConnectionType.WIFI_DIRECT -> " (WiFi Direct)"
                        }
                        binding.tvEsp32Status.text = binding.tvEsp32Status.text.toString() + connectionType
                    } catch (e: Exception) {
                        Log.e("PublisherActivity", "Error updating ESP32 connection type: ${e.message}")
                    }
                }
            }
        
            lifecycleScope.launch {
                viewModel.sensorData.collect { data ->
                    try {
                        val sensorText = if (data != null) {
                            "Acc: (${String.format("%.1f", data.accelerometerX)}, ${String.format("%.1f", data.accelerometerY)}, ${String.format("%.1f", data.accelerometerZ)}) " +
                            "Impact: ${String.format("%.1f", data.impactForce)}g"
                        } else {
                            "No sensor data"
                        }
                        binding.tvSensorData.text = "Sensor Data: $sensorText"
                    } catch (e: Exception) {
                        Log.e("PublisherActivity", "Error updating sensor data: ${e.message}")
                    }
                }
            }
        
            // Observe medical profile
            lifecycleScope.launch {
                viewModel.medicalProfile.collect { profile ->
                    try {
                        val profileText = if (profile != null) {
                            "${profile.fullName} - ${profile.bloodType} - ${profile.medicalConditions ?: "No conditions"}"
                        } else {
                            "No medical profile loaded"
                        }
                        binding.tvMedicalProfile.text = profileText
                    } catch (e: Exception) {
                        Log.e("PublisherActivity", "Error updating medical profile: ${e.message}")
                    }
                }
            }
        
            // Observe emergency mode
            lifecycleScope.launch {
                viewModel.isEmergencyMode.collect { isEmergency ->
                    try {
                        binding.cardEmergencyMode.visibility = if (isEmergency) View.VISIBLE else View.GONE
                    } catch (e: Exception) {
                        Log.e("PublisherActivity", "Error updating emergency mode visibility: ${e.message}")
                    }
                }
            }
        
            lifecycleScope.launch {
                viewModel.emergencyCountdown.collect { countdown ->
                    try {
                        binding.tvEmergencyCountdown.text = "Auto-send in: ${countdown}s"
                    } catch (e: Exception) {
                        Log.e("PublisherActivity", "Error updating emergency countdown: ${e.message}")
                    }
                }
            }
        
            lifecycleScope.launch {
                viewModel.gpsStatus.collect { status ->
                    try {
                        binding.tvGpsStatus.text = status
                    } catch (e: Exception) {
                        Log.e("PublisherActivity", "Error updating GPS status: ${e.message}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("PublisherActivity", "Error setting up observers: ${e.message}", e)
        }
    }
    
    private fun setupToolbar() {
        try {
            setSupportActionBar(binding.toolbar)
            supportActionBar?.title = "Crash Victim Mode"
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
        } catch (e: Exception) {
            Log.e("PublisherActivity", "Error setting up toolbar: ${e.message}")
        }
    }
    
    private fun setupEmergencyButton() {
        try {
            binding.btnEmergency.setOnClickListener {
                viewModel.startEmergencyMode()
            }
        } catch (e: Exception) {
            Log.e("PublisherActivity", "Error setting up emergency button: ${e.message}")
        }
    }
    
    private fun setupEsp32Buttons() {
        try {
            binding.btnDiscoverEsp32.setOnClickListener {
                if (PermissionManager.hasRequiredPermissions(this)) {
                    viewModel.startEsp32Discovery()
                } else {
                    PermissionManager.requestRequiredPermissions(this)
                }
            }
            
            binding.btnConnectEsp32.setOnClickListener {
                if (PermissionManager.hasRequiredPermissions(this)) {
                    showDeviceSelectionDialog()
                } else {
                    PermissionManager.requestRequiredPermissions(this)
                }
            }
            
            binding.btnDisconnectEsp32.setOnClickListener {
                viewModel.disconnectFromEsp32()
            }
        } catch (e: Exception) {
            Log.e("PublisherActivity", "Error setting up ESP32 buttons: ${e.message}")
        }
    }
    
    private fun setupMedicalProfileButton() {
        try {
            binding.btnLoadProfile.setOnClickListener {
                viewModel.loadMedicalProfile()
            }
        } catch (e: Exception) {
            Log.e("PublisherActivity", "Error setting up medical profile button: ${e.message}")
        }
    }
    
    private fun setupEmergencyModeButtons() {
        try {
            binding.btnCancelEmergency.setOnClickListener {
                viewModel.cancelEmergencyMode()
            }
            
            binding.btnSendNow.setOnClickListener {
                viewModel.sendEmergencyAlert()
            }
        } catch (e: Exception) {
            Log.e("PublisherActivity", "Error setting up emergency mode buttons: ${e.message}")
        }
    }
    
    private fun showDeviceSelectionDialog() {
        try {
            val dialogView = layoutInflater.inflate(R.layout.dialog_device_selection, null)
            val recyclerView = dialogView.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvDevices)
            val statusText = dialogView.findViewById<android.widget.TextView>(R.id.tvDiscoveryStatus)
            val refreshButton = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnRefresh)
            val cancelButton = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnCancel)
            
            val deviceAdapter = DeviceAdapter(mutableListOf()) { device ->
                // Handle device selection
                viewModel.connectToEsp32(device)
            }
            
            recyclerView.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
            recyclerView.adapter = deviceAdapter
            
            val dialog = MaterialAlertDialogBuilder(this)
                .setView(dialogView)
                .setCancelable(false)
                .create()
            
            // Start device discovery
            viewModel.startEsp32Discovery()
            statusText.text = "Discovering ESP32 devices..."
            
            // Observe discovered devices
            lifecycleScope.launch {
                viewModel.discoveredDevices.collect { devices ->
                    deviceAdapter.updateDevices(devices)
                    statusText.text = if (devices.isEmpty()) {
                        "No devices found. Make sure ESP32 is powered on and discoverable."
                    } else {
                        "${devices.size} device(s) found"
                    }
                }
            }
            
            // Observe connection state
            lifecycleScope.launch {
                viewModel.esp32ConnectionState.collect { state ->
                    when (state) {
                        Esp32Manager.ConnectionState.CONNECTED -> {
                            dialog.dismiss()
                            showToast("Connected to ESP32 device!")
                        }
                        Esp32Manager.ConnectionState.ERROR -> {
                            showToast("Failed to connect to ESP32 device")
                        }
                        else -> { /* Other states handled elsewhere */ }
                    }
                }
            }
            
            refreshButton.setOnClickListener {
                viewModel.startEsp32Discovery()
                statusText.text = "Discovering ESP32 devices..."
            }
            
            cancelButton.setOnClickListener {
                viewModel.stopEsp32Discovery()
                dialog.dismiss()
            }
            
            dialog.show()
        } catch (e: Exception) {
            Log.e("PublisherActivity", "Error showing device selection dialog: ${e.message}")
        }
    }
    
    private fun showAnimatedConfirmation() {
        try {
            val lottie = binding.lottieCheckmark
            val fallbackCheckmark = binding.fallbackCheckmark
            
            // Add failure listener to handle animation loading errors gracefully
            lottie.setFailureListener { throwable ->
                Log.e("LottieError", "Failed to load checkmark animation", throwable)
                // Hide the Lottie view and show fallback image
                lottie.visibility = View.GONE
                fallbackCheckmark.visibility = View.VISIBLE
                // Show a simple toast
                showToast("Success!")
            }
            
            // Hide fallback and show Lottie animation
            fallbackCheckmark.visibility = View.GONE
            lottie.visibility = View.VISIBLE
            lottie.playAnimation()
            
            Handler(Looper.getMainLooper()).postDelayed({
                lottie.visibility = View.GONE
                fallbackCheckmark.visibility = View.GONE
            }, 1800)
        } catch (e: Exception) {
            Log.e("PublisherActivity", "Error showing animated confirmation: ${e.message}")
        }
    }
    
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        try {
            when (requestCode) {
                PermissionManager.getPermissionRequestCode() -> {
                    if (grantResults.isNotEmpty() && grantResults.all { it == android.content.pm.PackageManager.PERMISSION_GRANTED }) {
                        // Permissions granted, proceed with ESP32 operations and GPS
                        showToast("Permissions granted! You can now use ESP32 features and GPS.")
                        viewModel.startGpsUpdates()
                    } else {
                        // Permissions denied
                        showToast("Permissions required for ESP32 communication and GPS")
                    }
                }
                PermissionManager.getPermissionRequestCode() + 1 -> {
                    if (grantResults.isNotEmpty() && grantResults.all { it == android.content.pm.PackageManager.PERMISSION_GRANTED }) {
                        // Camera permissions granted
                        showToast("Camera permissions granted!")
                    } else {
                        showToast("Camera permissions required for profile photos")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("PublisherActivity", "Error handling permission results: ${e.message}")
        }
    }
    
    private fun enableMqttService() {
        try {
            Log.i("PublisherActivity", "Enabling MQTT service for publisher role")
            
            // Start MQTT service with explicit enable action
            val serviceIntent = Intent(this, MqttService::class.java).apply {
                action = MqttService.ACTION_ENABLE
                putExtra("role", "PUBLISHER")
            }
            startService(serviceIntent)
            
            // Update UI
            binding.btnEnableMqtt.text = "Enabled"
            binding.btnEnableMqtt.isEnabled = false
            binding.btnEnableMqtt.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.success))
            
            // Start observing MQTT connection state
            MqttService.connectionState.observe(this, Observer { state ->
                try {
                    val statusText = when (state) {
                        ConnectionState.CONNECTING -> "MQTT: Connecting..."
                        ConnectionState.CONNECTED -> "MQTT: Connected"
                        ConnectionState.DISCONNECTED -> "MQTT: Disconnected"
                        else -> "MQTT: ${state.toString()}"
                    }
                    binding.tvStatus.text = statusText
                    
                    // Enable/disable MQTT test buttons based on connection state
                    binding.btnTestMqttConnection.isEnabled = state == ConnectionState.CONNECTED
                    binding.btnSendTestMessage.isEnabled = state == ConnectionState.CONNECTED
                    binding.btnSendSimpleMessage.isEnabled = state == ConnectionState.CONNECTED
                    binding.btnSendCustomMessage.isEnabled = state == ConnectionState.CONNECTED
                } catch (e: Exception) {
                    Log.e("PublisherActivity", "Error updating MQTT status: ${e.message}")
                }
            })
            
            showToast("MQTT service enabled for publisher role")
            
        } catch (e: Exception) {
            Log.e("PublisherActivity", "Error enabling MQTT service: ${e.message}")
            showToast("Failed to enable MQTT service: ${e.message}")
        }
    }
    
    private fun setupMqttTestButtons() {
        // Test MQTT Connection
        binding.btnTestMqttConnection.setOnClickListener {
            animateButtonClick(it)
            testMqttConnection()
        }
        
        // Send Test Message
        binding.btnSendTestMessage.setOnClickListener {
            animateButtonClick(it)
            sendTestMessage()
        }
        
        // Send Simple Message
        binding.btnSendSimpleMessage.setOnClickListener {
            animateButtonClick(it)
            sendSimpleMessage()
        }
        
        // Send Custom Message
        binding.btnSendCustomMessage.setOnClickListener {
            animateButtonClick(it)
            showCustomMessageDialog()
        }
        
        // MQTT Settings
        binding.btnMqttSettings.setOnClickListener {
            animateButtonClick(it)
            openMqttSettings()
        }
    }
    
    private fun animateButtonClick(view: View) {
        // Scale down animation
        view.animate()
            .scaleX(0.95f)
            .scaleY(0.95f)
            .setDuration(100)
            .withEndAction {
                // Scale back up
                view.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(100)
                    .start()
            }
            .start()
    }
    
    private fun showCustomMessageDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_custom_message, null)
        val messageInput = dialogView.findViewById<EditText>(R.id.etCustomMessage)
        
        // Set default message
        messageInput.setText("Custom message from Publisher at ${System.currentTimeMillis()}")
        
        MaterialAlertDialogBuilder(this)
            .setTitle("📝 Send Custom Message")
            .setView(dialogView)
            .setPositiveButton("Send") { _, _ ->
                val customMessage = messageInput.text.toString()
                if (customMessage.isNotEmpty()) {
                    sendCustomMessage(customMessage)
                } else {
                    showToast("Please enter a message")
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun sendCustomMessage(message: String) {
        try {
            viewModel.sendCustomMessage(message)
            Log.i("PublisherActivity", "Custom message sent: $message")
        } catch (e: Exception) {
            Log.e("PublisherActivity", "Error sending custom message: ${e.message}")
            showToast("Error sending custom message: ${e.message}")
        }
    }
    
    private fun openMqttSettings() {
        try {
            val intent = Intent(this, com.example.cc.ui.settings.MqttSettingsActivity::class.java)
            startActivity(intent)
        } catch (e: Exception) {
            Log.e("PublisherActivity", "Error opening MQTT settings: ${e.message}")
            showToast("Error opening MQTT settings: ${e.message}")
        }
    }
    
    private fun testMqttConnection() {
        try {
            val isConnected = MqttService.isConnected()
            val status = MqttService.getStatusString()
            
            val message = if (isConnected) {
                "✅ MQTT Connection Test: SUCCESS\nStatus: $status"
            } else {
                "❌ MQTT Connection Test: FAILED\nStatus: $status"
            }
            
            showToast(message)
            Log.i("PublisherActivity", "MQTT Connection Test: $message")
            
        } catch (e: Exception) {
            Log.e("PublisherActivity", "Error testing MQTT connection: ${e.message}")
            showToast("Error testing MQTT connection: ${e.message}")
        }
    }
    
    private fun sendTestMessage() {
        try {
            // Use the ViewModel's sendTestMessage function which creates a proper EmergencyAlertMessage
            viewModel.sendTestMessage("Test emergency alert from Publisher")
            
            Log.i("PublisherActivity", "Test message sent via ViewModel")
            
        } catch (e: Exception) {
            Log.e("PublisherActivity", "Error sending test message: ${e.message}")
            showToast("Error sending test message: ${e.message}")
        }
    }
    
    private fun sendSimpleMessage() {
        try {
            // Use the ViewModel's sendSimpleTestMessage function
            viewModel.sendSimpleTestMessage()
            
            Log.i("PublisherActivity", "Simple message sent via ViewModel")
            
        } catch (e: Exception) {
            Log.e("PublisherActivity", "Error sending simple message: ${e.message}")
            showToast("Error sending simple message: ${e.message}")
        }
    }
} 
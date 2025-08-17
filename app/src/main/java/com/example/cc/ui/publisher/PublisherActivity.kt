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

class PublisherActivity : BaseActivity<ActivityPublisherBinding>() {
    
    private val viewModel: PublisherViewModel by viewModels()
    
    override fun getViewBinding(): ActivityPublisherBinding = ActivityPublisherBinding.inflate(layoutInflater)
    
    override fun setupViews() {
        try {
            setupToolbar()
            setupEmergencyButton()
            setupEsp32Buttons()
            setupMedicalProfileButton()
            setupEmergencyModeButtons()
            viewModel.initializeMqtt(this)
            
            // Start GPS updates if permissions are granted
            if (PermissionManager.hasRequiredPermissions(this)) {
                viewModel.startGpsUpdates()
            }
            
            // MQTT service will be started manually when user enables it
            Log.i("PublisherActivity", "MQTT service auto-start disabled for stability")
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
            
            // Start MQTT service
            val serviceIntent = Intent(this, MqttService::class.java).apply {
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
} 
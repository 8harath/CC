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

class PublisherActivity : BaseActivity<View>() {
    
    private val viewModel: PublisherViewModel by viewModels()
    
    override fun getViewBinding(): View = layoutInflater.inflate(R.layout.activity_publisher, null)
    
    override fun setupViews() {
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
        
        // Start MQTT service to manage background connection and topic subscriptions for publisher if needed
        try {
            val serviceIntent = Intent(this, MqttService::class.java).apply {
                putExtra("role", "PUBLISHER")
            }
            startService(serviceIntent)
        } catch (e: Exception) {
            // Log error but don't crash the app
            android.util.Log.e("PublisherActivity", "Failed to start MQTT service: ${e.message}")
        }
    }
    
    override fun setupObservers() {
        lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                findViewById<com.google.android.material.button.MaterialButton>(R.id.btnEmergency).isEnabled = !isLoading
            }
        }
        
        lifecycleScope.launch {
            viewModel.errorMessage.collect { error ->
                error?.let { showToast(it) }
            }
        }
        
        lifecycleScope.launch {
            viewModel.successMessage.collect { message ->
                message?.let { 
                    showAnimatedConfirmation()
                    showToast(it)
                }
            }
        }

        // Observe connection state from service
        MqttService.connectionState.observe(this, Observer { state ->
            val statusText = when (state) {
                ConnectionState.CONNECTING -> "Connecting..."
                ConnectionState.CONNECTED -> "Connected"
                ConnectionState.DISCONNECTED -> "Disconnected"
                else -> state.toString()
            }
            findViewById<android.widget.TextView>(R.id.tvStatus).text = "MQTT: $statusText"
        })
        
        // Observe ESP32 states
        lifecycleScope.launch {
            viewModel.esp32ConnectionState.collect { state ->
                val esp32Status = when (state) {
                    Esp32Manager.ConnectionState.DISCONNECTED -> "Not Connected"
                    Esp32Manager.ConnectionState.DISCOVERING -> "Discovering..."
                    Esp32Manager.ConnectionState.CONNECTING -> "Connecting..."
                    Esp32Manager.ConnectionState.CONNECTED -> "Connected"
                    Esp32Manager.ConnectionState.ERROR -> "Error"
                }
                findViewById<android.widget.TextView>(R.id.tvEsp32Status).text = "ESP32: $esp32Status"
            }
        }
        
        lifecycleScope.launch {
            viewModel.esp32ConnectionType.collect { type ->
                val connectionType = when (type) {
                    Esp32Manager.ConnectionType.NONE -> ""
                    Esp32Manager.ConnectionType.BLUETOOTH_CLASSIC -> " (Bluetooth Classic)"
                    Esp32Manager.ConnectionType.BLUETOOTH_BLE -> " (Bluetooth BLE)"
                    Esp32Manager.ConnectionType.WIFI_DIRECT -> " (WiFi Direct)"
                }
                findViewById<android.widget.TextView>(R.id.tvEsp32Status).text = findViewById<android.widget.TextView>(R.id.tvEsp32Status).text.toString() + connectionType
            }
        }
        
        lifecycleScope.launch {
            viewModel.sensorData.collect { data ->
                val sensorText = if (data != null) {
                    "Acc: (${String.format("%.1f", data.accelerometerX)}, ${String.format("%.1f", data.accelerometerY)}, ${String.format("%.1f", data.accelerometerZ)}) " +
                    "Impact: ${String.format("%.1f", data.impactForce)}g"
                } else {
                    "No sensor data"
                }
                findViewById<android.widget.TextView>(R.id.tvSensorData).text = "Sensor Data: $sensorText"
            }
        }
        
        // Observe medical profile
        lifecycleScope.launch {
            viewModel.medicalProfile.collect { profile ->
                val profileText = if (profile != null) {
                    "${profile.fullName} - ${profile.bloodType} - ${profile.medicalConditions ?: "No conditions"}"
                } else {
                    "No medical profile loaded"
                }
                findViewById<android.widget.TextView>(R.id.tvMedicalProfile).text = profileText
            }
        }
        
        // Observe emergency mode
        lifecycleScope.launch {
            viewModel.isEmergencyMode.collect { isEmergency ->
                findViewById<com.google.android.material.card.MaterialCardView>(R.id.cardEmergencyMode).visibility = if (isEmergency) View.VISIBLE else View.GONE
            }
        }
        
        lifecycleScope.launch {
            viewModel.emergencyCountdown.collect { countdown ->
                findViewById<android.widget.TextView>(R.id.tvEmergencyCountdown).text = "Auto-send in: ${countdown}s"
            }
        }
        
        lifecycleScope.launch {
            viewModel.gpsStatus.collect { status ->
                findViewById<android.widget.TextView>(R.id.tvGpsStatus).text = status
            }
        }
    }
    
    private fun setupToolbar() {
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.title = "Crash Victim Mode"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }
    
    private fun setupEmergencyButton() {
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnEmergency).setOnClickListener {
            viewModel.startEmergencyMode()
        }
    }
    
    private fun setupEsp32Buttons() {
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnDiscoverEsp32).setOnClickListener {
            if (PermissionManager.hasRequiredPermissions(this)) {
                viewModel.startEsp32Discovery()
            } else {
                PermissionManager.requestRequiredPermissions(this)
            }
        }
        
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnConnectEsp32).setOnClickListener {
            if (PermissionManager.hasRequiredPermissions(this)) {
                showDeviceSelectionDialog()
            } else {
                PermissionManager.requestRequiredPermissions(this)
            }
        }
        
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnDisconnectEsp32).setOnClickListener {
            viewModel.disconnectFromEsp32()
        }
    }
    
    private fun setupMedicalProfileButton() {
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnLoadProfile).setOnClickListener {
            viewModel.loadMedicalProfile()
        }
    }
    
    private fun setupEmergencyModeButtons() {
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnCancelEmergency).setOnClickListener {
            viewModel.cancelEmergencyMode()
        }
        
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSendNow).setOnClickListener {
            viewModel.sendEmergencyAlert()
        }
    }
    
    private fun showDeviceSelectionDialog() {
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
    }
    
    private fun showAnimatedConfirmation() {
        val lottie = findViewById<LottieAnimationView>(R.id.lottieCheckmark)
        val fallbackCheckmark = findViewById<android.widget.ImageView>(R.id.fallbackCheckmark)
        
        // Add failure listener to handle animation loading errors gracefully
        lottie.addFailureListener { throwable: Throwable ->
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
    }
} 
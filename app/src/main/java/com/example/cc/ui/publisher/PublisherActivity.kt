package com.example.cc.ui.publisher

import android.os.Bundle
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.cc.R
import com.example.cc.databinding.ActivityPublisherBinding
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

class PublisherActivity : BaseActivity<ActivityPublisherBinding>() {
    
    private val viewModel: PublisherViewModel by viewModels()
    
    override fun getViewBinding(): ActivityPublisherBinding = ActivityPublisherBinding.inflate(layoutInflater)
    
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
                binding.btnEmergency.isEnabled = !isLoading
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
            binding.tvStatus.text = "MQTT: $statusText"
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
                binding.tvEsp32Status.text = "ESP32: $esp32Status"
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
                binding.tvEsp32Status.text = binding.tvEsp32Status.text.toString() + connectionType
            }
        }
        
        lifecycleScope.launch {
            viewModel.sensorData.collect { data ->
                val sensorText = if (data != null) {
                    "Acc: (${data.accelerometerX:.1f}, ${data.accelerometerY:.1f}, ${data.accelerometerZ:.1f}) " +
                    "Impact: ${data.impactForce:.1f}g"
                } else {
                    "No sensor data"
                }
                binding.tvSensorData.text = "Sensor Data: $sensorText"
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
                binding.tvMedicalProfile.text = profileText
            }
        }
        
        // Observe emergency mode
        lifecycleScope.launch {
            viewModel.isEmergencyMode.collect { isEmergency ->
                binding.cardEmergencyMode.visibility = if (isEmergency) View.VISIBLE else View.GONE
            }
        }
        
        lifecycleScope.launch {
            viewModel.emergencyCountdown.collect { countdown ->
                binding.tvEmergencyCountdown.text = "Auto-send in: ${countdown}s"
            }
        }
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "Crash Victim Mode"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }
    
    private fun setupEmergencyButton() {
        binding.btnEmergency.setOnClickListener {
            viewModel.startEmergencyMode()
        }
    }
    
    private fun setupEsp32Buttons() {
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
    }
    
    private fun setupMedicalProfileButton() {
        binding.btnLoadProfile.setOnClickListener {
            viewModel.loadMedicalProfile()
        }
    }
    
    private fun setupEmergencyModeButtons() {
        binding.btnCancelEmergency.setOnClickListener {
            viewModel.cancelEmergencyMode()
        }
        
        binding.btnSendNow.setOnClickListener {
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
            dialog.dismiss()
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
        val lottie = binding.lottieCheckmark
        lottie.visibility = View.VISIBLE
        lottie.playAnimation()
        Handler(Looper.getMainLooper()).postDelayed({
            lottie.visibility = View.GONE
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
                    // Permissions granted, proceed with ESP32 operations
                    showToast("Permissions granted! You can now use ESP32 features.")
                } else {
                    // Permissions denied
                    showToast("Permissions required for ESP32 communication")
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
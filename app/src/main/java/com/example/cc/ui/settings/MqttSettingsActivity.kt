package com.example.cc.ui.settings

import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.cc.R
import com.example.cc.ui.base.BaseActivity
import com.example.cc.databinding.ActivityMqttSettingsBinding
import com.example.cc.util.MqttConfig
import com.example.cc.util.MqttService
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class MqttSettingsActivity : BaseActivity<ActivityMqttSettingsBinding>() {
    
    private val viewModel: MqttSettingsViewModel by viewModels()
    
    override fun getViewBinding(): ActivityMqttSettingsBinding = ActivityMqttSettingsBinding.inflate(layoutInflater)
    
    override fun setupViews() {
        try {
            setupToolbar()
            setupMqttSettings()
            setupTestConnectionButton()
            setupSaveButton()
            
            // Load current settings
            viewModel.loadCurrentSettings(this)
            
            // Show current settings info
            showCurrentSettingsInfo()
            
            // Add settings summary
            addSettingsSummary()
            
        } catch (e: Exception) {
            Log.e("MqttSettingsActivity", "Error setting up views: ${e.message}", e)
            showToast("Error setting up MQTT settings: ${e.message}")
        }
    }
    
    override fun setupObservers() {
        try {
            lifecycleScope.launch {
                viewModel.brokerIp.collect { ip ->
                    binding.etBrokerIp.setText(ip)
                }
            }
            
            lifecycleScope.launch {
                viewModel.brokerPort.collect { port ->
                    binding.etBrokerPort.setText(port.toString())
                }
            }
            
            lifecycleScope.launch {
                viewModel.connectionStatus.collect { status ->
                    binding.tvConnectionStatus.text = status
                    
                    // Show additional connection info
                    val currentIp = binding.etBrokerIp.text.toString()
                    val currentPort = binding.etBrokerPort.text.toString()
                    if (currentIp.isNotEmpty() && currentPort.isNotEmpty()) {
                        binding.tvConnectionStatus.text = "$status\nBroker: $currentIp:$currentPort"
                    }
                }
            }
            
            lifecycleScope.launch {
                viewModel.isLoading.collect { isLoading ->
                    binding.btnTestConnection.isEnabled = !isLoading
                    binding.btnSaveSettings.isEnabled = !isLoading
                    binding.progressBar.visibility = if (isLoading) android.view.View.VISIBLE else android.view.View.GONE
                }
            }
            
            lifecycleScope.launch {
                viewModel.errorMessage.collect { error ->
                    error?.let { showToast(it) }
                }
            }
            
            lifecycleScope.launch {
                viewModel.successMessage.collect { message ->
                    message?.let { showToast(it) }
                }
            }
            
        } catch (e: Exception) {
            Log.e("MqttSettingsActivity", "Error setting up observers: ${e.message}", e)
        }
    }
    
    private fun setupToolbar() {
        try {
            setSupportActionBar(binding.toolbar)
            supportActionBar?.title = "MQTT Settings"
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
        } catch (e: Exception) {
            Log.e("MqttSettingsActivity", "Error setting up toolbar: ${e.message}")
        }
    }
    
    private fun setupMqttSettings() {
        try {
            // Set up input validation
            binding.etBrokerIp.addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: android.text.Editable?) {
                    val ip = s?.toString() ?: ""
                    viewModel.updateBrokerIp(ip)
                    
                    // Validate IP format in real-time
                    if (ip.isNotEmpty() && ip != "localhost") {
                        if (!isValidIpFormat(ip)) {
                            binding.etBrokerIp.error = "Invalid IP format. Use format like 192.168.1.100"
                        } else {
                            binding.etBrokerIp.error = null
                        }
                    } else {
                        binding.etBrokerIp.error = null
                    }
                }
            })
            
            binding.etBrokerPort.addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: android.text.Editable?) {
                    val portStr = s?.toString() ?: ""
                    if (portStr.isNotEmpty()) {
                        val port = portStr.toIntOrNull()
                        if (port != null && port in 1..65535) {
                            viewModel.updateBrokerPort(port)
                            binding.etBrokerPort.error = null
                        } else {
                            binding.etBrokerPort.error = "Port must be between 1 and 65535"
                        }
                    } else {
                        binding.etBrokerPort.error = null
                    }
                }
            })
            
            // Set up quick IP buttons
            binding.btnLocalhost.setOnClickListener {
                binding.etBrokerIp.setText("localhost")
                binding.etBrokerIp.requestFocus()
                binding.etBrokerIp.text?.let { text ->
                    binding.etBrokerIp.setSelection(text.length)
                }
            }
            
            binding.btnLocalIp.setOnClickListener {
                binding.etBrokerIp.setText("192.168.1.100")
                binding.etBrokerIp.requestFocus()
                binding.etBrokerIp.text?.let { text ->
                    binding.etBrokerIp.setSelection(text.length)
                }
            }
            
            binding.btnCustomIp.setOnClickListener {
                showCustomIpDialog()
            }
            
            binding.btnClearIp.setOnClickListener {
                binding.etBrokerIp.setText("")
                binding.etBrokerIp.requestFocus()
            }
            
            // Add reset to defaults button functionality
            binding.btnLocalIp.setOnLongClickListener {
                // Long press to reset to defaults
                binding.etBrokerIp.setText("192.168.1.100")
                binding.etBrokerPort.setText("1883")
                showToast("Reset to default settings")
                true
            }
            
            // Add clear button functionality
            binding.etBrokerIp.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    // Show clear button or highlight the field
                    binding.etBrokerIp.text?.let { text ->
                        binding.etBrokerIp.setSelection(text.length)
                    }
                }
            }
            
        } catch (e: Exception) {
            Log.e("MqttSettingsActivity", "Error setting up MQTT settings: ${e.message}")
        }
    }
    
    private fun setupTestConnectionButton() {
        try {
            binding.btnTestConnection.setOnClickListener {
                // Get current input values for testing
                val currentIp = binding.etBrokerIp.text.toString()
                val currentPort = binding.etBrokerPort.text.toString()
                
                if (currentIp.isNotEmpty() && currentPort.isNotEmpty()) {
                    // Update ViewModel with current values
                    viewModel.updateBrokerIp(currentIp)
                    viewModel.updateBrokerPort(currentPort.toIntOrNull() ?: 1883)
                    
                    // Test connection with current settings
                    viewModel.testConnection()
                } else {
                    showToast("Please enter both IP address and port before testing")
                }
            }
            
            binding.btnEnableMqttService.setOnClickListener {
                enableMqttService()
            }
        } catch (e: Exception) {
            Log.e("MqttSettingsActivity", "Error setting up test connection button: ${e.message}")
        }
    }
    
    private fun setupSaveButton() {
        try {
            binding.btnSaveSettings.setOnClickListener {
                // Get current input values for saving
                val currentIp = binding.etBrokerIp.text.toString()
                val currentPort = binding.etBrokerPort.text.toString()
                
                if (currentIp.isNotEmpty() && currentPort.isNotEmpty()) {
                    // Show confirmation dialog
                    showSaveConfirmationDialog(currentIp, currentPort.toIntOrNull() ?: 1883)
                } else {
                    showToast("Please enter both IP address and port before saving")
                }
            }
        } catch (e: Exception) {
            Log.e("MqttSettingsActivity", "Error setting up save button: ${e.message}")
        }
    }
    
    private fun showCustomIpDialog() {
        try {
            val dialogView = layoutInflater.inflate(R.layout.dialog_custom_ip, null)
            val etCustomIp = dialogView.findViewById<android.widget.EditText>(R.id.etCustomIp)
            
            // Pre-fill with current IP if available
            val currentIp = binding.etBrokerIp.text.toString()
            if (currentIp.isNotEmpty()) {
                etCustomIp.setText(currentIp)
                etCustomIp.setSelection(currentIp.length)
            }
            
            val dialog = MaterialAlertDialogBuilder(this)
                .setTitle("Enter Custom IP Address")
                .setView(dialogView)
                .setPositiveButton("Set", null) // We'll set the listener after creating the dialog
                .setNegativeButton("Cancel", null)
                .create()
            
            // Set up validation for the positive button
            dialog.setOnShowListener {
                val positiveButton = dialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE)
                positiveButton.setOnClickListener {
                    val customIp = etCustomIp.text.toString().trim()
                    if (customIp.isEmpty()) {
                        etCustomIp.error = "IP address cannot be empty"
                        return@setOnClickListener
                    }
                    
                    if (customIp != "localhost" && !isValidIpFormat(customIp)) {
                        etCustomIp.error = "Invalid IP format. Use format like 192.168.1.100"
                        return@setOnClickListener
                    }
                    
                    // IP is valid, set it and dismiss
                    binding.etBrokerIp.setText(customIp)
                    dialog.dismiss()
                }
            }
            
            dialog.show()
                
        } catch (e: Exception) {
            Log.e("MqttSettingsActivity", "Error showing custom IP dialog: ${e.message}")
        }
    }
    
    private fun showCurrentSettingsInfo() {
        try {
            // Show a toast with current settings
            val currentIp = binding.etBrokerIp.text.toString()
            val currentPort = binding.etBrokerPort.text.toString()
            if (currentIp.isNotEmpty() && currentPort.isNotEmpty()) {
                showToast("Current settings: $currentIp:$currentPort")
            }
        } catch (e: Exception) {
            Log.e("MqttSettingsActivity", "Error showing current settings info: ${e.message}")
        }
    }
    
    /**
     * Validate IP address format
     */
    private fun isValidIpFormat(ip: String): Boolean {
        return try {
            val parts = ip.split(".")
            if (parts.size != 4) return false
            
            parts.all { part ->
                val num = part.toInt()
                num in 0..255
            }
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Add settings summary information
     */
    private fun addSettingsSummary() {
        try {
            // Add a small info text below the settings
            val summaryText = "💡 Tip: You can edit the IP address and port above. " +
                            "Use the quick selection buttons or enter a custom IP address. " +
                            "The settings will be saved and used for all MQTT connections."
            
            // You can add this as a TextView in the layout or show it as a toast
            // For now, we'll show it as a toast
            showToast(summaryText)
        } catch (e: Exception) {
            Log.e("MqttSettingsActivity", "Error adding settings summary: ${e.message}")
        }
    }
    
    /**
     * Show confirmation dialog before saving settings
     */
    private fun showSaveConfirmationDialog(ip: String, port: Int) {
        try {
            MaterialAlertDialogBuilder(this)
                .setTitle("Save MQTT Settings")
                .setMessage("Are you sure you want to save these settings?\n\nBroker: $ip:$port\n\nThis will update all MQTT connections in the app.")
                .setPositiveButton("Save") { _, _ ->
                    // Update ViewModel with current values
                    viewModel.updateBrokerIp(ip)
                    viewModel.updateBrokerPort(port)
                    
                    // Save settings
                    viewModel.saveSettings(this)
                }
                .setNegativeButton("Cancel", null)
                .show()
        } catch (e: Exception) {
            Log.e("MqttSettingsActivity", "Error showing save confirmation dialog: ${e.message}")
        }
    }
    
    /**
     * Enable MQTT service for testing
     */
    private fun enableMqttService() {
        try {
            // Get current input values
            val currentIp = binding.etBrokerIp.text.toString()
            val currentPort = binding.etBrokerPort.text.toString()
            
            if (currentIp.isEmpty() || currentPort.isEmpty()) {
                showToast("Please enter both IP address and port before enabling MQTT service")
                return
            }
            
            // Update ViewModel with current values
            viewModel.updateBrokerIp(currentIp)
            viewModel.updateBrokerPort(currentPort.toIntOrNull() ?: 1883)
            
            // Save settings first
            viewModel.saveSettings(this)
            
            // Enable MQTT service
            val serviceIntent = Intent(this, MqttService::class.java).apply {
                action = MqttService.ACTION_ENABLE
                putExtra("role", "TESTER")
            }
            startService(serviceIntent)
            
            // Update button state
            binding.btnEnableMqttService.text = "MQTT Service Enabled"
            binding.btnEnableMqttService.isEnabled = false
            binding.btnEnableMqttService.backgroundTintList = android.content.res.ColorStateList.valueOf(
                android.graphics.Color.parseColor("#4CAF50") // Green color
            )
            
            showToast("MQTT service enabled! You can now test publisher/subscriber functionality.")
            
        } catch (e: Exception) {
            Log.e("MqttSettingsActivity", "Error enabling MQTT service: ${e.message}")
            showToast("Error enabling MQTT service: ${e.message}")
        }
    }
    
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}

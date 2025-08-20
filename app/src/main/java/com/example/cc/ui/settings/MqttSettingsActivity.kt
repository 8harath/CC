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
                binding.etBrokerIp.setSelection(binding.etBrokerIp.text.length)
            }
            
            binding.btnLocalIp.setOnClickListener {
                binding.etBrokerIp.setText("192.168.1.100")
                binding.etBrokerIp.requestFocus()
                binding.etBrokerIp.setSelection(binding.etBrokerIp.text.length)
            }
            
            binding.btnCustomIp.setOnClickListener {
                showCustomIpDialog()
            }
            
            binding.btnClearIp.setOnClickListener {
                binding.etBrokerIp.setText("")
                binding.etBrokerIp.requestFocus()
            }
            
            // Add clear button functionality
            binding.etBrokerIp.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    // Show clear button or highlight the field
                    binding.etBrokerIp.setSelection(binding.etBrokerIp.text.length)
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
                    // Update ViewModel with current values
                    viewModel.updateBrokerIp(currentIp)
                    viewModel.updateBrokerPort(currentPort.toIntOrNull() ?: 1883)
                    
                    // Save settings
                    viewModel.saveSettings(this)
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
            
            MaterialAlertDialogBuilder(this)
                .setTitle("Enter Custom IP Address")
                .setView(dialogView)
                .setPositiveButton("Set") { _, _ ->
                    val customIp = etCustomIp.text.toString().trim()
                    if (customIp.isNotEmpty()) {
                        binding.etBrokerIp.setText(customIp)
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
                
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
    
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}

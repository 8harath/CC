package com.example.cc.ui.publisher

import android.os.Bundle
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.cc.ui.base.BaseActivity
import com.example.cc.util.MqttService
import com.example.cc.util.MqttConfig
import kotlinx.coroutines.launch
import android.view.View
import android.util.Log
import android.content.Intent
import com.example.cc.databinding.ActivityPublisherBinding
import com.google.android.material.snackbar.Snackbar

class PublisherActivity : BaseActivity<ActivityPublisherBinding>() {
    
    companion object {
        private const val TAG = "PublisherActivity"
    }
    
    private val viewModel: PublisherViewModel by viewModels()
    
    override fun getViewBinding(): ActivityPublisherBinding = ActivityPublisherBinding.inflate(layoutInflater)
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            // Initialize MqttConfig
            MqttConfig.init(this)
            
            // Initialize MQTT service for publisher
            Log.i(TAG, "Initializing MQTT service for publisher")
            val serviceIntent = Intent(this, MqttService::class.java).apply {
                action = MqttService.ACTION_ENABLE
                putExtra("role", "PUBLISHER")
            }
            startService(serviceIntent)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate: ${e.message}", e)
        }
    }
    
    override fun setupViews() {
        try {
            setupToolbar()
            setupConnectionStatus()
            setupBrokerSettings()
            setupEmergencyButton()
            setupExperimentalFeatures()
            
            Log.i(TAG, "Views setup completed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up views: ${e.message}", e)
            showToast("Error initializing app: ${e.message}")
        }
    }
    
    override fun setupObservers() {
        try {
            lifecycleScope.launch {
                viewModel.connectionState.collect { state ->
                    updateConnectionStatus(state)
                }
            }
            
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
                viewModel.customMessage.collect { message ->
                    binding.etCustomMessage.setText(message)
                }
            }
            
            lifecycleScope.launch {
                viewModel.messageStatus.collect { status ->
                    if (status.isNotEmpty()) {
                        binding.tvMessageStatus.text = status
                        binding.cardMessageStatus.visibility = View.VISIBLE
                    }
                }
            }
            
            lifecycleScope.launch {
                viewModel.showMessageStatus.collect { show ->
                    binding.cardMessageStatus.visibility = if (show) View.VISIBLE else View.GONE
                }
            }
            
            lifecycleScope.launch {
                viewModel.showExperimentalFeatures.collect { show ->
                    binding.llExperimentalFeatures.visibility = if (show) View.VISIBLE else View.GONE
                    binding.btnToggleExperimental.text = if (show) "Hide Advanced Features" else "Show Advanced Features"
                }
            }
            
            lifecycleScope.launch {
                viewModel.isConnecting.collect { connecting ->
                    binding.btnTestConnection.isEnabled = !connecting
                    binding.btnTestConnection.text = if (connecting) "Testing..." else "Test Connection"
                }
            }
            
            lifecycleScope.launch {
                viewModel.isSending.collect { sending ->
                    binding.btnSendEmergency.isEnabled = !sending
                    binding.btnSendEmergency.text = if (sending) "Sending..." else "Send Emergency Alert"
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up observers: ${e.message}", e)
        }
    }
    
    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
    }
    
    private fun setupConnectionStatus() {
        binding.btnTestConnection.setOnClickListener {
            viewModel.testConnection()
        }
    }
    
    private fun setupBrokerSettings() {
        binding.etBrokerIp.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                viewModel.updateBrokerIp(binding.etBrokerIp.text.toString())
            }
        }
        
        binding.etBrokerPort.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                try {
                    val port = binding.etBrokerPort.text.toString().toInt()
                    viewModel.updateBrokerPort(port)
                } catch (e: NumberFormatException) {
                    binding.etBrokerPort.setText("1883")
                    viewModel.updateBrokerPort(1883)
                }
            }
        }
        
        binding.btnSaveSettings.setOnClickListener {
            viewModel.saveSettings()
            showToast("Settings saved successfully")
        }
    }
    
    private fun setupEmergencyButton() {
        binding.etCustomMessage.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                viewModel.updateCustomMessage(binding.etCustomMessage.text.toString())
            }
        }
        
        binding.btnSendEmergency.setOnClickListener {
            viewModel.sendEmergencyAlert()
        }
    }
    
    private fun setupExperimentalFeatures() {
        binding.btnToggleExperimental.setOnClickListener {
            viewModel.toggleExperimentalFeatures()
        }
        
        // Setup experimental feature buttons if they exist
        binding.btnConnectEsp32?.setOnClickListener {
            showToast("ESP32 connection feature coming soon")
        }
        
        binding.btnTestBluetooth?.setOnClickListener {
            showToast("Bluetooth testing feature coming soon")
        }
        
        binding.btnMedicalProfile?.setOnClickListener {
            showToast("Medical profile feature coming soon")
        }
    }
    
    private fun updateConnectionStatus(state: MqttService.ConnectionState) {
        val (color, text) = when (state) {
            MqttService.ConnectionState.CONNECTED -> {
                android.graphics.Color.GREEN to "Connected"
            }
            MqttService.ConnectionState.CONNECTING -> {
                android.graphics.Color.YELLOW to "Connecting..."
            }
            MqttService.ConnectionState.DISCONNECTED -> {
                android.graphics.Color.RED to "Disconnected"
            }
        }
        
        binding.connectionIndicator.setBackgroundColor(color)
        binding.tvConnectionStatus.text = text
    }
    

} 
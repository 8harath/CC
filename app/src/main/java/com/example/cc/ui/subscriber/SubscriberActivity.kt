package com.example.cc.ui.subscriber

import android.os.Bundle
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.cc.ui.base.BaseActivity
import com.example.cc.util.MqttService
import com.example.cc.util.MqttConfig
import kotlinx.coroutines.launch
import android.view.View
import android.util.Log
import android.content.Intent
import com.example.cc.databinding.ActivitySubscriberBinding
import com.google.android.material.snackbar.Snackbar

class SubscriberActivity : BaseActivity<ActivitySubscriberBinding>() {
    
    companion object {
        private const val TAG = "SubscriberActivity"
    }
    
    private val viewModel: SubscriberViewModel by viewModels()
    private lateinit var alertAdapter: AlertHistoryAdapter
    
    override fun getViewBinding(): ActivitySubscriberBinding = ActivitySubscriberBinding.inflate(layoutInflater)
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            // Initialize MqttConfig
            MqttConfig.init(this)
            
            // Initialize MQTT service for subscriber
            Log.i(TAG, "Initializing MQTT service for subscriber")
            val serviceIntent = Intent(this, MqttService::class.java).apply {
                action = MqttService.ACTION_ENABLE
                putExtra("role", "SUBSCRIBER")
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
            setupAlertsList()
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
                viewModel.emergencyAlerts.collect { alerts ->
                    updateAlertsList(alerts)
                }
            }
            
            lifecycleScope.launch {
                viewModel.alertCount.collect { count ->
                    binding.tvAlertCount.text = "$count alerts received"
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
    
    private fun setupAlertsList() {
        binding.btnClearAlerts.setOnClickListener {
            viewModel.clearAllAlerts()
            showToast("All alerts cleared")
        }
        
        // Setup RecyclerView
        binding.rvAlerts.layoutManager = LinearLayoutManager(this)
        alertAdapter = AlertHistoryAdapter { incident ->
            // Handle incident click - could open detail view
            showToast("Alert: ${incident.message}")
        }
        binding.rvAlerts.adapter = alertAdapter
    }
    
    private fun setupExperimentalFeatures() {
        binding.btnToggleExperimental.setOnClickListener {
            viewModel.toggleExperimentalFeatures()
        }
        
        // Setup experimental feature buttons if they exist
        binding.btnTestMqttConnection?.setOnClickListener {
            showToast("MQTT testing feature coming soon")
        }
        
        binding.btnSendTestMessage?.setOnClickListener {
            showToast("Test message feature coming soon")
        }
        
        binding.btnMqttSettings?.setOnClickListener {
            showToast("MQTT settings feature coming soon")
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
    
    private fun updateAlertsList(alerts: List<com.example.cc.data.model.Incident>) {
        if (alerts.isEmpty()) {
            binding.tvNoAlerts.visibility = View.VISIBLE
            binding.rvAlerts.visibility = View.GONE
        } else {
            binding.tvNoAlerts.visibility = View.GONE
            binding.rvAlerts.visibility = View.VISIBLE
            alertAdapter.submitList(alerts)
        }
    }
    
    private fun showToast(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }
} 
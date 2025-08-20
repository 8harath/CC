package com.example.cc.ui.subscriber

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.BroadcastReceiver
import android.content.IntentFilter
import android.os.Build
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.core.app.NotificationCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.cc.ui.base.BaseActivity
import com.example.cc.R
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import android.os.Bundle
import com.example.cc.util.MqttService
import androidx.lifecycle.Observer
import com.example.cc.util.MqttService.ConnectionState
import android.view.View
import android.util.Log
import com.example.cc.databinding.ActivitySubscriberBinding
import android.content.res.ColorStateList
import androidx.core.content.ContextCompat

class SubscriberActivity : BaseActivity<ActivitySubscriberBinding>() {
    
    private val viewModel: SubscriberViewModel by viewModels()
    private lateinit var alertAdapter: AlertHistoryAdapter
    
    private val emergencyAlertReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            try {
                val json = intent?.getStringExtra("alert_json") ?: return
                onMqttEmergencyAlert(json)
            } catch (e: Exception) {
                Log.e("SubscriberActivity", "Error in emergency alert receiver: ${e.message}")
            }
        }
    }
    
    private val simpleMessageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            try {
                val topic = intent?.getStringExtra("topic") ?: return
                val message = intent?.getStringExtra("message") ?: return
                onMqttSimpleMessage(topic, message)
            } catch (e: Exception) {
                Log.e("SubscriberActivity", "Error in simple message receiver: ${e.message}")
            }
        }
    }

    private val customMessageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            try {
                val topic = intent?.getStringExtra("topic") ?: return
                val message = intent?.getStringExtra("message") ?: return
                onMqttCustomMessage(topic, message)
            } catch (e: Exception) {
                Log.e("SubscriberActivity", "Error in custom message receiver: ${e.message}")
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            // Register broadcast receivers for different message types
            registerReceiver(emergencyAlertReceiver, IntentFilter("com.example.cc.EMERGENCY_ALERT_RECEIVED"))
            registerReceiver(simpleMessageReceiver, IntentFilter("com.example.cc.SIMPLE_MESSAGE_RECEIVED"))
            registerReceiver(customMessageReceiver, IntentFilter("com.example.cc.CUSTOM_MESSAGE_RECEIVED"))
            
            // Initialize MQTT service immediately for subscriber
            Log.i("SubscriberActivity", "Initializing MQTT service for subscriber")
            val serviceIntent = Intent(this, MqttService::class.java).apply {
                action = MqttService.ACTION_ENABLE
                putExtra("role", "SUBSCRIBER")
            }
            startService(serviceIntent)
            
            // Setup MQTT enable button (for manual control if needed)
            binding.btnEnableMqtt.setOnClickListener {
                enableMqttService()
            }
            
            // Don't add sample data - let real messages populate the list
            Log.i("SubscriberActivity", "Ready to receive real MQTT messages")
        } catch (e: Exception) {
            Log.e("SubscriberActivity", "Error in onCreate: ${e.message}", e)
            showToast("Error initializing Emergency Responder mode")
        }
    }
    
    override fun onDestroy() {
        try {
            unregisterReceiver(emergencyAlertReceiver)
            unregisterReceiver(simpleMessageReceiver)
            unregisterReceiver(customMessageReceiver)
        } catch (e: Exception) {
            Log.e("SubscriberActivity", "Error in onDestroy: ${e.message}")
        }
        super.onDestroy()
    }
    
    override fun getViewBinding(): ActivitySubscriberBinding = ActivitySubscriberBinding.inflate(layoutInflater)
    
    override fun setupViews() {
        try {
            setupToolbar()
            setupAlertHistoryList()
            setupMqttTestButtons()
            
            // Initialize MQTT service immediately for subscriber
            Log.i("SubscriberActivity", "Initializing MQTT service for subscriber")
            val serviceIntent = Intent(this, MqttService::class.java).apply {
                action = MqttService.ACTION_ENABLE
                putExtra("role", "SUBSCRIBER")
            }
            startService(serviceIntent)
            
            // Setup MQTT enable button (for manual control if needed)
            binding.btnEnableMqtt.setOnClickListener {
                enableMqttService()
            }
            
            // Don't add sample data - let real messages populate the list
            Log.i("SubscriberActivity", "Ready to receive real MQTT messages")
        } catch (e: Exception) {
            Log.e("SubscriberActivity", "Error in setupViews: ${e.message}", e)
            showToast("Error setting up Emergency Responder interface")
        }
    }
    
    override fun setupObservers() {
        try {
            lifecycleScope.launch {
                viewModel.isLoading.collect { isLoading ->
                    // Handle loading state
                }
            }
            
            lifecycleScope.launch {
                viewModel.errorMessage.collect { error ->
                    try {
                        error?.let { showToast(it) }
                    } catch (e: Exception) {
                        Log.e("SubscriberActivity", "Error showing error message: ${e.message}")
                    }
                }
            }
            
            lifecycleScope.launch {
                viewModel.connectionStatus.collect { status ->
                    try {
                        updateConnectionStatus(status)
                    } catch (e: Exception) {
                        Log.e("SubscriberActivity", "Error updating connection status: ${e.message}")
                    }
                }
            }
            
            lifecycleScope.launch {
                viewModel.alertHistory.collect { alerts ->
                    updateDashboardStats(alerts.size)
                }
            }
            
            lifecycleScope.launch {
                viewModel.isResponding.collect { respondingSet ->
                    updateActiveResponses(respondingSet.size)
                }
            }
            
            lifecycleScope.launch {
                viewModel.alertHistory.collect { alerts ->
                    alertAdapter.submitList(alerts)
                }
            }
            
            lifecycleScope.launch {
                viewModel.responseStatus.collect { responseStatus ->
                    alertAdapter.updateResponseStatus(responseStatus)
                }
            }

            // Listen to service connection state
            MqttService.connectionState.observe(this, Observer { state ->
                val statusText = when (state) {
                    ConnectionState.CONNECTING -> "Connecting..."
                    ConnectionState.CONNECTED -> "Connected"
                    ConnectionState.DISCONNECTED -> "Disconnected"
                    else -> state.toString()
                }
                updateConnectionStatus(statusText)
            })
        } catch (e: Exception) {
            Log.e("SubscriberActivity", "Error in setupObservers: ${e.message}", e)
            showToast("Error setting up data observers")
        }
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "Emergency Responder Mode"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }
    
    private fun setupAlertHistoryList() {
        try {
            alertAdapter = AlertHistoryAdapter().apply {
                onIncidentClick = { incident ->
                    openIncidentDetails(incident)
                }
            }
            
            binding.recyclerViewAlerts.apply {
                layoutManager = LinearLayoutManager(this@SubscriberActivity)
                adapter = alertAdapter
            }
            
            // Observe alert history from ViewModel
            lifecycleScope.launch {
                viewModel.alertHistory.collect { alerts ->
                    try {
                        Log.i("SubscriberActivity", "Updating alert history with ${alerts.size} alerts")
                        alertAdapter.submitList(alerts)
                        
                        // Update dashboard stats
                        updateDashboardStats(alerts.size)
                        updateActiveResponses(alerts.count { it.severity == "HIGH" })
                        
                    } catch (e: Exception) {
                        Log.e("SubscriberActivity", "Error updating alert history: ${e.message}")
                    }
                }
            }
            
            Log.i("SubscriberActivity", "Alert history list setup completed")
        } catch (e: Exception) {
            Log.e("SubscriberActivity", "Error setting up alert history list: ${e.message}")
        }
    }
    
    private fun updateConnectionStatus(status: String) {
        binding.tvConnectionStatus.text = status
    }
    
    private fun updateDashboardStats(totalAlerts: Int) {
        binding.tvTotalAlerts.text = totalAlerts.toString()
    }
    
    private fun updateActiveResponses(activeCount: Int) {
        binding.tvActiveResponses.text = activeCount.toString()
    }

    // Call this when an MQTT message is received
    fun onMqttEmergencyAlert(json: String) {
        viewModel.onEmergencyAlertReceived(json)
        showEmergencyNotification()
    }
    
    // Call this when a simple MQTT message is received
    fun onMqttSimpleMessage(topic: String, message: String) {
        viewModel.onSimpleMessageReceived(topic, message)
        showSimpleMessageNotification(message)
    }
    
    private fun showSimpleMessageNotification(message: String) {
        val channelId = "simple_messages"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Simple Messages", NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }
        val intent = Intent(this, SubscriberActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("📨 Test Message Received")
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_message)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun showEmergencyNotification() {
        val channelId = "emergency_alerts"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Emergency Alerts", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }
        val intent = Intent(this, SubscriberActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("🚨 Emergency Alert Received")
            .setContentText("Tap to view details.")
            .setSmallIcon(R.drawable.ic_emergency)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
    
    private fun openIncidentDetails(incident: com.example.cc.util.EmergencyAlertMessage) {
        val intent = Intent(this, IncidentDetailActivity::class.java).apply {
            putExtra("incident_json", kotlinx.serialization.json.Json.encodeToString(com.example.cc.util.EmergencyAlertMessage.serializer(), incident))
        }
        startActivity(intent)
    }
    
    private fun checkReceivedMessages() {
        try {
            // Get the current list of alerts from the ViewModel
            val currentAlerts = viewModel.alertHistory.value ?: emptyList()
            
            val message = if (currentAlerts.isNotEmpty()) {
                val latestAlert = currentAlerts.maxByOrNull { it.timestamp }
                "📨 Received Messages: ${currentAlerts.size} alerts\nLatest: ${latestAlert?.victimName ?: "Unknown"}"
            } else {
                "📨 No messages received yet\nWaiting for emergency alerts..."
            }
            
            showToast(message)
            Log.i("SubscriberActivity", "Check Received Messages: $message")
            
        } catch (e: Exception) {
            Log.e("SubscriberActivity", "Error checking received messages: ${e.message}")
            showToast("Error checking received messages: ${e.message}")
        }
    }
    
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
    
    private fun enableMqttService() {
        try {
            Log.i("SubscriberActivity", "Enabling MQTT service for subscriber role")
            
            // Start MQTT service with explicit enable action
            val serviceIntent = Intent(this, MqttService::class.java).apply {
                action = MqttService.ACTION_ENABLE
                putExtra("role", "SUBSCRIBER")
            }
            startService(serviceIntent)
            
            // Update UI
            binding.btnEnableMqtt.text = "Enabled"
            binding.btnEnableMqtt.isEnabled = false
            binding.btnEnableMqtt.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.success))
            
            // Update connection status
            binding.tvConnectionStatus.text = "MQTT: Enabled"
            
            // Start observing MQTT connection state
            MqttService.connectionState.observe(this, Observer { state ->
                try {
                    val statusText = when (state) {
                        ConnectionState.CONNECTING -> "MQTT: Connecting..."
                        ConnectionState.CONNECTED -> "MQTT: Connected"
                        ConnectionState.DISCONNECTED -> "MQTT: Disconnected"
                        else -> "MQTT: ${state.toString()}"
                    }
                    binding.tvConnectionStatus.text = statusText
                    
                    // Enable/disable MQTT test buttons based on connection state
                    binding.btnTestMqttConnection.isEnabled = state == ConnectionState.CONNECTED
                    binding.btnCheckReceivedMessages.isEnabled = state == ConnectionState.CONNECTED
                } catch (e: Exception) {
                    Log.e("SubscriberActivity", "Error updating MQTT status: ${e.message}")
                }
            })
            
            showToast("MQTT service enabled for subscriber role")
            
        } catch (e: Exception) {
            Log.e("SubscriberActivity", "Error enabling MQTT service: ${e.message}")
            showToast("Failed to enable MQTT service: ${e.message}")
        }
    }
    
    private fun setupMqttTestButtons() {
        // Test MQTT Connection
        binding.btnTestMqttConnection.setOnClickListener {
            animateButtonClick(it)
            testMqttConnection()
        }
        
        // Check Received Messages
        binding.btnCheckReceivedMessages.setOnClickListener {
            animateButtonClick(it)
            checkReceivedMessages()
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
    
    private fun openMqttSettings() {
        try {
            val intent = Intent(this, com.example.cc.ui.settings.MqttSettingsActivity::class.java)
            startActivity(intent)
        } catch (e: Exception) {
            Log.e("SubscriberActivity", "Error opening MQTT settings: ${e.message}")
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
            Log.i("SubscriberActivity", "MQTT Connection Test: $message")
            
        } catch (e: Exception) {
            Log.e("SubscriberActivity", "Error testing MQTT connection: ${e.message}")
            showToast("Error testing MQTT connection: ${e.message}")
        }
    }

    // Call this when a custom MQTT message is received
    fun onMqttCustomMessage(topic: String, message: String) {
        viewModel.onCustomMessageReceived(topic, message)
        showCustomMessageNotification(message)
    }
    
    private fun showCustomMessageNotification(message: String) {
        val channelId = "custom_messages"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Custom Messages", NotificationManager.IMPORTANCE_HIGH)
            channel.description = "Custom messages from publisher"
            notificationManager.createNotificationChannel(channel)
        }
        val intent = Intent(this, SubscriberActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("💬 Custom Message Received")
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_message)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .build()
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
} 
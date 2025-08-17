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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            registerReceiver(emergencyAlertReceiver, IntentFilter("com.example.cc.EMERGENCY_ALERT_RECEIVED"))
            // MQTT service will be started manually when user enables it
            Log.i("SubscriberActivity", "MQTT service auto-start disabled for stability")
        } catch (e: Exception) {
            Log.e("SubscriberActivity", "Error in onCreate: ${e.message}", e)
            showToast("Error initializing Emergency Responder mode")
        }
    }

    override fun onDestroy() {
        try {
            unregisterReceiver(emergencyAlertReceiver)
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
            // MQTT initialization disabled since service is not auto-started
            Log.i("SubscriberActivity", "MQTT initialization disabled for stability")
            
            // Setup MQTT enable button
            binding.btnEnableMqtt.setOnClickListener {
                enableMqttService()
            }
            
            // Add sample data for demonstration
            addSampleAlerts()
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
        alertAdapter = AlertHistoryAdapter()
        alertAdapter.onIncidentClick = { incident ->
            openIncidentDetails(incident)
        }
        binding.recyclerViewAlerts.apply {
            layoutManager = LinearLayoutManager(this@SubscriberActivity)
            adapter = alertAdapter
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
    
    private fun addSampleAlerts() {
        try {
            // Create sample emergency alerts for demonstration
            val sampleAlert1 = com.example.cc.util.EmergencyAlertMessage(
                incidentId = "INC_001",
                victimId = "VICTIM_001",
                victimName = "John Smith",
                location = com.example.cc.util.EmergencyAlertMessage.Location(40.7128, -74.0060),
                timestamp = System.currentTimeMillis() - 300000, // 5 minutes ago
                severity = "HIGH",
                medicalInfo = com.example.cc.util.EmergencyAlertMessage.MedicalInfo(
                    bloodType = "O+",
                    allergies = listOf("Penicillin"),
                    medications = listOf("Aspirin"),
                    conditions = listOf("Hypertension")
                )
            )
            
            val sampleAlert2 = com.example.cc.util.EmergencyAlertMessage(
                incidentId = "INC_002",
                victimId = "VICTIM_002",
                victimName = "Sarah Johnson",
                location = com.example.cc.util.EmergencyAlertMessage.Location(40.7589, -73.9851),
                timestamp = System.currentTimeMillis() - 600000, // 10 minutes ago
                severity = "MEDIUM",
                medicalInfo = com.example.cc.util.EmergencyAlertMessage.MedicalInfo(
                    bloodType = "A-",
                    allergies = listOf("None"),
                    medications = listOf("None"),
                    conditions = listOf("None")
                )
            )
            
            // Add sample alerts to the adapter
            alertAdapter.submitList(listOf(sampleAlert1, sampleAlert2))
            
            // Update dashboard stats
            updateDashboardStats(2)
            updateActiveResponses(1)
            updateConnectionStatus("Demo Mode")
            
            Log.i("SubscriberActivity", "Sample alerts added for demonstration")
        } catch (e: Exception) {
            Log.e("SubscriberActivity", "Error adding sample alerts: ${e.message}", e)
        }
    }
    
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
    
    private fun enableMqttService() {
        try {
            Log.i("SubscriberActivity", "Enabling MQTT service for subscriber role")
            
            // Start MQTT service
            val serviceIntent = Intent(this, MqttService::class.java).apply {
                putExtra("role", "SUBSCRIBER")
            }
            startService(serviceIntent)
            
            // Update UI
            binding.btnEnableMqtt.text = "Enabled"
            binding.btnEnableMqtt.isEnabled = false
            binding.btnEnableMqtt.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.success))
            
            // Update connection status
            binding.tvConnectionStatus.text = "MQTT: Enabled"
            
            showToast("MQTT service enabled for subscriber role")
            
        } catch (e: Exception) {
            Log.e("SubscriberActivity", "Error enabling MQTT service: ${e.message}")
            showToast("Failed to enable MQTT service: ${e.message}")
        }
    }
} 
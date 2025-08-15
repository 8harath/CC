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
import android.content.ComponentName
import android.content.ServiceConnection
import android.os.IBinder
import android.content.ContextWrapper
import com.example.cc.util.MqttService
import androidx.lifecycle.Observer
import com.example.cc.util.MqttService.ConnectionState
import android.view.View

class SubscriberActivity : BaseActivity<View>() {
    
    private val viewModel: SubscriberViewModel by viewModels()
    
    private lateinit var alertAdapter: AlertHistoryAdapter
    
    private val emergencyAlertReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val json = intent?.getStringExtra("alert_json") ?: return
            onMqttEmergencyAlert(json)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            registerReceiver(emergencyAlertReceiver, IntentFilter("com.example.cc.EMERGENCY_ALERT_RECEIVED"))
            // Start MQTT service with role for dynamic subscriptions
            try {
                val serviceIntent = Intent(this, MqttService::class.java).apply {
                    putExtra("role", "SUBSCRIBER")
                }
                startService(serviceIntent)
            } catch (e: Exception) {
                // Log error but don't crash the app
                android.util.Log.e("SubscriberActivity", "Failed to start MQTT service: ${e.message}")
            }
        } catch (e: Exception) {
            android.util.Log.e("SubscriberActivity", "Error in onCreate: ${e.message}", e)
            showToast("Error initializing Emergency Responder mode")
        }
    }

    override fun onDestroy() {
        unregisterReceiver(emergencyAlertReceiver)
        super.onDestroy()
    }
    
    override fun getViewBinding(): View = layoutInflater.inflate(R.layout.activity_subscriber, null)
    
    override fun setupViews() {
        try {
            setupToolbar()
            setupAlertHistoryList()
            // Temporarily disable MQTT initialization to prevent crashes
            // viewModel.initializeMqtt(this)
            android.util.Log.i("SubscriberActivity", "MQTT initialization disabled for stability")
        } catch (e: Exception) {
            android.util.Log.e("SubscriberActivity", "Error in setupViews: ${e.message}", e)
            showToast("Error setting up Emergency Responder interface")
        }
    }
    
    override fun setupObservers() {
        lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                // Handle loading state
            }
        }
        
        lifecycleScope.launch {
            viewModel.errorMessage.collect { error ->
                showToast(error)
            }
        }
        
        lifecycleScope.launch {
            viewModel.connectionStatus.collect { status ->
                updateConnectionStatus(status)
            }
        }
        
        lifecycleScope.launch {
            viewModel.alertHistory.collectLatest { alerts ->
                updateDashboardStats(alerts.size)
            }
        }
        
        lifecycleScope.launch {
            viewModel.isResponding.collectLatest { respondingSet ->
                updateActiveResponses(respondingSet.size)
            }
        }

        lifecycleScope.launch {
            viewModel.alertHistory.collectLatest { alerts ->
                alertAdapter.submitList(alerts)
            }
        }
        
        lifecycleScope.launch {
            viewModel.responseStatus.collectLatest { responseStatus ->
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
    }
    
    private fun setupToolbar() {
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.title = "Emergency Responder Mode"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }
    
    private fun setupAlertHistoryList() {
        alertAdapter = AlertHistoryAdapter()
        alertAdapter.onIncidentClick = { incident ->
            openIncidentDetails(incident)
        }
        findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.recyclerViewAlerts).apply {
            layoutManager = LinearLayoutManager(this@SubscriberActivity)
            adapter = alertAdapter
        }
    }
    
    private fun setupStatusDisplay() {
        // TODO: Implement status display
    }
    
    private fun updateConnectionStatus(status: String) {
        findViewById<android.widget.TextView>(R.id.tvConnectionStatus).text = status
    }
    
    private fun updateDashboardStats(totalAlerts: Int) {
        findViewById<android.widget.TextView>(R.id.tvTotalAlerts).text = totalAlerts.toString()
    }
    
    private fun updateActiveResponses(activeCount: Int) {
        findViewById<android.widget.TextView>(R.id.tvActiveResponses).text = activeCount.toString()
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
    
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
} 
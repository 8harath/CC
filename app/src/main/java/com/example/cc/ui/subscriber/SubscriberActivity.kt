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
import com.example.cc.databinding.ActivitySubscriberBinding
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

class SubscriberActivity : BaseActivity<ActivitySubscriberBinding>() {
    
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
        registerReceiver(emergencyAlertReceiver, IntentFilter("com.example.cc.EMERGENCY_ALERT_RECEIVED"))
        // Start MQTT service with role for dynamic subscriptions
        val serviceIntent = Intent(this, MqttService::class.java).apply {
            putExtra("role", "SUBSCRIBER")
        }
        startService(serviceIntent)
    }

    override fun onDestroy() {
        unregisterReceiver(emergencyAlertReceiver)
        super.onDestroy()
    }
    
    override fun getViewBinding(): ActivitySubscriberBinding = ActivitySubscriberBinding.inflate(layoutInflater)
    
    override fun setupViews() {
        setupToolbar()
        setupAlertHistoryList()
        viewModel.initializeMqtt(this)
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
                alertAdapter.submitList(alerts)
            }
        }
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "Emergency Responder Mode"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }
    
    private fun setupAlertHistoryList() {
        alertAdapter = AlertHistoryAdapter()
        binding.recyclerViewAlerts.apply {
            layoutManager = LinearLayoutManager(this@SubscriberActivity)
            adapter = alertAdapter
        }
    }
    
    private fun setupStatusDisplay() {
        // TODO: Implement status display
    }
    
    private fun updateConnectionStatus(status: String) {
        binding.tvStatus.text = "Status: $status"
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
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
} 
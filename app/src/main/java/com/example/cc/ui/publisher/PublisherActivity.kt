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

class PublisherActivity : BaseActivity<ActivityPublisherBinding>() {
    
    private val viewModel: PublisherViewModel by viewModels()
    
    override fun getViewBinding(): ActivityPublisherBinding = ActivityPublisherBinding.inflate(layoutInflater)
    
    override fun setupViews() {
        setupToolbar()
        setupEmergencyButton()
        viewModel.initializeMqtt(this)
        // Start MQTT service to manage background connection and topic subscriptions for publisher if needed
        val serviceIntent = Intent(this, MqttService::class.java).apply {
            putExtra("role", "PUBLISHER")
        }
        startService(serviceIntent)
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
            binding.tvStatus.text = "Status: $statusText"
        })
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "Crash Victim Mode"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }
    
    private fun setupEmergencyButton() {
        binding.btnEmergency.setOnClickListener {
            viewModel.sendEmergencyAlert()
        }
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
        onBackPressed()
        return true
    }
} 
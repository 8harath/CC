package com.example.cc.ui.subscriber

import android.os.Bundle
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.cc.databinding.ActivitySubscriberBinding
import com.example.cc.ui.base.BaseActivity
import kotlinx.coroutines.launch

class SubscriberActivity : BaseActivity<ActivitySubscriberBinding>() {
    
    private val viewModel: SubscriberViewModel by viewModels()
    
    override fun getViewBinding(): ActivitySubscriberBinding = ActivitySubscriberBinding.inflate(layoutInflater)
    
    override fun setupViews() {
        setupToolbar()
        setupStatusDisplay()
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
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "Emergency Responder Mode"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }
    
    private fun setupStatusDisplay() {
        // TODO: Implement status display
    }
    
    private fun updateConnectionStatus(status: String) {
        binding.tvStatus.text = "Status: $status"
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
} 
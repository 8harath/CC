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

class PublisherActivity : BaseActivity<ActivityPublisherBinding>() {
    
    private val viewModel: PublisherViewModel by viewModels()
    
    override fun getViewBinding(): ActivityPublisherBinding = ActivityPublisherBinding.inflate(layoutInflater)
    
    override fun setupViews() {
        setupToolbar()
        setupEmergencyButton()
    }
    
    override fun setupObservers() {
        super.setupObservers()
        lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                binding.btnEmergency.isEnabled = !isLoading
            }
        }
        
        lifecycleScope.launch {
            viewModel.errorMessage.collect { error ->
                showToast(error)
            }
        }
        
        lifecycleScope.launch {
            viewModel.successMessage.collect { message ->
                showAnimatedConfirmation()
                showToast(message)
            }
        }
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
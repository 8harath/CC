package com.example.cc.ui.publisher

import com.example.cc.ui.base.BaseViewModel
import javax.inject.Inject

class PublisherViewModel @Inject constructor() : BaseViewModel() {
    
    fun sendEmergencyAlert() {
        launchWithLoading {
            // TODO: Implement MQTT emergency alert sending
            // For Phase 1, just show a success message
            showToast("Emergency alert sent! (MQTT not implemented yet)")
        }
    }
} 
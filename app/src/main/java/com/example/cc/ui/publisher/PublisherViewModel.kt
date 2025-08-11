package com.example.cc.ui.publisher

import com.example.cc.ui.base.BaseViewModel
class PublisherViewModel : BaseViewModel() {
    
    fun sendEmergencyAlert() {
        launchWithLoading {
            // TODO: Implement MQTT emergency alert sending
            // For Phase 1, just show a success message
            showSuccess("Emergency alert sent! (MQTT not implemented yet)")
        }
    }
} 
package com.example.cc.ui.subscriber

import com.example.cc.ui.base.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

class SubscriberViewModel @Inject constructor() : BaseViewModel() {
    
    private val _connectionStatus = MutableStateFlow("Disconnected")
    val connectionStatus: StateFlow<String> = _connectionStatus
    
    init {
        // TODO: Implement MQTT connection
        _connectionStatus.value = "MQTT not implemented yet"
    }
} 
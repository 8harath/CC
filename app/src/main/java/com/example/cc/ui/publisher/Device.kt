package com.example.cc.ui.publisher

import com.example.cc.util.Esp32Manager

data class Device(
    val name: String,
    val address: String,
    val deviceType: Esp32Manager.ConnectionType,
    val signalStrength: Int = 0,
    val isConnected: Boolean = false
)

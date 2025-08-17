package com.example.cc.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.util.Log
import java.net.NetworkInterface
import java.net.InetAddress
import java.net.Socket
import java.net.InetSocketAddress

object NetworkHelper {
    private const val TAG = "NetworkHelper"
    
    /**
     * Get the device's local IP address
     */
    fun getLocalIpAddress(): String? {
        try {
            val networkInterfaces = NetworkInterface.getNetworkInterfaces()
            while (networkInterfaces.hasMoreElements()) {
                val networkInterface = networkInterfaces.nextElement()
                val inetAddresses = networkInterface.inetAddresses
                
                while (inetAddresses.hasMoreElements()) {
                    val inetAddress = inetAddresses.nextElement()
                    if (!inetAddress.isLoopbackAddress && inetAddress.hostAddress.indexOf(':') < 0) {
                        val ip = inetAddress.hostAddress
                        // Prefer WiFi IP addresses (usually 192.168.x.x)
                        if (ip.startsWith("192.168.") || ip.startsWith("10.") || ip.startsWith("172.")) {
                            Log.d(TAG, "Found local IP: $ip")
                            return ip
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting local IP: ${e.message}")
        }
        return null
    }
    
    /**
     * Test MQTT broker connectivity
     */
    fun testBrokerConnectivity(host: String, port: Int, timeout: Int = 5000): Boolean {
        return try {
            val socket = Socket()
            socket.connect(InetSocketAddress(host, port), timeout)
            socket.close()
            Log.i(TAG, "Broker connectivity test successful: $host:$port")
            true
        } catch (e: Exception) {
            Log.w(TAG, "Broker connectivity test failed: $host:$port - ${e.message}")
            false
        }
    }
    
    /**
     * Get recommended broker URL based on network configuration
     */
    fun getRecommendedBrokerUrl(): String {
        val localIp = getLocalIpAddress()
        return if (localIp != null) {
            "tcp://$localIp:1883"
        } else {
            // Fallback to localhost if local IP not found
            "tcp://localhost:1883"
        }
    }
    
    /**
     * Check if device is on WiFi network
     */
    fun isOnWifiNetwork(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }
    
    /**
     * Get network type description
     */
    fun getNetworkType(context: Context): String {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return "No Network"
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return "Unknown"
        
        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WiFi"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Cellular"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
            else -> "Unknown"
        }
    }
    
    /**
     * Get network quality indicator
     */
    fun getNetworkQuality(context: Context): String {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return "Poor"
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return "Poor"
        
        return when {
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) -> "Excellent"
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) -> "Good"
            else -> "Poor"
        }
    }
    
    /**
     * Get network information summary
     */
    fun getNetworkInfo(context: Context): Map<String, String> {
        return mapOf(
            "type" to getNetworkType(context),
            "quality" to getNetworkQuality(context),
            "local_ip" to (getLocalIpAddress() ?: "Unknown"),
            "recommended_broker" to getRecommendedBrokerUrl()
        )
    }
}

package com.example.cc.ui.settings

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.Toast
import android.util.Log
import com.example.cc.R
import com.example.cc.util.MqttConfig
import com.example.cc.util.NetworkHelper
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.button.MaterialButton
import com.google.android.material.textview.MaterialTextView

class MqttSettingsActivity : AppCompatActivity() {
    
    private lateinit var brokerIpInput: TextInputEditText
    private lateinit var brokerPortInput: TextInputEditText
    private lateinit var saveButton: MaterialButton
    private lateinit var testButton: MaterialButton
    private lateinit var autoDetectButton: MaterialButton
    private lateinit var statusText: MaterialTextView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mqtt_settings)
        
        setupViews()
        loadCurrentSettings()
        setupClickListeners()
    }
    
    private fun setupViews() {
        brokerIpInput = findViewById(R.id.etBrokerIp)
        brokerPortInput = findViewById(R.id.etBrokerPort)
        saveButton = findViewById(R.id.btnSave)
        testButton = findViewById(R.id.btnTest)
        autoDetectButton = findViewById(R.id.btnAutoDetect)
        statusText = findViewById(R.id.tvStatus)
        
        // Set up toolbar
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.title = "MQTT Settings"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }
    
    private fun loadCurrentSettings() {
        val prefs = getSharedPreferences("mqtt_settings", MODE_PRIVATE)
        val ip = prefs.getString("broker_ip", "") ?: ""
        val port = prefs.getInt("broker_port", 1883)
        
        brokerIpInput.setText(ip)
        brokerPortInput.setText(port.toString())
        
        updateStatus()
    }
    
    private fun setupClickListeners() {
        saveButton.setOnClickListener {
            saveSettings()
        }
        
        testButton.setOnClickListener {
            testConnection()
        }
        
        autoDetectButton.setOnClickListener {
            autoDetectBroker()
        }
    }
    
    private fun saveSettings() {
        val ip = brokerIpInput.text.toString().trim()
        val portStr = brokerPortInput.text.toString().trim()
        
        if (ip.isEmpty()) {
            Toast.makeText(this, "Please enter a broker IP address", Toast.LENGTH_SHORT).show()
            return
        }
        
        val port = try {
            portStr.toInt()
        } catch (e: NumberFormatException) {
            Toast.makeText(this, "Please enter a valid port number", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (port <= 0 || port > 65535) {
            Toast.makeText(this, "Port must be between 1 and 65535", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Save to SharedPreferences
        val prefs = getSharedPreferences("mqtt_settings", MODE_PRIVATE)
        prefs.edit().apply {
            putString("broker_ip", ip)
            putInt("broker_port", port)
            apply()
        }
        
        // Update MqttConfig
        MqttConfig.setCustomBroker(ip, port)
        
        Toast.makeText(this, "Settings saved successfully", Toast.LENGTH_SHORT).show()
        updateStatus()
    }
    
    private fun testConnection() {
        val ip = brokerIpInput.text.toString().trim()
        val portStr = brokerPortInput.text.toString().trim()
        
        if (ip.isEmpty()) {
            Toast.makeText(this, "Please enter a broker IP address", Toast.LENGTH_SHORT).show()
            return
        }
        
        val port = try {
            portStr.toInt()
        } catch (e: NumberFormatException) {
            Toast.makeText(this, "Please enter a valid port number", Toast.LENGTH_SHORT).show()
            return
        }
        
        testButton.isEnabled = false
        testButton.text = "Testing..."
        
        // Test connection in background
        Thread {
            val isConnected = NetworkHelper.testBrokerConnectivity(ip, port, 5000)
            
            runOnUiThread {
                testButton.isEnabled = true
                testButton.text = "Test Connection"
                
                if (isConnected) {
                    Toast.makeText(this, "✅ Connection successful!", Toast.LENGTH_LONG).show()
                    statusText.text = "Status: ✅ Connected to $ip:$port"
                } else {
                    Toast.makeText(this, "❌ Connection failed", Toast.LENGTH_LONG).show()
                    statusText.text = "Status: ❌ Failed to connect to $ip:$port"
                }
            }
        }.start()
    }
    
    private fun autoDetectBroker() {
        autoDetectButton.isEnabled = false
        autoDetectButton.text = "Detecting..."
        
        Thread {
            val localIp = NetworkHelper.getLocalIpAddress()
            
            runOnUiThread {
                autoDetectButton.isEnabled = true
                autoDetectButton.text = "Auto Detect"
                
                if (localIp != null) {
                    brokerIpInput.setText(localIp)
                    Toast.makeText(this, "Auto-detected IP: $localIp", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Could not auto-detect IP address", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }
    
    private fun updateStatus() {
        val prefs = getSharedPreferences("mqtt_settings", MODE_PRIVATE)
        val ip = prefs.getString("broker_ip", "") ?: ""
        val port = prefs.getInt("broker_port", 1883)
        
        if (ip.isEmpty()) {
            statusText.text = "Status: No broker configured"
        } else {
            statusText.text = "Status: Configured for $ip:$port"
        }
    }
    
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}

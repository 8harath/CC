package com.example.cc.ui.subscriber

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.cc.R
import com.example.cc.ui.base.BaseActivity
import com.example.cc.util.EmergencyAlertMessage
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class IncidentDetailActivity : BaseActivity<View>() {
    
    private val viewModel: SubscriberViewModel by viewModels()
    private var currentIncident: EmergencyAlertMessage? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Get incident from intent
        val incidentJson = intent.getStringExtra("incident_json")
        if (incidentJson != null) {
            try {
                currentIncident = kotlinx.serialization.json.Json.decodeFromString(incidentJson)
                setupViews()
                setupObservers()
            } catch (e: Exception) {
                showToast("Error loading incident details")
                finish()
            }
        } else {
            showToast("No incident data provided")
            finish()
        }
    }
    
    override fun getViewBinding(): View = layoutInflater.inflate(R.layout.activity_incident_detail, null)
    
    override fun setupViews() {
        setupToolbar()
        populateIncidentDetails()
        setupActionButtons()
    }
    
    override fun setupObservers() {
        lifecycleScope.launch {
            viewModel.responseStatus.collectLatest { responseStatus ->
                updateResponseStatus(responseStatus)
            }
        }
        
        lifecycleScope.launch {
            viewModel.isResponding.collectLatest { respondingSet ->
                updateResponseButtons(respondingSet)
            }
        }
    }
    
    private fun setupToolbar() {
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.title = "Incident Details"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }
    
    private fun populateIncidentDetails() {
        currentIncident?.let { incident ->
            // Basic incident info
            findViewById<android.widget.TextView>(R.id.tvVictimName).text = incident.victimName
            findViewById<android.widget.TextView>(R.id.tvIncidentId).text = "ID: ${incident.incidentId}"
            findViewById<android.widget.TextView>(R.id.tvSeverity).text = "Severity: ${incident.severity}"
            
            // Timestamp
            val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.getDefault())
            findViewById<android.widget.TextView>(R.id.tvTimestamp).text = "Time: ${sdf.format(Date(incident.timestamp))}"
            
            // Location
            findViewById<android.widget.TextView>(R.id.tvLocation).text = 
                "Location: ${String.format("%.6f, %.6f", incident.location.latitude, incident.location.longitude)}"
            
            // Medical information
            findViewById<android.widget.TextView>(R.id.tvBloodType).text = "Blood Type: ${incident.medicalInfo.bloodType}"
            
            val allergiesText = if (incident.medicalInfo.allergies.isNotEmpty()) {
                incident.medicalInfo.allergies.joinToString(", ")
            } else {
                "None"
            }
            findViewById<android.widget.TextView>(R.id.tvAllergies).text = "Allergies: $allergiesText"
            
            val medicationsText = if (incident.medicalInfo.medications.isNotEmpty()) {
                incident.medicalInfo.medications.joinToString(", ")
            } else {
                "None"
            }
            findViewById<android.widget.TextView>(R.id.tvMedications).text = "Medications: $medicationsText"
            
            val conditionsText = if (incident.medicalInfo.conditions.isNotEmpty()) {
                incident.medicalInfo.conditions.joinToString(", ")
            } else {
                "None"
            }
            findViewById<android.widget.TextView>(R.id.tvConditions).text = "Conditions: $conditionsText"
        }
    }
    
    private fun setupActionButtons() {
        // Respond button
        findViewById<android.widget.Button>(R.id.btnRespond).setOnClickListener {
            showResponseDialog()
        }
        
        // Cancel response button
        findViewById<android.widget.Button>(R.id.btnCancelResponse).setOnClickListener {
            showCancelResponseDialog()
        }
        
        // Navigation buttons
        findViewById<android.widget.Button>(R.id.btnNavigateMaps).setOnClickListener {
            openGoogleMaps()
        }
        
        findViewById<android.widget.Button>(R.id.btnNavigateWaze).setOnClickListener {
            openWaze()
        }
        
        // Call emergency services
        findViewById<android.widget.Button>(R.id.btnCallEmergency).setOnClickListener {
            callEmergencyServices()
        }
    }
    
    private fun showResponseDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_response_ack, null)
        val etEta = dialogView.findViewById<EditText>(R.id.etEtaMinutes)
        
        AlertDialog.Builder(this)
            .setTitle("Acknowledge Response")
            .setView(dialogView)
            .setPositiveButton("Respond") { _, _ ->
                val etaText = etEta.text.toString()
                val etaMinutes = etaText.toIntOrNull() ?: 5
                val responderName = viewModel.getResponderName()
                
                currentIncident?.let { incident ->
                    viewModel.acknowledgeResponse(incident.incidentId, responderName, etaMinutes)
                    showToast("Response acknowledged - ETA: ${etaMinutes} minutes")
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun showCancelResponseDialog() {
        AlertDialog.Builder(this)
            .setTitle("Cancel Response")
            .setMessage("Are you sure you want to cancel your response to this incident?")
            .setPositiveButton("Cancel Response") { _, _ ->
                val responderName = viewModel.getResponderName()
                currentIncident?.let { incident ->
                    viewModel.cancelResponse(incident.incidentId, responderName)
                    showToast("Response cancelled")
                }
            }
            .setNegativeButton("Keep Responding", null)
            .show()
    }
    
    private fun openGoogleMaps() {
        currentIncident?.let { incident ->
            val intent = viewModel.openNavigation(incident.location.latitude, incident.location.longitude)
            try {
                startActivity(intent)
            } catch (e: Exception) {
                // Fallback to generic geo intent
                val fallbackIntent = Intent(Intent.ACTION_VIEW, 
                    Uri.parse("geo:${incident.location.latitude},${incident.location.longitude}"))
                startActivity(fallbackIntent)
            }
        }
    }
    
    private fun openWaze() {
        currentIncident?.let { incident ->
            val intent = viewModel.openWazeNavigation(incident.location.latitude, incident.location.longitude)
            try {
                startActivity(intent)
            } catch (e: Exception) {
                showToast("Waze not installed. Opening Google Maps instead.")
                openGoogleMaps()
            }
        }
    }
    
    private fun callEmergencyServices() {
        val intent = Intent(Intent.ACTION_DIAL)
        intent.data = Uri.parse("tel:911")
        startActivity(intent)
    }
    
    private fun updateResponseStatus(responseStatus: Map<String, String>) {
        currentIncident?.let { incident ->
            val status = responseStatus[incident.incidentId]
            findViewById<android.widget.TextView>(R.id.tvResponseStatus).text = 
                "Response Status: ${status ?: "No response yet"}"
        }
    }
    
    private fun updateResponseButtons(respondingSet: Set<String>) {
        currentIncident?.let { incident ->
            val isCurrentlyResponding = respondingSet.contains(incident.incidentId)
            findViewById<android.widget.Button>(R.id.btnRespond).visibility = 
                if (isCurrentlyResponding) View.GONE else View.VISIBLE
            findViewById<android.widget.Button>(R.id.btnCancelResponse).visibility = 
                if (isCurrentlyResponding) View.VISIBLE else View.GONE
        }
    }
    
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}

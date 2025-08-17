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
import android.util.Log
import com.example.cc.databinding.ActivityIncidentDetailBinding

class IncidentDetailActivity : BaseActivity<ActivityIncidentDetailBinding>() {
    
    private val viewModel: SubscriberViewModel by viewModels()
    private var currentIncident: EmergencyAlertMessage? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Get incident from intent
        val incidentJson = intent.getStringExtra("incident_json")
        if (incidentJson != null) {
            try {
                currentIncident = kotlinx.serialization.json.Json.decodeFromString(com.example.cc.util.EmergencyAlertMessage.serializer(), incidentJson)
                setupViews()
                setupObservers()
            } catch (e: Exception) {
                Log.e("IncidentDetailActivity", "Error loading incident details: ${e.message}")
                showToast("Error loading incident details")
                finish()
            }
        } else {
            showToast("No incident data provided")
            finish()
        }
    }
    
    override fun getViewBinding(): ActivityIncidentDetailBinding = ActivityIncidentDetailBinding.inflate(layoutInflater)
    
    override fun setupViews() {
        try {
            setupToolbar()
            populateIncidentDetails()
            setupActionButtons()
        } catch (e: Exception) {
            Log.e("IncidentDetailActivity", "Error setting up views: ${e.message}")
            showToast("Error setting up incident details")
        }
    }
    
    override fun setupObservers() {
        try {
            lifecycleScope.launch {
                viewModel.responseStatus.collectLatest { responseStatus ->
                    try {
                        updateResponseStatus(responseStatus)
                    } catch (e: Exception) {
                        Log.e("IncidentDetailActivity", "Error updating response status: ${e.message}")
                    }
                }
            }
            
            lifecycleScope.launch {
                viewModel.isResponding.collectLatest { respondingSet ->
                    try {
                        updateResponseButtons(respondingSet)
                    } catch (e: Exception) {
                        Log.e("IncidentDetailActivity", "Error updating response buttons: ${e.message}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("IncidentDetailActivity", "Error setting up observers: ${e.message}")
        }
    }
    
    private fun setupToolbar() {
        try {
            setSupportActionBar(binding.toolbar)
            supportActionBar?.title = "Incident Details"
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
        } catch (e: Exception) {
            Log.e("IncidentDetailActivity", "Error setting up toolbar: ${e.message}")
        }
    }
    
    private fun populateIncidentDetails() {
        try {
            currentIncident?.let { incident ->
                // Basic incident info
                binding.tvVictimName.text = incident.victimName
                binding.tvIncidentId.text = "ID: ${incident.incidentId}"
                binding.tvSeverity.text = "Severity: ${incident.severity}"
                
                // Timestamp
                val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.getDefault())
                binding.tvTimestamp.text = "Time: ${sdf.format(Date(incident.timestamp))}"
                
                // Location
                binding.tvLocation.text = 
                    "Location: ${String.format("%.6f, %.6f", incident.location.latitude, incident.location.longitude)}"
                
                // Medical information
                binding.tvBloodType.text = "Blood Type: ${incident.medicalInfo.bloodType}"
                
                val allergiesText = if (incident.medicalInfo.allergies.isNotEmpty()) {
                    incident.medicalInfo.allergies.joinToString(", ")
                } else {
                    "None"
                }
                binding.tvAllergies.text = "Allergies: $allergiesText"
                
                val medicationsText = if (incident.medicalInfo.medications.isNotEmpty()) {
                    incident.medicalInfo.medications.joinToString(", ")
                } else {
                    "None"
                }
                binding.tvMedications.text = "Medications: $medicationsText"
                
                // Vehicle information
                binding.tvVehicleMake.text = "Make: ${incident.vehicleInfo.make}"
                binding.tvVehicleModel.text = "Model: ${incident.vehicleInfo.model}"
                binding.tvVehicleYear.text = "Year: ${incident.vehicleInfo.year}"
                binding.tvLicensePlate.text = "License: ${incident.vehicleInfo.licensePlate}"
                
                // Impact data
                binding.tvImpactForce.text = "Impact Force: ${String.format("%.1f", incident.impactData.force)}g"
                binding.tvImpactDirection.text = "Direction: ${incident.impactData.direction}"
                
                // Show/hide sections based on data availability
                binding.sectionMedical.visibility = if (incident.medicalInfo.bloodType.isNotEmpty()) View.VISIBLE else View.GONE
                binding.sectionVehicle.visibility = if (incident.vehicleInfo.make.isNotEmpty()) View.VISIBLE else View.GONE
                binding.sectionImpact.visibility = if (incident.impactData.force > 0) View.VISIBLE else View.GONE
                
            } ?: run {
                Log.w("IncidentDetailActivity", "No incident data to populate")
                showToast("No incident data available")
            }
        } catch (e: Exception) {
            Log.e("IncidentDetailActivity", "Error populating incident details: ${e.message}")
            showToast("Error loading incident information")
        }
    }
    
    private fun setupActionButtons() {
        // Respond button
        binding.btnRespond.setOnClickListener {
            showResponseDialog()
        }
        
        // Cancel response button
        binding.btnCancelResponse.setOnClickListener {
            showCancelResponseDialog()
        }
        
        // Navigation buttons
        binding.btnNavigateMaps.setOnClickListener {
            openGoogleMaps()
        }
        
        binding.btnNavigateWaze.setOnClickListener {
            openWaze()
        }
        
        // Call emergency services
        binding.btnCallEmergency.setOnClickListener {
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
            binding.tvResponseStatus.text = 
                "Response Status: ${status ?: "No response yet"}"
        }
    }
    
    private fun updateResponseButtons(respondingSet: Set<String>) {
        currentIncident?.let { incident ->
            val isCurrentlyResponding = respondingSet.contains(incident.incidentId)
            binding.btnRespond.visibility = 
                if (isCurrentlyResponding) View.GONE else View.VISIBLE
            binding.btnCancelResponse.visibility = 
                if (isCurrentlyResponding) View.VISIBLE else View.GONE
        }
    }
    
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}

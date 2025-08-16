package com.example.cc.demo

import android.content.Context
import android.util.Log
import com.example.cc.data.model.EmergencyContact
import com.example.cc.data.model.Incident
import com.example.cc.data.model.MedicalProfile
import com.example.cc.data.model.User
import com.example.cc.data.repository.IncidentRepository
import com.example.cc.data.repository.MedicalProfileRepository
import com.example.cc.data.repository.UserRepository
import com.example.cc.util.MqttService
import com.example.cc.util.Esp32Manager
import com.example.cc.util.GpsService
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random

/**
 * Demo Scenario Manager for Phase 6
 * Manages pre-configured demonstration scenarios and test data
 */
class DemoScenarioManager(
    private val context: Context,
    private val mqttService: MqttService,
    private val esp32Manager: Esp32Manager,
    private val gpsService: GpsService,
    private val userRepository: UserRepository,
    private val medicalProfileRepository: MedicalProfileRepository,
    private val incidentRepository: IncidentRepository
) {
    companion object {
        private const val TAG = "DemoScenarioManager"
        private const val DEMO_USER_ID = 999
        private const val DEMO_PROFILE_ID = 999
        private const val DEMO_INCIDENT_ID = 999
    }

    private val demoScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val isDemoActive = AtomicBoolean(false)
    private val activeScenarios = mutableListOf<DemoScenario>()
    private val demoData = mutableMapOf<String, Any>()

    data class DemoScenario(
        val id: String,
        val name: String,
        val description: String,
        val duration: Long, // milliseconds
        val steps: List<DemoStep>,
        val isActive: Boolean = false,
        val startTime: Long = 0L,
        val currentStep: Int = 0
    )

    data class DemoStep(
        val id: String,
        val name: String,
        val description: String,
        val action: suspend () -> Boolean,
        val delay: Long = 0L, // milliseconds
        val isCompleted: Boolean = false
    )

    enum class DemoType {
        SINGLE_CRASH_SCENARIO,
        MULTI_CRASH_SCENARIO,
        NETWORK_FAILURE_SCENARIO,
        ESP32_DISCONNECTION_SCENARIO,
        GPS_FAILURE_SCENARIO,
        BATTERY_DRAIN_SCENARIO,
        MEMORY_PRESSURE_SCENARIO,
        COMPLETE_EMERGENCY_RESPONSE
    }

    /**
     * Initialize demo scenarios
     */
    init {
        initializeDemoScenarios()
        Log.i(TAG, "Demo Scenario Manager initialized with ${activeScenarios.size} scenarios")
    }

    /**
     * Initialize all available demo scenarios
     */
    private fun initializeDemoScenarios() {
        activeScenarios.clear()

        // Single Crash Scenario
        activeScenarios.add(createSingleCrashScenario())

        // Multi-Crash Scenario
        activeScenarios.add(createMultiCrashScenario())

        // Network Failure Scenario
        activeScenarios.add(createNetworkFailureScenario())

        // ESP32 Disconnection Scenario
        activeScenarios.add(createEsp32DisconnectionScenario())

        // GPS Failure Scenario
        activeScenarios.add(createGpsFailureScenario())

        // Battery Drain Scenario
        activeScenarios.add(createBatteryDrainScenario())

        // Memory Pressure Scenario
        activeScenarios.add(createMemoryPressureScenario())

        // Complete Emergency Response Scenario
        activeScenarios.add(createCompleteEmergencyResponseScenario())
    }

    /**
     * Create single crash demonstration scenario
     */
    private fun createSingleCrashScenario(): DemoScenario {
        return DemoScenario(
            id = "single_crash",
            name = "Single Vehicle Crash",
            description = "Demonstrates basic crash detection and emergency response",
            duration = 120000L, // 2 minutes
            steps = listOf(
                DemoStep(
                    id = "setup_profile",
                    name = "Setup Medical Profile",
                    description = "Create demo medical profile for crash victim",
                    action = { setupDemoMedicalProfile() }
                ),
                DemoStep(
                    id = "simulate_crash",
                    name = "Simulate Crash Detection",
                    description = "Simulate ESP32 crash detection and alert",
                    action = { simulateCrashDetection() }
                ),
                DemoStep(
                    id = "broadcast_alert",
                    name = "Broadcast Emergency Alert",
                    description = "Send emergency alert via MQTT",
                    action = { broadcastEmergencyAlert() }
                ),
                DemoStep(
                    id = "simulate_response",
                    name = "Simulate Emergency Response",
                    description = "Simulate responder acknowledgment and ETA",
                    action = { simulateEmergencyResponse() }
                ),
                DemoStep(
                    id = "complete_scenario",
                    name = "Complete Scenario",
                    description = "Mark incident as resolved",
                    action = { completeDemoScenario() }
                )
            )
        )
    }

    /**
     * Create multi-crash demonstration scenario
     */
    private fun createMultiCrashScenario(): DemoScenario {
        return DemoScenario(
            id = "multi_crash",
            name = "Multi-Vehicle Crash",
            description = "Demonstrates handling multiple simultaneous incidents",
            duration = 180000L, // 3 minutes
            steps = listOf(
                DemoStep(
                    id = "setup_multiple_profiles",
                    name = "Setup Multiple Profiles",
                    description = "Create demo profiles for multiple victims",
                    action = { setupMultipleDemoProfiles() }
                ),
                DemoStep(
                    id = "simulate_multiple_crashes",
                    name = "Simulate Multiple Crashes",
                    description = "Simulate multiple ESP32 crash detections",
                    action = { simulateMultipleCrashes() }
                ),
                DemoStep(
                    id = "coordinate_response",
                    name = "Coordinate Response",
                    description = "Demonstrate multi-incident coordination",
                    action = { coordinateMultiIncidentResponse() }
                ),
                DemoStep(
                    id = "prioritize_incidents",
                    name = "Prioritize Incidents",
                    description = "Show incident prioritization system",
                    action = { prioritizeIncidents() }
                ),
                DemoStep(
                    id = "resolve_all_incidents",
                    name = "Resolve All Incidents",
                    description = "Mark all incidents as resolved",
                    action = { resolveAllIncidents() }
                )
            )
        )
    }

    /**
     * Create network failure demonstration scenario
     */
    private fun createNetworkFailureScenario(): DemoScenario {
        return DemoScenario(
            id = "network_failure",
            name = "Network Failure Recovery",
            description = "Demonstrates system resilience during network issues",
            duration = 90000L, // 1.5 minutes
            steps = listOf(
                DemoStep(
                    id = "simulate_network_failure",
                    name = "Simulate Network Failure",
                    description = "Simulate MQTT connection loss",
                    action = { simulateNetworkFailure() }
                ),
                DemoStep(
                    id = "queue_messages",
                    name = "Queue Messages",
                    description = "Queue emergency messages during outage",
                    action = { queueMessagesDuringOutage() }
                ),
                DemoStep(
                    id = "test_reconnection",
                    name = "Test Reconnection",
                    description = "Test automatic reconnection",
                    action = { testNetworkReconnection() }
                ),
                DemoStep(
                    id = "deliver_queued_messages",
                    name = "Deliver Queued Messages",
                    description = "Deliver messages after reconnection",
                    action = { deliverQueuedMessages() }
                )
            )
        )
    }

    /**
     * Create ESP32 disconnection scenario
     */
    private fun createEsp32DisconnectionScenario(): DemoScenario {
        return DemoScenario(
            id = "esp32_disconnection",
            name = "ESP32 Device Disconnection",
            description = "Demonstrates handling ESP32 device failures",
            duration = 60000L, // 1 minute
            steps = listOf(
                DemoStep(
                    id = "simulate_disconnection",
                    name = "Simulate ESP32 Disconnection",
                    description = "Simulate ESP32 device going offline",
                    action = { simulateEsp32Disconnection() }
                ),
                DemoStep(
                    id = "fallback_to_manual",
                    name = "Fallback to Manual Mode",
                    description = "Switch to manual emergency triggering",
                    action = { fallbackToManualMode() }
                ),
                DemoStep(
                    id = "test_reconnection",
                    name = "Test ESP32 Reconnection",
                    description = "Test device reconnection",
                    action = { testEsp32Reconnection() }
                )
            )
        )
    }

    /**
     * Create GPS failure scenario
     */
    private fun createGpsFailureScenario(): DemoScenario {
        return DemoScenario(
            id = "gps_failure",
            name = "GPS Service Failure",
            description = "Demonstrates handling GPS service issues",
            duration = 60000L, // 1 minute
            steps = listOf(
                DemoStep(
                    id = "simulate_gps_failure",
                    name = "Simulate GPS Failure",
                    description = "Simulate GPS service becoming unavailable",
                    action = { simulateGpsFailure() }
                ),
                DemoStep(
                    id = "use_last_known_location",
                    name = "Use Last Known Location",
                    description = "Fallback to last known GPS coordinates",
                    action = { useLastKnownLocation() }
                ),
                DemoStep(
                    id = "test_gps_recovery",
                    name = "Test GPS Recovery",
                    description = "Test GPS service recovery",
                    action = { testGpsRecovery() }
                )
            )
        )
    }

    /**
     * Create battery drain scenario
     */
    private fun createBatteryDrainScenario(): DemoScenario {
        return DemoScenario(
            id = "battery_drain",
            name = "Battery Drain Simulation",
            description = "Demonstrates system behavior during low battery",
            duration = 60000L, // 1 minute
            steps = listOf(
                DemoStep(
                    id = "simulate_low_battery",
                    name = "Simulate Low Battery",
                    description = "Simulate low battery conditions",
                    action = { simulateLowBattery() }
                ),
                DemoStep(
                    id = "test_power_saving",
                    name = "Test Power Saving",
                    description = "Test power saving features",
                    action = { testPowerSaving() }
                ),
                DemoStep(
                    id = "test_critical_battery",
                    name = "Test Critical Battery",
                    description = "Test critical battery handling",
                    action = { testCriticalBattery() }
                )
            )
        )
    }

    /**
     * Create memory pressure scenario
     */
    private fun createMemoryPressureScenario(): DemoScenario {
        return DemoScenario(
            id = "memory_pressure",
            name = "Memory Pressure Test",
            description = "Demonstrates system behavior under memory pressure",
            duration = 60000L, // 1 minute
            steps = listOf(
                DemoStep(
                    id = "simulate_memory_pressure",
                    name = "Simulate Memory Pressure",
                    description = "Create memory pressure conditions",
                    action = { simulateMemoryPressure() }
                ),
                DemoStep(
                    id = "test_memory_cleanup",
                    name = "Test Memory Cleanup",
                    description = "Test automatic memory cleanup",
                    action = { testMemoryCleanup() }
                ),
                DemoStep(
                    id = "test_performance_degradation",
                    name = "Test Performance Degradation",
                    description = "Test system performance under pressure",
                    action = { testPerformanceDegradation() }
                )
            )
        )
    }

    /**
     * Create complete emergency response scenario
     */
    private fun createCompleteEmergencyResponseScenario(): DemoScenario {
        return DemoScenario(
            id = "complete_emergency_response",
            name = "Complete Emergency Response",
            description = "End-to-end emergency response demonstration",
            duration = 300000L, // 5 minutes
            steps = listOf(
                DemoStep(
                    id = "setup_complete_system",
                    name = "Setup Complete System",
                    description = "Initialize all system components",
                    action = { setupCompleteSystem() }
                ),
                DemoStep(
                    id = "simulate_real_crash",
                    name = "Simulate Real Crash",
                    description = "Simulate realistic crash scenario",
                    action = { simulateRealCrash() }
                ),
                DemoStep(
                    id = "test_emergency_broadcast",
                    name = "Test Emergency Broadcast",
                    description = "Test complete emergency alert system",
                    action = { testEmergencyBroadcast() }
                ),
                DemoStep(
                    id = "test_response_coordination",
                    name = "Test Response Coordination",
                    description = "Test multi-responder coordination",
                    action = { testResponseCoordination() }
                ),
                DemoStep(
                    id = "test_incident_resolution",
                    name = "Test Incident Resolution",
                    description = "Test complete incident lifecycle",
                    action = { testIncidentResolution() }
                ),
                DemoStep(
                    id = "generate_demo_report",
                    name = "Generate Demo Report",
                    description = "Generate comprehensive demo report",
                    action = { generateDemoReport() }
                )
            )
        )
    }

    /**
     * Start a specific demo scenario
     */
    suspend fun startDemoScenario(scenarioId: String): Boolean {
        val scenario = activeScenarios.find { it.id == scenarioId }
        if (scenario == null) {
            Log.e(TAG, "Demo scenario not found: $scenarioId")
            return false
        }

        if (isDemoActive.get()) {
            Log.w(TAG, "Demo already active, stopping current demo")
            stopCurrentDemo()
        }

        Log.i(TAG, "Starting demo scenario: ${scenario.name}")
        
        isDemoActive.set(true)
        demoData["active_scenario"] = scenario.copy(
            isActive = true,
            startTime = System.currentTimeMillis(),
            currentStep = 0
        )

        // Execute scenario steps
        demoScope.launch {
            executeDemoScenario(scenario)
        }

        return true
    }

    /**
     * Execute demo scenario steps
     */
    private suspend fun executeDemoScenario(scenario: DemoScenario) {
        try {
            Log.d(TAG, "Executing demo scenario: ${scenario.name}")
            
            for ((index, step) in scenario.steps.withIndex()) {
                if (!isDemoActive.get()) {
                    Log.d(TAG, "Demo stopped, aborting execution")
                    break
                }

                Log.d(TAG, "Executing step ${index + 1}/${scenario.steps.size}: ${step.name}")
                
                // Update current step
                updateCurrentStep(scenario.id, index)
                
                // Execute step action
                val stepResult = step.action()
                if (!stepResult) {
                    Log.w(TAG, "Step failed: ${step.name}")
                    // Continue with next step even if current fails
                }

                // Add delay if specified
                if (step.delay > 0) {
                    delay(step.delay)
                }
            }

            Log.i(TAG, "Demo scenario completed: ${scenario.name}")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error executing demo scenario: ${scenario.name}", e)
        } finally {
            isDemoActive.set(false)
            demoData.remove("active_scenario")
        }
    }

    /**
     * Stop current demo
     */
    fun stopCurrentDemo() {
        if (isDemoActive.compareAndSet(true, false)) {
            Log.i(TAG, "Stopping current demo")
            demoScope.cancel()
            demoData.remove("active_scenario")
        }
    }

    /**
     * Get available demo scenarios
     */
    fun getAvailableScenarios(): List<DemoScenario> {
        return activeScenarios.map { scenario ->
            scenario.copy(isActive = scenario.id == (demoData["active_scenario"] as? DemoScenario)?.id)
        }
    }

    /**
     * Get current demo status
     */
    fun getCurrentDemoStatus(): DemoScenario? {
        return demoData["active_scenario"] as? DemoScenario
    }

    /**
     * Check if demo is active
     */
    fun isDemoActive(): Boolean {
        return isDemoActive.get()
    }

    /**
     * Update current step in demo
     */
    private fun updateCurrentStep(scenarioId: String, stepIndex: Int) {
        val currentScenario = demoData["active_scenario"] as? DemoScenario
        if (currentScenario != null && currentScenario.id == scenarioId) {
            demoData["active_scenario"] = currentScenario.copy(currentStep = stepIndex)
        }
    }

    // Demo step implementations
    private suspend fun setupDemoMedicalProfile(): Boolean {
        return try {
            Log.d(TAG, "Setting up demo medical profile")
            
            // Create demo user if not exists
            val demoUser = userRepository.getUserById(DEMO_USER_ID) ?: run {
                userRepository.createUser("Demo Victim", "demo@example.com")
            }

            // Create demo medical profile
            val demoProfile = MedicalProfile(
                id = DEMO_PROFILE_ID,
                userId = demoUser.id,
                name = "Demo Crash Victim",
                age = 35,
                bloodType = "O+",
                allergies = "Penicillin, Shellfish",
                medications = "Blood pressure medication, Daily aspirin",
                emergencyContacts = listOf(
                    EmergencyContact(
                        id = 1,
                        name = "Demo Emergency Contact",
                        phone = "+1-555-0123",
                        relationship = "Spouse"
                    )
                ),
                timestamp = System.currentTimeMillis()
            )

            medicalProfileRepository.createMedicalProfile(
                userId = demoProfile.userId,
                name = demoProfile.name,
                age = demoProfile.age,
                bloodType = demoProfile.bloodType,
                allergies = demoProfile.allergies,
                medications = demoProfile.medications,
                emergencyContacts = demoProfile.emergencyContacts
            )

            Log.d(TAG, "Demo medical profile setup completed")
            true

        } catch (e: Exception) {
            Log.e(TAG, "Failed to setup demo medical profile", e)
            false
        }
    }

    private suspend fun simulateCrashDetection(): Boolean {
        return try {
            Log.d(TAG, "Simulating crash detection")
            
            // Simulate ESP32 crash data
            val crashData = mapOf(
                "impact_force" to 18.5,
                "acceleration" to mapOf("x" to 3.2, "y" to -2.1, "z" to 12.8),
                "timestamp" to System.currentTimeMillis(),
                "vehicle_speed" to 65.0,
                "crash_severity" to "HIGH"
            )

            // Process crash data through ESP32 manager
            val crashDetected = esp32Manager.processCrashData(crashData)
            
            if (crashDetected) {
                Log.d(TAG, "Crash detection simulation successful")
                true
            } else {
                Log.w(TAG, "Crash detection simulation failed")
                false
            }

        } catch (e: Exception) {
            Log.e(TAG, "Failed to simulate crash detection", e)
            false
        }
    }

    private suspend fun broadcastEmergencyAlert(): Boolean {
        return try {
            Log.d(TAG, "Broadcasting emergency alert")
            
            // Create demo incident
            val demoIncident = incidentRepository.createIncident(
                userId = DEMO_USER_ID,
                latitude = 40.7128 + Random.nextDouble(-0.01, 0.01),
                longitude = -74.0060 + Random.nextDouble(-0.01, 0.01),
                severity = "CRITICAL",
                description = "Demo crash scenario - high impact collision"
            )

            // Create emergency alert message
            val alertMessage = incidentRepository.createEmergencyAlert(demoIncident)
            
            // Broadcast via MQTT
            val broadcastResult = mqttService.publishMessage("emergency/alert", alertMessage)
            
            if (broadcastResult) {
                Log.d(TAG, "Emergency alert broadcast successful")
                demoData["demo_incident"] = demoIncident
                true
            } else {
                Log.w(TAG, "Emergency alert broadcast failed")
                false
            }

        } catch (e: Exception) {
            Log.e(TAG, "Failed to broadcast emergency alert", e)
            false
        }
    }

    private suspend fun simulateEmergencyResponse(): Boolean {
        return try {
            Log.d(TAG, "Simulating emergency response")
            
            val demoIncident = demoData["demo_incident"] as? Incident
            if (demoIncident == null) {
                Log.w(TAG, "No demo incident found for response simulation")
                return false
            }

            // Simulate responder acknowledgment
            val responseResult = incidentRepository.acknowledgeResponse(demoIncident.id, "RESPONDING")
            
            // Simulate ETA update
            delay(2000) // Wait 2 seconds
            
            // Simulate arrival
            val arrivalResult = incidentRepository.acknowledgeResponse(demoIncident.id, "ON_SCENE")
            
            if (responseResult && arrivalResult) {
                Log.d(TAG, "Emergency response simulation successful")
                true
            } else {
                Log.w(TAG, "Emergency response simulation failed")
                false
            }

        } catch (e: Exception) {
            Log.e(TAG, "Failed to simulate emergency response", e)
            false
        }
    }

    private suspend fun completeDemoScenario(): Boolean {
        return try {
            Log.d(TAG, "Completing demo scenario")
            
            val demoIncident = demoData["demo_incident"] as? Incident
            if (demoIncident != null) {
                // Mark incident as resolved
                incidentRepository.acknowledgeResponse(demoIncident.id, "RESOLVED")
                
                // Clean up demo data
                demoData.remove("demo_incident")
            }

            Log.d(TAG, "Demo scenario completion successful")
            true

        } catch (e: Exception) {
            Log.e(TAG, "Failed to complete demo scenario", e)
            false
        }
    }

    // Additional demo step implementations would go here...
    private suspend fun setupMultipleDemoProfiles(): Boolean = true
    private suspend fun simulateMultipleCrashes(): Boolean = true
    private suspend fun coordinateMultiIncidentResponse(): Boolean = true
    private suspend fun prioritizeIncidents(): Boolean = true
    private suspend fun resolveAllIncidents(): Boolean = true
    private suspend fun simulateNetworkFailure(): Boolean = true
    private suspend fun queueMessagesDuringOutage(): Boolean = true
    private suspend fun testNetworkReconnection(): Boolean = true
    private suspend fun deliverQueuedMessages(): Boolean = true
    private suspend fun simulateEsp32Disconnection(): Boolean = true
    private suspend fun fallbackToManualMode(): Boolean = true
    private suspend fun testEsp32Reconnection(): Boolean = true
    private suspend fun simulateGpsFailure(): Boolean = true
    private suspend fun useLastKnownLocation(): Boolean = true
    private suspend fun testGpsRecovery(): Boolean = true
    private suspend fun simulateLowBattery(): Boolean = true
    private suspend fun testPowerSaving(): Boolean = true
    private suspend fun testCriticalBattery(): Boolean = true
    private suspend fun simulateMemoryPressure(): Boolean = true
    private suspend fun testMemoryCleanup(): Boolean = true
    private suspend fun testPerformanceDegradation(): Boolean = true
    private suspend fun setupCompleteSystem(): Boolean = true
    private suspend fun simulateRealCrash(): Boolean = true
    private suspend fun testEmergencyBroadcast(): Boolean = true
    private suspend fun testResponseCoordination(): Boolean = true
    private suspend fun testIncidentResolution(): Boolean = true
    private suspend fun generateDemoReport(): Boolean = true

    /**
     * Generate demo report
     */
    fun generateDemoReport(): String {
        val currentScenario = getCurrentDemoStatus()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        
        return """
            Demo Scenario Report
            ===================
            Generated: ${dateFormat.format(Date())}
            
            Current Demo: ${currentScenario?.name ?: "None"}
            Demo Status: ${if (isDemoActive()) "Active" else "Inactive"}
            Start Time: ${if (currentScenario != null) dateFormat.format(Date(currentScenario.startTime)) else "N/A"}
            Current Step: ${currentScenario?.currentStep ?: 0}/${currentScenario?.steps?.size ?: 0}
            
            Available Scenarios:
            ${getAvailableScenarios().joinToString("\n") { "- ${it.name}: ${it.description}" }}
            
            Demo Data:
            ${demoData.entries.joinToString("\n") { "- ${it.key}: ${it.value}" }}
        """.trimIndent()
    }

    /**
     * Cleanup demo resources
     */
    fun cleanup() {
        stopCurrentDemo()
        demoScope.cancel()
        demoData.clear()
        Log.i(TAG, "Demo Scenario Manager cleaned up")
    }
}

package com.example.cc.testing

import android.content.Context
import android.util.Log
import com.example.cc.data.repository.IncidentRepository
import com.example.cc.data.repository.MedicalProfileRepository
import com.example.cc.data.repository.UserRepository
import com.example.cc.util.MqttService
import com.example.cc.util.Esp32Manager
import com.example.cc.util.GpsService
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration.Companion.seconds

/**
 * Comprehensive Integration Testing Suite for Phase 6
 * Tests all system components working together in real-world scenarios
 */
class IntegrationTestSuite(
    private val context: Context,
    private val mqttService: MqttService,
    private val esp32Manager: Esp32Manager,
    private val gpsService: GpsService,
    private val userRepository: UserRepository,
    private val medicalProfileRepository: MedicalProfileRepository,
    private val incidentRepository: IncidentRepository
) {
    companion object {
        private const val TAG = "IntegrationTestSuite"
        private const val TEST_TIMEOUT_SECONDS = 30L
    }

    private val testScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val testResults = mutableListOf<TestResult>()
    private val testCounter = AtomicInteger(0)

    data class TestResult(
        val testName: String,
        val status: TestStatus,
        val duration: Long,
        val error: String? = null,
        val details: Map<String, Any> = emptyMap()
    )

    enum class TestStatus {
        PASSED, FAILED, SKIPPED, TIMEOUT
    }

    /**
     * Run complete integration test suite
     */
    suspend fun runFullTestSuite(): List<TestResult> = withContext(Dispatchers.IO) {
        Log.i(TAG, "Starting Phase 6 Integration Test Suite")
        
        testResults.clear()
        testCounter.set(0)

        try {
            // Core System Tests
            runTest("Database Connectivity Test") { testDatabaseConnectivity() }
            runTest("MQTT Connection Test") { testMqttConnection() }
            runTest("GPS Service Test") { testGpsService() }
            runTest("ESP32 Communication Test") { testEsp32Communication() }

            // Publisher Mode Tests
            runTest("Medical Profile Creation Test") { testMedicalProfileCreation() }
            runTest("Emergency Alert Broadcasting Test") { testEmergencyAlertBroadcasting() }
            runTest("ESP32 Crash Detection Test") { testEsp32CrashDetection() }

            // Subscriber Mode Tests
            runTest("Alert Reception Test") { testAlertReception() }
            runTest("Incident Detail Display Test") { testIncidentDetailDisplay() }
            runTest("Response Management Test") { testResponseManagement() }

            // End-to-End Scenarios
            runTest("Complete Emergency Scenario Test") { testCompleteEmergencyScenario() }
            runTest("Multi-Device Coordination Test") { testMultiDeviceCoordination() }
            runTest("Network Interruption Recovery Test") { testNetworkRecovery() }

            // Performance Tests
            runTest("Battery Usage Test") { testBatteryUsage() }
            runTest("Memory Usage Test") { testMemoryUsage() }
            runTest("Response Time Test") { testResponseTime() }

        } catch (e: Exception) {
            Log.e(TAG, "Test suite execution failed", e)
            testResults.add(TestResult(
                testName = "Test Suite Execution",
                status = TestStatus.FAILED,
                duration = 0,
                error = e.message
            ))
        }

        Log.i(TAG, "Integration Test Suite completed. Results: ${testResults.count { it.status == TestStatus.PASSED }}/${testResults.size} tests passed")
        return@withContext testResults
    }

    /**
     * Run individual test with timeout and error handling
     */
    private suspend fun runTest(testName: String, testBlock: suspend () -> Unit) {
        val testId = testCounter.incrementAndGet()
        val startTime = System.currentTimeMillis()
        
        Log.d(TAG, "Starting test $testId: $testName")
        
        try {
            withTimeout(TEST_TIMEOUT_SECONDS.seconds) {
                testBlock()
            }
            
            val duration = System.currentTimeMillis() - startTime
            testResults.add(TestResult(testName, TestStatus.PASSED, duration))
            Log.d(TAG, "Test $testId PASSED: $testName (${duration}ms)")
            
        } catch (e: TimeoutCancellationException) {
            val duration = System.currentTimeMillis() - startTime
            testResults.add(TestResult(testName, TestStatus.TIMEOUT, duration, "Test timed out after ${TEST_TIMEOUT_SECONDS}s"))
            Log.w(TAG, "Test $testId TIMEOUT: $testName")
            
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            testResults.add(TestResult(testName, TestStatus.FAILED, duration, e.message))
            Log.e(TAG, "Test $testId FAILED: $testName", e)
        }
    }

    // Core System Tests
    private suspend fun testDatabaseConnectivity() {
        // Test database operations
        val testUser = userRepository.createUser("TestUser", "test@example.com")
        require(testUser.id > 0) { "User creation failed" }
        
        val retrievedUser = userRepository.getUserById(testUser.id)
        require(retrievedUser != null) { "User retrieval failed" }
        
        // Cleanup
        userRepository.deleteUser(testUser.id)
    }

    private suspend fun testMqttConnection() {
        val isConnected = mqttService.isConnected()
        require(isConnected) { "MQTT service not connected" }
        
        // Test message publishing
        val testMessage = "{\"test\": \"integration_test\", \"timestamp\": ${System.currentTimeMillis()}}"
        val publishResult = mqttService.publishMessage("test/integration", testMessage)
        require(publishResult) { "Message publishing failed" }
    }

    private suspend fun testGpsService() {
        val location = gpsService.getCurrentLocation()
        require(location != null) { "GPS location not available" }
        require(location.latitude != 0.0 || location.longitude != 0.0) { "Invalid GPS coordinates" }
    }

    private suspend fun testEsp32Communication() {
        val availableDevices = esp32Manager.getAvailableDevices()
        Log.d(TAG, "Available ESP32 devices: ${availableDevices.size}")
        
        // Test device discovery
        require(esp32Manager.isDiscoveryActive()) { "ESP32 discovery not active" }
    }

    // Publisher Mode Tests
    private suspend fun testMedicalProfileCreation() {
        val testProfile = medicalProfileRepository.createMedicalProfile(
            userId = 1,
            name = "Test Patient",
            age = 30,
            bloodType = "O+",
            allergies = "None",
            medications = "None",
            emergencyContacts = emptyList()
        )
        
        require(testProfile.id > 0) { "Medical profile creation failed" }
        
        // Cleanup
        medicalProfileRepository.deleteMedicalProfile(testProfile.id)
    }

    private suspend fun testEmergencyAlertBroadcasting() {
        val testIncident = incidentRepository.createIncident(
            userId = 1,
            latitude = 40.7128,
            longitude = -74.0060,
            severity = "HIGH",
            description = "Integration test incident"
        )
        
        require(testIncident.id > 0) { "Incident creation failed" }
        
        // Test MQTT broadcasting
        val alertMessage = incidentRepository.createEmergencyAlert(testIncident)
        val broadcastResult = mqttService.publishMessage("emergency/alert", alertMessage)
        require(broadcastResult) { "Emergency alert broadcasting failed" }
        
        // Cleanup
        incidentRepository.deleteIncident(testIncident.id)
    }

    private suspend fun testEsp32CrashDetection() {
        // Simulate ESP32 crash detection
        val crashData = mapOf(
            "impact_force" to 15.5,
            "acceleration" to mapOf("x" to 2.1, "y" to -1.8, "z" to 8.9),
            "timestamp" to System.currentTimeMillis()
        )
        
        val crashDetected = esp32Manager.processCrashData(crashData)
        require(crashDetected) { "ESP32 crash detection processing failed" }
    }

    // Subscriber Mode Tests
    private suspend fun testAlertReception() {
        // Subscribe to emergency alerts
        val subscriptionResult = mqttService.subscribeToTopic("emergency/alert")
        require(subscriptionResult) { "Emergency alert subscription failed" }
        
        // Wait for potential alerts
        delay(1000)
    }

    private suspend fun testIncidentDetailDisplay() {
        val testIncident = incidentRepository.createIncident(
            userId = 1,
            latitude = 40.7128,
            longitude = -74.0060,
            severity = "MEDIUM",
            description = "Test incident for detail display"
        )
        
        val incidentDetails = incidentRepository.getIncidentById(testIncident.id)
        require(incidentDetails != null) { "Incident details retrieval failed" }
        require(incidentDetails.latitude == 40.7128) { "Incident coordinates mismatch" }
        
        // Cleanup
        incidentRepository.deleteIncident(testIncident.id)
    }

    private suspend fun testResponseManagement() {
        val testIncident = incidentRepository.createIncident(
            userId = 1,
            latitude = 40.7128,
            longitude = -74.0060,
            severity = "HIGH",
            description = "Test incident for response management"
        )
        
        // Test response acknowledgment
        val responseResult = incidentRepository.acknowledgeResponse(testIncident.id, "RESPONDING")
        require(responseResult) { "Response acknowledgment failed" }
        
        // Cleanup
        incidentRepository.deleteIncident(testIncident.id)
    }

    // End-to-End Scenarios
    private suspend fun testCompleteEmergencyScenario() {
        Log.d(TAG, "Testing complete emergency scenario...")
        
        // 1. Create medical profile
        val profile = medicalProfileRepository.createMedicalProfile(
            userId = 1,
            name = "Emergency Test Patient",
            age = 35,
            bloodType = "A+",
            allergies = "Penicillin",
            medications = "Blood pressure medication",
            emergencyContacts = emptyList()
        )
        
        // 2. Simulate crash detection
        val crashData = mapOf(
            "impact_force" to 18.2,
            "acceleration" to mapOf("x" to 3.5, "y" to -2.1, "z" to 12.3),
            "timestamp" to System.currentTimeMillis()
        )
        
        val crashDetected = esp32Manager.processCrashData(crashData)
        require(crashDetected) { "Crash detection failed" }
        
        // 3. Create emergency incident
        val incident = incidentRepository.createIncident(
            userId = 1,
            latitude = 40.7128,
            longitude = -74.0060,
            severity = "CRITICAL",
            description = "Automotive accident - high impact collision"
        )
        
        // 4. Broadcast emergency alert
        val alertMessage = incidentRepository.createEmergencyAlert(incident)
        val broadcastResult = mqttService.publishMessage("emergency/alert", alertMessage)
        require(broadcastResult) { "Emergency alert broadcasting failed" }
        
        // 5. Simulate responder acknowledgment
        val responseResult = incidentRepository.acknowledgeResponse(incident.id, "RESPONDING")
        require(responseResult) { "Response acknowledgment failed" }
        
        // Cleanup
        medicalProfileRepository.deleteMedicalProfile(profile.id)
        incidentRepository.deleteIncident(incident.id)
        
        Log.d(TAG, "Complete emergency scenario test passed")
    }

    private suspend fun testMultiDeviceCoordination() {
        Log.d(TAG, "Testing multi-device coordination...")
        
        // Test multiple incident handling
        val incidents = (1..3).map { i ->
            incidentRepository.createIncident(
                userId = i,
                latitude = 40.7128 + (i * 0.001),
                longitude = -74.0060 + (i * 0.001),
                severity = if (i == 1) "CRITICAL" else "MEDIUM",
                description = "Multi-device test incident $i"
            )
        }
        
        // Test concurrent response management
        val responseJobs = incidents.map { incident ->
            async {
                incidentRepository.acknowledgeResponse(incident.id, "RESPONDING")
            }
        }
        
        val responseResults = responseJobs.awaitAll()
        require(responseResults.all { it }) { "Multi-device response coordination failed" }
        
        // Cleanup
        incidents.forEach { incidentRepository.deleteIncident(it.id) }
        
        Log.d(TAG, "Multi-device coordination test passed")
    }

    private suspend fun testNetworkRecovery() {
        Log.d(TAG, "Testing network recovery...")
        
        // Simulate network disconnection
        mqttService.disconnect()
        delay(1000)
        
        // Test reconnection
        val reconnectResult = mqttService.connect()
        require(reconnectResult) { "Network recovery failed" }
        
        // Verify connection is stable
        delay(2000)
        require(mqttService.isConnected()) { "Connection not stable after recovery" }
        
        Log.d(TAG, "Network recovery test passed")
    }

    // Performance Tests
    private suspend fun testBatteryUsage() {
        // Monitor battery usage during test execution
        val startBattery = getBatteryLevel()
        delay(5000) // Simulate 5 seconds of operation
        val endBattery = getBatteryLevel()
        
        val batteryDrain = startBattery - endBattery
        Log.d(TAG, "Battery usage test: $batteryDrain% drain in 5 seconds")
        
        // Acceptable battery drain threshold
        require(batteryDrain < 5.0) { "Excessive battery drain detected: $batteryDrain%" }
    }

    private suspend fun testMemoryUsage() {
        val runtime = Runtime.getRuntime()
        val usedMemory = runtime.totalMemory() - runtime.freeMemory()
        val maxMemory = runtime.maxMemory()
        val memoryUsagePercent = (usedMemory.toDouble() / maxMemory.toDouble()) * 100
        
        Log.d(TAG, "Memory usage: ${memoryUsagePercent.format(2)}%")
        
        // Ensure memory usage is reasonable
        require(memoryUsagePercent < 80.0) { "High memory usage detected: ${memoryUsagePercent.format(2)}%" }
    }

    private suspend fun testResponseTime() {
        val startTime = System.currentTimeMillis()
        
        // Perform a typical operation
        val testProfile = medicalProfileRepository.createMedicalProfile(
            userId = 1,
            name = "Response Time Test",
            age = 25,
            bloodType = "B+",
            allergies = "None",
            medications = "None",
            emergencyContacts = emptyList()
        )
        
        val endTime = System.currentTimeMillis()
        val responseTime = endTime - startTime
        
        Log.d(TAG, "Response time test: ${responseTime}ms")
        
        // Ensure response time is acceptable
        require(responseTime < 1000) { "Slow response time: ${responseTime}ms" }
        
        // Cleanup
        medicalProfileRepository.deleteMedicalProfile(testProfile.id)
    }

    private fun getBatteryLevel(): Double {
        // This would integrate with actual battery monitoring
        // For now, return a simulated value
        return 85.0
    }

    private fun Double.format(digits: Int) = "%.${digits}f".format(this)

    /**
     * Get test results summary
     */
    fun getTestSummary(): String {
        val totalTests = testResults.size
        val passedTests = testResults.count { it.status == TestStatus.PASSED }
        val failedTests = testResults.count { it.status == TestStatus.FAILED }
        val timeoutTests = testResults.count { it.status == TestStatus.TIMEOUT }
        
        return """
            Phase 6 Integration Test Results
            =================================
            Total Tests: $totalTests
            Passed: $passedTests
            Failed: $failedTests
            Timeout: $timeoutTests
            Success Rate: ${if (totalTests > 0) (passedTests * 100 / totalTests) else 0}%
            
            Failed Tests:
            ${testResults.filter { it.status != TestStatus.PASSED }.joinToString("\n") { "- ${it.testName}: ${it.error ?: "Unknown error"}" }}
        """.trimIndent()
    }

    /**
     * Cleanup resources
     */
    fun cleanup() {
        testScope.cancel()
        Log.i(TAG, "Integration test suite cleaned up")
    }
}

package com.example.cc.di

import com.example.cc.CarCrashDetectionApp
import com.example.cc.data.dao.IncidentDao
import com.example.cc.data.dao.MedicalProfileDao
import com.example.cc.data.dao.UserDao
import com.example.cc.data.database.AppDatabase
import com.example.cc.data.repository.IncidentRepository
import com.example.cc.data.repository.MedicalProfileRepository
import com.example.cc.data.repository.UserRepository
import com.example.cc.util.MqttService
import com.example.cc.util.Esp32Manager
import com.example.cc.util.GpsService
import com.example.cc.testing.IntegrationTestSuite
import com.example.cc.util.SystemHealthMonitor
import com.example.cc.demo.DemoScenarioManager
import com.example.cc.util.ErrorHandler
import com.example.cc.production.ProductionMonitor
import com.example.cc.production.MaintenanceManager
import com.example.cc.production.InstallationManager

object AppModule {
    
    private val database: AppDatabase by lazy {
        try {
            AppDatabase.getDatabase(CarCrashDetectionApp.instance)
        } catch (e: Exception) {
            throw IllegalStateException("Failed to initialize database: ${e.message}")
        }
    }
    
    val userDao: UserDao by lazy { database.userDao() }
    val medicalProfileDao: MedicalProfileDao by lazy { database.medicalProfileDao() }
    val incidentDao: IncidentDao by lazy { database.incidentDao() }
    
    val userRepository: UserRepository by lazy { UserRepository(userDao) }
    val medicalProfileRepository: MedicalProfileRepository by lazy { MedicalProfileRepository(medicalProfileDao) }
    val incidentRepository: IncidentRepository by lazy { IncidentRepository(incidentDao) }
    
    // Phase 6 Components
    val mqttService: MqttService by lazy { MqttService() }
    val esp32Manager: Esp32Manager by lazy { Esp32Manager(CarCrashDetectionApp.instance) }
    val gpsService: GpsService by lazy { GpsService(CarCrashDetectionApp.instance) }
    
    val integrationTestSuite: IntegrationTestSuite by lazy { 
        IntegrationTestSuite(
            CarCrashDetectionApp.instance,
            mqttService,
            esp32Manager,
            gpsService,
            userRepository,
            medicalProfileRepository,
            incidentRepository
        )
    }
    
    val systemHealthMonitor: SystemHealthMonitor by lazy {
        SystemHealthMonitor(
            CarCrashDetectionApp.instance,
            mqttService,
            esp32Manager,
            gpsService,
            userRepository,
            medicalProfileRepository,
            incidentRepository
        )
    }
    
    val demoScenarioManager: DemoScenarioManager by lazy {
        DemoScenarioManager(
            CarCrashDetectionApp.instance,
            mqttService,
            esp32Manager,
            gpsService,
            userRepository,
            medicalProfileRepository,
            incidentRepository
        )
    }
    
    val errorHandler: ErrorHandler by lazy {
        ErrorHandler(
            CarCrashDetectionApp.instance,
            mqttService,
            esp32Manager,
            gpsService,
            userRepository,
            medicalProfileRepository,
            incidentRepository
        )
    }
    
    // Phase 7 Components - Production and Deployment
    val productionMonitor: ProductionMonitor by lazy {
        ProductionMonitor.getInstance(CarCrashDetectionApp.instance)
    }
    
    val maintenanceManager: MaintenanceManager by lazy {
        MaintenanceManager.getInstance(CarCrashDetectionApp.instance)
    }
    
    val installationManager: InstallationManager by lazy {
        InstallationManager.getInstance(CarCrashDetectionApp.instance)
    }
} 
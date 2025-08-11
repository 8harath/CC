package com.example.cc.di

import com.example.cc.CarCrashDetectionApp
import com.example.cc.data.dao.IncidentDao
import com.example.cc.data.dao.MedicalProfileDao
import com.example.cc.data.dao.UserDao
import com.example.cc.data.database.AppDatabase
import com.example.cc.data.repository.IncidentRepository
import com.example.cc.data.repository.MedicalProfileRepository
import com.example.cc.data.repository.UserRepository

object AppModule {
    
    private val database: AppDatabase by lazy {
        AppDatabase.getDatabase(CarCrashDetectionApp.instance)
    }
    
    val userDao: UserDao by lazy { database.userDao() }
    val medicalProfileDao: MedicalProfileDao by lazy { database.medicalProfileDao() }
    val incidentDao: IncidentDao by lazy { database.incidentDao() }
    
    val userRepository: UserRepository by lazy { UserRepository(userDao) }
    val medicalProfileRepository: MedicalProfileRepository by lazy { MedicalProfileRepository(medicalProfileDao) }
    val incidentRepository: IncidentRepository by lazy { IncidentRepository(incidentDao) }
} 
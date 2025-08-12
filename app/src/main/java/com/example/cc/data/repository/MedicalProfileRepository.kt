package com.example.cc.data.repository

import com.example.cc.data.dao.MedicalProfileDao
import com.example.cc.data.model.MedicalProfile
class MedicalProfileRepository(
    private val medicalProfileDao: MedicalProfileDao
) {
    suspend fun getMedicalProfileByUserId(userId: Long): MedicalProfile? = 
        medicalProfileDao.getMedicalProfileByUserId(userId)
    
    suspend fun getMedicalProfileById(id: Long): MedicalProfile? = 
        medicalProfileDao.getMedicalProfileById(id)
    
    suspend fun insertMedicalProfile(medicalProfile: MedicalProfile): Long = 
        medicalProfileDao.insertMedicalProfile(medicalProfile)
    
    suspend fun updateMedicalProfile(medicalProfile: MedicalProfile) = 
        medicalProfileDao.updateMedicalProfile(medicalProfile)
    
    suspend fun deleteMedicalProfile(medicalProfile: MedicalProfile) = 
        medicalProfileDao.deleteMedicalProfile(medicalProfile)
    
    suspend fun deleteMedicalProfileByUserId(userId: Long) = 
        medicalProfileDao.deleteMedicalProfileByUserId(userId)
} 
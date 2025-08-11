package com.example.cc.data.dao

import androidx.room.*
import com.example.cc.data.model.MedicalProfile
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicalProfileDao {
    @Query("SELECT * FROM medical_profiles WHERE userId = :userId")
    suspend fun getMedicalProfileByUserId(userId: Long): MedicalProfile?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedicalProfile(medicalProfile: MedicalProfile): Long
    
    @Update
    suspend fun updateMedicalProfile(medicalProfile: MedicalProfile)
    
    @Delete
    suspend fun deleteMedicalProfile(medicalProfile: MedicalProfile)
    
    @Query("DELETE FROM medical_profiles WHERE userId = :userId")
    suspend fun deleteMedicalProfileByUserId(userId: Long)
} 
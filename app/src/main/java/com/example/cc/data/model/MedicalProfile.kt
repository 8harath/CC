package com.example.cc.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "medical_profiles",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class MedicalProfile(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long,
    val bloodType: String? = null,
    val allergies: String? = null,
    val medications: String? = null,
    val medicalConditions: String? = null,
    val emergencyContacts: String? = null, // JSON string of contacts
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) 
package com.example.cc.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
import com.example.cc.data.dao.IncidentDao
import com.example.cc.data.dao.MedicalProfileDao
import com.example.cc.data.dao.UserDao
import com.example.cc.data.model.Incident
import com.example.cc.data.model.MedicalProfile
import com.example.cc.data.model.User
import com.example.cc.data.util.Converters

@Database(
    entities = [User::class, MedicalProfile::class, Incident::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun userDao(): UserDao
    abstract fun medicalProfileDao(): MedicalProfileDao
    abstract fun incidentDao(): IncidentDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "car_crash_detection_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
} 
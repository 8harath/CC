package com.example.cc

import android.app.Application
import com.example.cc.data.database.AppDatabase

class CarCrashDetectionApp : Application() {
    
    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }
    
    override fun onCreate() {
        super.onCreate()
        instance = this
    }
    
    companion object {
        lateinit var instance: CarCrashDetectionApp
            private set
    }
} 
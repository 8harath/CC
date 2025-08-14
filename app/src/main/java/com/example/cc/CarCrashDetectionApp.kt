package com.example.cc

import android.app.Application
import com.example.cc.data.database.AppDatabase
import com.example.cc.util.CrashHandler
import com.example.cc.util.LogConfig

class CarCrashDetectionApp : Application() {
    
    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }
    
    override fun onCreate() {
        super.onCreate()
        
        try {
            LogConfig.i("App", "Application onCreate started")
            
            // Initialize crash handler
            CrashHandler.getInstance().init(this)
            
            // Set application instance
            instance = this
            
            // Log system information
            LogConfig.logSystemInfo()
            
            LogConfig.i("App", "Application onCreate completed successfully")
            
        } catch (e: Exception) {
            LogConfig.e("App", "Error in Application onCreate: ${e.message}", e)
            // Don't re-throw to prevent app from crashing during initialization
        }
    }
    
    companion object {
        lateinit var instance: CarCrashDetectionApp
            private set
    }
} 
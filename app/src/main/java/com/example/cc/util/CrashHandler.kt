package com.example.cc.util

import android.content.Context
import android.os.Process
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.*

class CrashHandler private constructor() : Thread.UncaughtExceptionHandler {
    
    companion object {
        private const val TAG = "CrashHandler"
        private var instance: CrashHandler? = null
        
        fun getInstance(): CrashHandler {
            if (instance == null) {
                instance = CrashHandler()
            }
            return instance!!
        }
    }
    
    private var context: Context? = null
    private val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
    
    fun init(context: Context) {
        this.context = context.applicationContext
        Thread.setDefaultUncaughtExceptionHandler(this)
        LogConfig.i(TAG, "Crash handler initialized")
    }
    
    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        try {
            LogConfig.e(TAG, "Uncaught exception detected", throwable)
            LogConfig.logCrashInfo(throwable, "UncaughtException")
            
            // Save crash report to file
            saveCrashReport(throwable)
            
            // Log system information
            LogConfig.logSystemInfo()
            
        } catch (e: Exception) {
            LogConfig.e(TAG, "Error handling crash: ${e.message}", e)
        } finally {
            // Call default handler if available
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }
    
    private fun saveCrashReport(throwable: Throwable) {
        try {
            val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(Date())
            val fileName = "crash_report_$timestamp.txt"
            
            val crashDir = File(context?.filesDir, "crashes")
            if (!crashDir.exists()) {
                crashDir.mkdirs()
            }
            
            val crashFile = File(crashDir, fileName)
            PrintWriter(FileWriter(crashFile)).use { writer ->
                writer.println("=== CRASH REPORT ===")
                writer.println("Timestamp: $timestamp")
                writer.println("App Version: ${context?.packageManager?.getPackageInfo(context?.packageName ?: "", 0)?.versionName ?: "Unknown"}")
                writer.println("Android Version: ${android.os.Build.VERSION.RELEASE}")
                writer.println("Device: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
                writer.println("SDK Level: ${android.os.Build.VERSION.SDK_INT}")
                writer.println()
                writer.println("=== STACK TRACE ===")
                throwable.printStackTrace(writer)
                writer.println()
                writer.println("=== CAUSE ===")
                var cause = throwable.cause
                var level = 1
                while (cause != null) {
                    writer.println("Cause Level $level:")
                    cause.printStackTrace(writer)
                    writer.println()
                    cause = cause.cause
                    level++
                }
            }
            
            LogConfig.i(TAG, "Crash report saved to: ${crashFile.absolutePath}")
            
        } catch (e: Exception) {
            LogConfig.e(TAG, "Failed to save crash report: ${e.message}", e)
        }
    }
    
    fun getCrashReports(): List<File> {
        val crashDir = File(context?.filesDir, "crashes")
        return if (crashDir.exists()) {
            crashDir.listFiles()?.filter { it.name.startsWith("crash_report_") }?.sortedByDescending { it.lastModified() } ?: emptyList()
        } else {
            emptyList()
        }
    }
    
    fun clearCrashReports() {
        try {
            val crashDir = File(context?.filesDir, "crashes")
            if (crashDir.exists()) {
                crashDir.listFiles()?.forEach { it.delete() }
                LogConfig.i(TAG, "Crash reports cleared")
            }
        } catch (e: Exception) {
            LogConfig.e(TAG, "Failed to clear crash reports: ${e.message}", e)
        }
    }
}

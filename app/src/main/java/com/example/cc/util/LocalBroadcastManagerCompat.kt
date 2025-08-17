package com.example.cc.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.localbroadcastmanager.content.LocalBroadcastManager as AndroidXLocalBroadcastManager

/**
 * Compatibility wrapper for LocalBroadcastManager to handle the transition from
 * android.support.v4.content.LocalBroadcastManager to androidx.localbroadcastmanager.content.LocalBroadcastManager
 * 
 * This class provides the same interface as the old support library LocalBroadcastManager
 * but redirects all calls to the AndroidX version.
 */
object LocalBroadcastManagerCompat {
    
    /**
     * Get an instance of LocalBroadcastManager for the given context
     */
    fun getInstance(context: Context): LocalBroadcastManagerCompat {
        return LocalBroadcastManagerCompat(context)
    }
    
    /**
     * Wrapper class that provides the same interface as the old support library LocalBroadcastManager
     */
    class LocalBroadcastManagerCompat(private val context: Context) {
        
        private val androidXManager: AndroidXLocalBroadcastManager = AndroidXLocalBroadcastManager.getInstance(context)
        
        /**
         * Register a receiver for local broadcasts
         */
        fun registerReceiver(receiver: BroadcastReceiver, filter: IntentFilter) {
            androidXManager.registerReceiver(receiver, filter)
        }
        
        /**
         * Unregister a receiver
         */
        fun unregisterReceiver(receiver: BroadcastReceiver) {
            androidXManager.unregisterReceiver(receiver)
        }
        
        /**
         * Send a local broadcast
         */
        fun sendBroadcast(intent: Intent): Boolean {
            return androidXManager.sendBroadcast(intent)
        }
        
        /**
         * Send a local broadcast synchronously
         */
        fun sendBroadcastSync(intent: Intent) {
            androidXManager.sendBroadcastSync(intent)
        }
    }
}

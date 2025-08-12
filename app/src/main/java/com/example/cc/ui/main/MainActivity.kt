package com.example.cc.ui.main

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.cc.R
import com.example.cc.data.model.UserRole
import com.example.cc.ui.base.BaseActivity
import com.example.cc.ui.publisher.PublisherActivity
import com.example.cc.ui.subscriber.SubscriberActivity
import com.example.cc.util.DatabaseTest
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import android.view.View
import android.util.Log
import android.widget.Toast

class MainActivity : BaseActivity<View>() {
    
    companion object {
        private const val TAG = "MainActivity"
    }
    
    private val viewModel: MainViewModel by viewModels()
    
    override fun getViewBinding(): View = layoutInflater.inflate(R.layout.activity_main, null)
    
    override fun setupViews() {
        try {
            Log.d(TAG, "Setting up views...")
            setupRoleSelection()
            setupContinueButton()
            loadCurrentUser()
            
            // Temporarily disable database test to prevent crashes
            // DatabaseTest.testDatabase(this)
            Log.d(TAG, "Views setup completed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up views: ${e.message}", e)
            showToast("Error initializing app: ${e.message}")
        }
    }
    
    override fun setupObservers() {
        try {
            Log.d(TAG, "Setting up observers...")
            
            lifecycleScope.launch {
                viewModel.selectedRole.collect { role ->
                    try {
                        val continueButton = findViewById<com.google.android.material.button.MaterialButton>(R.id.btnContinue)
                        continueButton?.isEnabled = role != null
                        Log.d(TAG, "Role selection updated: $role")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error updating continue button: ${e.message}", e)
                    }
                }
            }
            
            lifecycleScope.launch {
                viewModel.currentUser.collect { user ->
                    try {
                        user?.let {
                            Log.d(TAG, "Current user loaded: ${it.name} (${it.role})")
                            navigateToRoleSpecificActivity(it.role)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error handling current user: ${e.message}", e)
                        showToast("Error loading user: ${e.message}")
                    }
                }
            }
            
            lifecycleScope.launch {
                viewModel.isLoading.collect { isLoading ->
                    try {
                        val continueButton = findViewById<com.google.android.material.button.MaterialButton>(R.id.btnContinue)
                        val selectedRole = viewModel.selectedRole.value
                        continueButton?.isEnabled = !isLoading && selectedRole != null
                        Log.d(TAG, "Loading state updated: $isLoading")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error updating loading state: ${e.message}", e)
                    }
                }
            }
            
            lifecycleScope.launch {
                viewModel.errorMessage.collect { error ->
                    try {
                        if (error != null) {
                            Log.e(TAG, "Error message received: $error")
                            showToast(error)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error showing error message: ${e.message}", e)
                    }
                }
            }
            
            Log.d(TAG, "Observers setup completed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up observers: ${e.message}", e)
            showToast("Error setting up app: ${e.message}")
        }
    }
    
    private fun setupRoleSelection() {
        try {
            Log.d(TAG, "Setting up role selection...")
            
            findViewById<com.google.android.material.card.MaterialCardView>(R.id.cardPublisher)?.setOnClickListener {
                try {
                    Log.d(TAG, "Publisher role selected")
                    viewModel.selectRole(UserRole.PUBLISHER)
                    updateCardSelection()
                } catch (e: Exception) {
                    Log.e(TAG, "Error selecting publisher role: ${e.message}", e)
                    showToast("Error selecting role: ${e.message}")
                }
            }
            
            findViewById<com.google.android.material.card.MaterialCardView>(R.id.cardSubscriber)?.setOnClickListener {
                try {
                    Log.d(TAG, "Subscriber role selected")
                    viewModel.selectRole(UserRole.SUBSCRIBER)
                    updateCardSelection()
                } catch (e: Exception) {
                    Log.e(TAG, "Error selecting subscriber role: ${e.message}", e)
                    showToast("Error selecting role: ${e.message}")
                }
            }
            
            Log.d(TAG, "Role selection setup completed")
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up role selection: ${e.message}", e)
            showToast("Error setting up role selection: ${e.message}")
        }
    }
    
    private fun setupContinueButton() {
        try {
            Log.d(TAG, "Setting up continue button...")
            
            findViewById<com.google.android.material.button.MaterialButton>(R.id.btnContinue)?.setOnClickListener {
                try {
                    val selectedRole = viewModel.selectedRole.value
                    Log.d(TAG, "Continue button clicked, selected role: $selectedRole")
                    
                    if (selectedRole != null) {
                        showNameInputDialog(selectedRole)
                    } else {
                        Log.w(TAG, "No role selected when continue button clicked")
                        showToast("Please select a role first")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error handling continue button click: ${e.message}", e)
                    showToast("Error: ${e.message}")
                }
            }
            
            Log.d(TAG, "Continue button setup completed")
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up continue button: ${e.message}", e)
            showToast("Error setting up continue button: ${e.message}")
        }
    }
    
    private fun updateCardSelection() {
        try {
            val selectedRole = viewModel.selectedRole.value
            Log.d(TAG, "Updating card selection for role: $selectedRole")
            
            // Reset card styles
            findViewById<com.google.android.material.card.MaterialCardView>(R.id.cardPublisher)?.strokeWidth = 0
            findViewById<com.google.android.material.card.MaterialCardView>(R.id.cardSubscriber)?.strokeWidth = 0
            
            // Apply selection style
            when (selectedRole) {
                UserRole.PUBLISHER -> {
                    findViewById<com.google.android.material.card.MaterialCardView>(R.id.cardPublisher)?.apply {
                        strokeWidth = 4
                        strokeColor = getColor(R.color.publisher_primary)
                    }
                }
                UserRole.SUBSCRIBER -> {
                    findViewById<com.google.android.material.card.MaterialCardView>(R.id.cardSubscriber)?.apply {
                        strokeWidth = 4
                        strokeColor = getColor(R.color.subscriber_primary)
                    }
                }
                null -> { 
                    Log.d(TAG, "No role selected")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating card selection: ${e.message}", e)
        }
    }
    
    private fun showNameInputDialog(role: UserRole) {
        try {
            Log.d(TAG, "Showing name input dialog for role: $role")
            
            val dialogView = layoutInflater.inflate(R.layout.dialog_name_input, null)
            val nameEditText = dialogView.findViewById<android.widget.EditText>(R.id.etName)
            
            MaterialAlertDialogBuilder(this)
                .setTitle("Enter Your Name")
                .setView(dialogView)
                .setPositiveButton("Continue") { _, _ ->
                    try {
                        val name = nameEditText.text?.toString()?.trim() ?: ""
                        Log.d(TAG, "Name entered: '$name'")
                        
                        if (name.isNotEmpty()) {
                            viewModel.createUser(name, role)
                        } else {
                            Log.w(TAG, "Empty name entered")
                            showToast("Please enter your name")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error handling name input: ${e.message}", e)
                        showToast("Error processing name: ${e.message}")
                    }
                }
                .setNegativeButton("Cancel") { _, _ ->
                    Log.d(TAG, "Name input dialog cancelled")
                }
                .setOnDismissListener {
                    Log.d(TAG, "Name input dialog dismissed")
                }
                .show()
                
        } catch (e: Exception) {
            Log.e(TAG, "Error showing name input dialog: ${e.message}", e)
            showToast("Error showing dialog: ${e.message}")
        }
    }
    
    private fun navigateToRoleSpecificActivity(role: UserRole) {
        try {
            Log.d(TAG, "Navigating to role-specific activity for: $role")
            
            val intent = when (role) {
                UserRole.PUBLISHER -> Intent(this, PublisherActivity::class.java)
                UserRole.SUBSCRIBER -> Intent(this, SubscriberActivity::class.java)
            }
            
            Log.d(TAG, "Starting activity: ${intent.component?.className}")
            startActivity(intent)
            finish()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error navigating to role-specific activity: ${e.message}", e)
            showToast("Error navigating to activity: ${e.message}")
        }
    }
    
    private fun loadCurrentUser() {
        try {
            Log.d(TAG, "Loading current user...")
            viewModel.loadCurrentUser()
        } catch (e: Exception) {
            Log.e(TAG, "Error loading current user: ${e.message}", e)
            showToast("Error loading user: ${e.message}")
        }
    }
} 
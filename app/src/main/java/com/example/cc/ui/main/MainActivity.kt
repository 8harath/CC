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

class MainActivity : BaseActivity<View>() {
    
    private val viewModel: MainViewModel by viewModels()
    
    override fun getViewBinding(): View = layoutInflater.inflate(R.layout.activity_main, null)
    
    override fun setupViews() {
        setupRoleSelection()
        setupContinueButton()
        loadCurrentUser()
        
        // Temporarily disable database test to prevent crashes
        // DatabaseTest.testDatabase(this)
    }
    
    override fun setupObservers() {
        lifecycleScope.launch {
            viewModel.selectedRole.collect { role ->
                findViewById<com.google.android.material.button.MaterialButton>(R.id.btnContinue).isEnabled = role != null
            }
        }
        
        lifecycleScope.launch {
            viewModel.currentUser.collect { user ->
                user?.let {
                    navigateToRoleSpecificActivity(it.role)
                }
            }
        }
        
        lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                findViewById<com.google.android.material.button.MaterialButton>(R.id.btnContinue).isEnabled = !isLoading && viewModel.selectedRole.value != null
            }
        }
        
        lifecycleScope.launch {
            viewModel.errorMessage.collect { error ->
                showToast(error)
            }
        }
    }
    
    private fun setupRoleSelection() {
        findViewById<com.google.android.material.card.MaterialCardView>(R.id.cardPublisher).setOnClickListener {
            viewModel.selectRole(UserRole.PUBLISHER)
            updateCardSelection()
        }
        
        findViewById<com.google.android.material.card.MaterialCardView>(R.id.cardSubscriber).setOnClickListener {
            viewModel.selectRole(UserRole.SUBSCRIBER)
            updateCardSelection()
        }
    }
    
    private fun setupContinueButton() {
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnContinue).setOnClickListener {
            val selectedRole = viewModel.selectedRole.value
            if (selectedRole != null) {
                showNameInputDialog(selectedRole)
            }
        }
    }
    
    private fun updateCardSelection() {
        val selectedRole = viewModel.selectedRole.value
        
        // Reset card styles
        findViewById<com.google.android.material.card.MaterialCardView>(R.id.cardPublisher).strokeWidth = 0
        findViewById<com.google.android.material.card.MaterialCardView>(R.id.cardSubscriber).strokeWidth = 0
        
        // Apply selection style
        when (selectedRole) {
            UserRole.PUBLISHER -> {
                findViewById<com.google.android.material.card.MaterialCardView>(R.id.cardPublisher).strokeWidth = 4
                findViewById<com.google.android.material.card.MaterialCardView>(R.id.cardPublisher).strokeColor = getColor(R.color.publisher_primary)
            }
            UserRole.SUBSCRIBER -> {
                findViewById<com.google.android.material.card.MaterialCardView>(R.id.cardSubscriber).strokeWidth = 4
                findViewById<com.google.android.material.card.MaterialCardView>(R.id.cardSubscriber).strokeColor = getColor(R.color.subscriber_primary)
            }
            null -> { /* No selection */ }
        }
    }
    
    private fun showNameInputDialog(role: UserRole) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_name_input, null)
        val nameEditText = dialogView.findViewById<android.widget.EditText>(R.id.etName)
        
        MaterialAlertDialogBuilder(this)
            .setTitle("Enter Your Name")
            .setView(dialogView)
            .setPositiveButton("Continue") { _, _ ->
                val name = nameEditText.text.toString().trim()
                if (name.isNotEmpty()) {
                    viewModel.createUser(name, role)
                } else {
                    showToast("Please enter your name")
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun navigateToRoleSpecificActivity(role: UserRole) {
        val intent = when (role) {
            UserRole.PUBLISHER -> Intent(this, PublisherActivity::class.java)
            UserRole.SUBSCRIBER -> Intent(this, SubscriberActivity::class.java)
        }
        startActivity(intent)
        finish()
    }
    
    private fun loadCurrentUser() {
        viewModel.loadCurrentUser()
    }
} 
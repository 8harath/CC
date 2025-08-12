package com.example.cc.ui.main

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.cc.R
import com.example.cc.data.model.UserRole
import com.example.cc.databinding.ActivityMainBinding
import com.example.cc.ui.base.BaseActivity
import com.example.cc.ui.publisher.PublisherActivity
import com.example.cc.ui.subscriber.SubscriberActivity
import com.example.cc.util.DatabaseTest
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class MainActivity : BaseActivity<ActivityMainBinding>() {
    
    private val viewModel: MainViewModel by viewModels()
    
    override fun getViewBinding(): ActivityMainBinding = ActivityMainBinding.inflate(layoutInflater)
    
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
                binding.btnContinue.isEnabled = role != null
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
                binding.btnContinue.isEnabled = !isLoading && viewModel.selectedRole.value != null
            }
        }
        
        lifecycleScope.launch {
            viewModel.errorMessage.collect { error ->
                showToast(error)
            }
        }
    }
    
    private fun setupRoleSelection() {
        binding.cardPublisher.setOnClickListener {
            viewModel.selectRole(UserRole.PUBLISHER)
            updateCardSelection()
        }
        
        binding.cardSubscriber.setOnClickListener {
            viewModel.selectRole(UserRole.SUBSCRIBER)
            updateCardSelection()
        }
    }
    
    private fun setupContinueButton() {
        binding.btnContinue.setOnClickListener {
            val selectedRole = viewModel.selectedRole.value
            if (selectedRole != null) {
                showNameInputDialog(selectedRole)
            }
        }
    }
    
    private fun updateCardSelection() {
        val selectedRole = viewModel.selectedRole.value
        
        // Reset card styles
        binding.cardPublisher.strokeWidth = 0
        binding.cardSubscriber.strokeWidth = 0
        
        // Apply selection style
        when (selectedRole) {
            UserRole.PUBLISHER -> {
                binding.cardPublisher.strokeWidth = 4
                binding.cardPublisher.strokeColor = getColor(R.color.publisher_primary)
            }
            UserRole.SUBSCRIBER -> {
                binding.cardSubscriber.strokeWidth = 4
                binding.cardSubscriber.strokeColor = getColor(R.color.subscriber_primary)
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
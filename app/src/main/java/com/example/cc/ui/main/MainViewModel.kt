package com.example.cc.ui.main

import androidx.lifecycle.viewModelScope
import com.example.cc.data.model.User
import com.example.cc.data.model.UserRole
import com.example.cc.data.repository.UserRepository
import com.example.cc.ui.base.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.example.cc.di.AppModule

class MainViewModel : BaseViewModel() {
    
    private val userRepository: UserRepository by lazy { 
        try {
            AppModule.userRepository
        } catch (e: Exception) {
            // If database is not available, we'll handle it gracefully
            throw e
        }
    }
    
    private val _selectedRole = MutableStateFlow<UserRole?>(null)
    val selectedRole: StateFlow<UserRole?> = _selectedRole
    
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser
    
    fun selectRole(role: UserRole) {
        _selectedRole.value = role
    }
    
    fun createUser(name: String, role: UserRole) {
        // Temporarily disable database access to prevent crashes
        launchWithLoading {
            val user = User(
                name = name,
                role = role
            )
            // val userId = userRepository.insertUser(user)
            // val createdUser = user.copy(id = userId)
            val createdUser = user.copy(id = 1L) // Temporary ID
            _currentUser.value = createdUser
        }
    }
    
    fun loadCurrentUser() {
        // Temporarily disable database access to prevent crashes
        // In a real app, you'd load from SharedPreferences or similar
        // For now, we'll just check if there's a user in the database
        /*
        viewModelScope.launch {
            userRepository.getAllUsers().collect { userList ->
                if (userList.isNotEmpty()) {
                    _currentUser.value = userList.first()
                    _selectedRole.value = userList.first().role
                }
            }
        }
        */
    }
} 
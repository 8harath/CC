package com.example.cc.ui.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

abstract class BaseViewModel : ViewModel() {
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    
    private val _errorMessage = MutableSharedFlow<String>()
    val errorMessage: SharedFlow<String> = _errorMessage
    
    private val _successMessage = MutableSharedFlow<String>()
    val successMessage: SharedFlow<String> = _successMessage
    
    protected fun showLoading() {
        _isLoading.value = true
    }
    
    protected fun hideLoading() {
        _isLoading.value = false
    }
    
    protected fun showError(message: String) {
        viewModelScope.launch {
            _errorMessage.emit(message)
        }
    }
    
    protected fun showSuccess(message: String) {
        viewModelScope.launch {
            _successMessage.emit(message)
        }
    }
    
    protected fun launchWithLoading(block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                showLoading()
                block()
            } catch (e: Exception) {
                showError(e.message ?: "An error occurred")
            } finally {
                hideLoading()
            }
        }
    }
} 
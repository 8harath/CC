package com.example.cc.ui.base

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import android.view.View

abstract class BaseActivity<VB : View> : AppCompatActivity() {
    
    protected lateinit var binding: VB
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = getViewBinding()
        setContentView(binding)
        
        setupViews()
        setupObservers()
    }
    
    abstract fun getViewBinding(): VB
    
    abstract fun setupViews()
    
    abstract fun setupObservers()
    
    protected fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
    
    protected fun showLongToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
} 
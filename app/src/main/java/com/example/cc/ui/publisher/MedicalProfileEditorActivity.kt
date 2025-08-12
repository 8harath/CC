package com.example.cc.ui.publisher

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.cc.R
import com.example.cc.data.model.EmergencyContact
import com.example.cc.data.model.MedicalProfile
import com.example.cc.databinding.ActivityMedicalProfileEditorBinding
import com.example.cc.util.PermissionManager
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class MedicalProfileEditorActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMedicalProfileEditorBinding
    private val viewModel: MedicalProfileEditorViewModel by viewModels()
    private lateinit var emergencyContactsAdapter: EmergencyContactsAdapter
    private var currentPhotoPath: String? = null
    
    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            currentPhotoPath?.let { path ->
                loadProfilePhoto(path)
            }
        }
    }
    
    private val getContentLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { loadProfilePhotoFromUri(it) }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMedicalProfileEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupToolbar()
        setupEmergencyContactsRecyclerView()
        setupButtons()
        setupObservers()
        
        // Load existing profile if editing
        intent.getLongExtra("profile_id", -1).let { profileId ->
            if (profileId != -1L) {
                viewModel.loadProfile(profileId)
            }
        }
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "Medical Profile"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }
    
    private fun setupEmergencyContactsRecyclerView() {
        emergencyContactsAdapter = EmergencyContactsAdapter(
            onContactClick = { contact -> showEditContactDialog(contact) },
            onContactDelete = { contact -> viewModel.removeEmergencyContact(contact) }
        )
        
        binding.rvEmergencyContacts.apply {
            layoutManager = LinearLayoutManager(this@MedicalProfileEditorActivity)
            adapter = emergencyContactsAdapter
        }
    }
    
    private fun setupButtons() {
        binding.btnTakePhoto.setOnClickListener {
            if (PermissionManager.hasCameraPermissions(this)) {
                showPhotoOptionsDialog()
            } else {
                PermissionManager.requestCameraPermissions(this)
            }
        }
        
        binding.btnAddContact.setOnClickListener {
            showAddContactDialog()
        }
        
        binding.btnSaveProfile.setOnClickListener {
            saveProfile()
        }
    }
    
    private fun setupObservers() {
        lifecycleScope.launch {
            viewModel.profile.collect { profile ->
                profile?.let { populateFields(it) }
            }
        }
        
        lifecycleScope.launch {
            viewModel.emergencyContacts.collect { contacts ->
                emergencyContactsAdapter.updateContacts(contacts)
            }
        }
        
        lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                binding.btnSaveProfile.isEnabled = !isLoading
            }
        }
        
        lifecycleScope.launch {
            viewModel.errorMessage.collect { error ->
                error?.let { showError(it) }
            }
        }
        
        lifecycleScope.launch {
            viewModel.successMessage.collect { message ->
                message?.let { 
                    showSuccess(it)
                    finish()
                }
            }
        }
    }
    
    private fun populateFields(profile: MedicalProfile) {
        binding.etFullName.setText(profile.fullName)
        binding.etDateOfBirth.setText(profile.dateOfBirth)
        binding.etHeight.setText(profile.height)
        binding.etWeight.setText(profile.weight)
        binding.etBloodType.setText(profile.bloodType)
        binding.etAllergies.setText(profile.allergies)
        binding.etMedications.setText(profile.medications)
        binding.etMedicalConditions.setText(profile.medicalConditions)
        binding.etInsuranceInfo.setText(profile.insuranceInfo)
        binding.cbOrganDonor.isChecked = profile.organDonor
        
        // Load profile photo
        profile.photoPath?.let { path ->
            loadProfilePhoto(path)
            currentPhotoPath = path
        }
    }
    
    private fun showPhotoOptionsDialog() {
        AlertDialog.Builder(this)
            .setTitle("Profile Photo")
            .setItems(arrayOf("Take Photo", "Choose from Gallery")) { _, which ->
                when (which) {
                    0 -> takePhoto()
                    1 -> chooseFromGallery()
                }
            }
            .show()
    }
    
    private fun takePhoto() {
        val photoFile = createImageFile()
        currentPhotoPath = photoFile.absolutePath
        
        val photoUri = FileProvider.getUriForFile(
            this,
            "${packageName}.fileprovider",
            photoFile
        )
        
        takePictureLauncher.launch(photoUri)
    }
    
    private fun chooseFromGallery() {
        getContentLauncher.launch("image/*")
    }
    
    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "PROFILE_${timeStamp}_"
        val storageDir = getExternalFilesDir(null)
        return File.createTempFile(imageFileName, ".jpg", storageDir)
    }
    
    private fun loadProfilePhoto(path: String) {
        try {
            val bitmap = BitmapFactory.decodeFile(path)
            binding.ivProfilePhoto.setImageBitmap(bitmap)
        } catch (e: Exception) {
            showError("Failed to load photo: ${e.message}")
        }
    }
    
    private fun loadProfilePhotoFromUri(uri: Uri) {
        try {
            val inputStream = contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            binding.ivProfilePhoto.setImageBitmap(bitmap)
            
            // Save the image to a file
            val photoFile = createImageFile()
            currentPhotoPath = photoFile.absolutePath
            
            FileOutputStream(photoFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
        } catch (e: Exception) {
            showError("Failed to load photo: ${e.message}")
        }
    }
    
    private fun showAddContactDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_emergency_contact, null)
        val etName = dialogView.findViewById<android.widget.EditText>(R.id.etName)
        val etRelationship = dialogView.findViewById<android.widget.EditText>(R.id.etRelationship)
        val etPhone = dialogView.findViewById<android.widget.EditText>(R.id.etPhone)
        val etEmail = dialogView.findViewById<android.widget.EditText>(R.id.etEmail)
        val cbPrimary = dialogView.findViewById<android.widget.CheckBox>(R.id.cbPrimary)
        
        AlertDialog.Builder(this)
            .setTitle("Add Emergency Contact")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val contact = EmergencyContact(
                    name = etName.text.toString(),
                    relationship = etRelationship.text.toString(),
                    phoneNumber = etPhone.text.toString(),
                    email = etEmail.text.toString().takeIf { it.isNotEmpty() },
                    isPrimary = cbPrimary.isChecked
                )
                viewModel.addEmergencyContact(contact)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun showEditContactDialog(contact: EmergencyContact) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_emergency_contact, null)
        val etName = dialogView.findViewById<android.widget.EditText>(R.id.etName)
        val etRelationship = dialogView.findViewById<android.widget.EditText>(R.id.etRelationship)
        val etPhone = dialogView.findViewById<android.widget.EditText>(R.id.etPhone)
        val etEmail = dialogView.findViewById<android.widget.EditText>(R.id.etEmail)
        val cbPrimary = dialogView.findViewById<android.widget.CheckBox>(R.id.cbPrimary)
        
        // Populate fields
        etName.setText(contact.name)
        etRelationship.setText(contact.relationship)
        etPhone.setText(contact.phoneNumber)
        etEmail.setText(contact.email)
        cbPrimary.isChecked = contact.isPrimary
        
        AlertDialog.Builder(this)
            .setTitle("Edit Emergency Contact")
            .setView(dialogView)
            .setPositiveButton("Update") { _, _ ->
                val updatedContact = contact.copy(
                    name = etName.text.toString(),
                    relationship = etRelationship.text.toString(),
                    phoneNumber = etPhone.text.toString(),
                    email = etEmail.text.toString().takeIf { it.isNotEmpty() },
                    isPrimary = cbPrimary.isChecked
                )
                viewModel.updateEmergencyContact(updatedContact)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun saveProfile() {
        val profile = MedicalProfile(
            id = intent.getLongExtra("profile_id", 0),
            userId = 1L, // TODO: Get from user session
            fullName = binding.etFullName.text.toString(),
            dateOfBirth = binding.etDateOfBirth.text.toString(),
            height = binding.etHeight.text.toString(),
            weight = binding.etWeight.text.toString(),
            bloodType = binding.etBloodType.text.toString(),
            allergies = binding.etAllergies.text.toString(),
            medications = binding.etMedications.text.toString(),
            medicalConditions = binding.etMedicalConditions.text.toString(),
            insuranceInfo = binding.etInsuranceInfo.text.toString(),
            organDonor = binding.cbOrganDonor.isChecked,
            photoPath = currentPhotoPath
        )
        
        viewModel.saveProfile(profile)
    }
    
    private fun showError(message: String) {
        AlertDialog.Builder(this)
            .setTitle("Error")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }
    
    private fun showSuccess(message: String) {
        AlertDialog.Builder(this)
            .setTitle("Success")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }
    
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}

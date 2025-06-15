package com.example.last.fragments

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.UUID
import kotlin.coroutines.resumeWithException

data class RecipientData(
    val fullName: String = "",
    val dob: String = "",
    val contactNumber: String = "",
    val email: String = "",
    val address: String = "",
    val bloodGroup: String = "",
    val organNeeded: String = "",
    val urgencyLevel: String = "",
    val medicalHistory: String = "",
    val diagnosisDetails: String = "",
    val medications: String = "",
    val allergies: String = "",
    val doctorName: String = "",
    val hospitalName: String = "",
    val insuranceProvider: String = "",
    val insuranceId: String = "",
    val emergencyContact: String = "",
    val currentLocation: String = "",
    val preferredHospitals: String = "",
    val profileImageUrl: String = "",
    val gender: String = "",
    val latitude: String = "",
    val longitude: String = ""
)

class RecipientHomeViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance().reference.child("recipient")
    private lateinit var storage: FirebaseStorage
    private lateinit var storageReference: StorageReference

    private val _recipientData = MutableStateFlow<RecipientData?>(null)
    val recipientData: StateFlow<RecipientData?> = _recipientData.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    private val _isEditing = MutableStateFlow(false)
    val isEditing: StateFlow<Boolean> = _isEditing.asStateFlow()

    private val _profileImageUri = MutableStateFlow<Uri?>(null)
    val profileImageUri: StateFlow<Uri?> = _profileImageUri.asStateFlow()

    private val _uploadProgress = MutableStateFlow<Double>(0.0)
    val uploadProgress: StateFlow<Double> = _uploadProgress.asStateFlow()

    // Maintain current recipient data for editing
    private var currentRecipientData = RecipientData()

    fun initializeStorage(storageApp: FirebaseApp) {
        storage = FirebaseStorage.getInstance(storageApp)
        storageReference = storage.reference
    }

    fun fetchRecipientData() {
        val user = auth.currentUser ?: return
        val emailKey = user.email?.replace(".", "_") ?: return

        viewModelScope.launch {
            try {
                val data = withContext(Dispatchers.IO) {
                    fetchDataFromFirebase(emailKey)
                }
                currentRecipientData = data
                _recipientData.value = data
                // Fetch profile image after getting recipient data
                fetchProfileImage()
            } catch (e: Exception) {
                _toastMessage.value = "Failed to load data: ${e.message}"
            }
        }
    }

    private suspend fun fetchDataFromFirebase(emailKey: String): RecipientData {
        return suspendCancellableCoroutine { continuation ->
            database.child(emailKey).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val data = RecipientData(
                            fullName = snapshot.child("fullName").value?.toString() ?: "",
                            dob = snapshot.child("dob").value?.toString() ?: "",
                            contactNumber = snapshot.child("contactNumber").value?.toString() ?: "",
                            email = snapshot.child("email").value?.toString() ?: "",
                            address = snapshot.child("address").value?.toString() ?: "",
                            bloodGroup = snapshot.child("bloodGroup").value?.toString() ?: "",
                            organNeeded = snapshot.child("organNeeded").value?.toString() ?: "",
                            urgencyLevel = snapshot.child("urgencyLevel").value?.toString() ?: "",
                            medicalHistory = snapshot.child("medicalHistory").value?.toString() ?: "",
                            diagnosisDetails = snapshot.child("diagnosisDetails").value?.toString() ?: "",
                            medications = snapshot.child("medications").value?.toString() ?: "",
                            allergies = snapshot.child("allergies").value?.toString() ?: "",
                            doctorName = snapshot.child("doctorName").value?.toString() ?: "",
                            hospitalName = snapshot.child("hospitalName").value?.toString() ?: "",
                            insuranceProvider = snapshot.child("insuranceProvider").value?.toString() ?: "",
                            insuranceId = snapshot.child("insuranceId").value?.toString() ?: "",
                            emergencyContact = snapshot.child("emergencyContact").value?.toString() ?: "",
                            currentLocation = snapshot.child("currentLocation").value?.toString() ?: "",
                            preferredHospitals = snapshot.child("preferredHospitals").value?.toString() ?: "",
                            profileImageUrl = snapshot.child("profileImageUrl").value?.toString() ?: "",
                            gender = snapshot.child("gender").value?.toString() ?: "",
                            latitude = snapshot.child("latitude").value?.toString() ?: "",
                            longitude = snapshot.child("longitude").value?.toString() ?: ""
                        )
                        continuation.resume(data, null)
                    } else {
                        continuation.resume(RecipientData(), null)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    continuation.resumeWithException(Exception(error.message))
                }
            })
        }
    }

    fun fetchProfileImage() {
        val user = auth.currentUser ?: return
        val emailKey = user.email?.replace(".", "_") ?: return

        viewModelScope.launch {
            try {
                val imageUrl = _recipientData.value?.profileImageUrl
                if (!imageUrl.isNullOrEmpty()) {
                    val imageRef = storage.getReferenceFromUrl(imageUrl)
                    imageRef.downloadUrl.addOnSuccessListener { uri ->
                        _profileImageUri.value = uri
                    }.addOnFailureListener { e ->
                        _toastMessage.value = "Failed to load profile image: ${e.message}"
                    }
                }
            } catch (e: Exception) {
                _toastMessage.value = "Error fetching profile image: ${e.message}"
            }
        }
    }

    fun fetchImageFromStorage(path: String, onSuccess: (Uri) -> Unit, onFailure: (Exception) -> Unit) {
        viewModelScope.launch {
            try {
                val imageRef = storageReference.child(path)
                imageRef.downloadUrl.addOnSuccessListener { uri ->
                    onSuccess(uri)
                }.addOnFailureListener { e ->
                    onFailure(e)
                }
            } catch (e: Exception) {
                onFailure(e)
            }
        }
    }

    fun updateField(field: String, value: String) {
        val currentData = _recipientData.value ?: RecipientData()
        val updatedData = when (field) {
            "fullName" -> currentData.copy(fullName = value)
            "dob" -> currentData.copy(dob = value)
            "contactNumber" -> currentData.copy(contactNumber = value)
            "email" -> currentData.copy(email = value)
            "address" -> currentData.copy(address = value)
            "bloodGroup" -> currentData.copy(bloodGroup = value)
            "organNeeded" -> currentData.copy(organNeeded = value)
            "urgencyLevel" -> currentData.copy(urgencyLevel = value)
            "medicalHistory" -> currentData.copy(medicalHistory = value)
            "diagnosisDetails" -> currentData.copy(diagnosisDetails = value)
            "medications" -> currentData.copy(medications = value)
            "allergies" -> currentData.copy(allergies = value)
            "doctorName" -> currentData.copy(doctorName = value)
            "hospitalName" -> currentData.copy(hospitalName = value)
            "insuranceProvider" -> currentData.copy(insuranceProvider = value)
            "insuranceId" -> currentData.copy(insuranceId = value)
            "emergencyContact" -> currentData.copy(emergencyContact = value)
            "currentLocation" -> currentData.copy(currentLocation = value)
            "preferredHospitals" -> currentData.copy(preferredHospitals = value)
            "gender" -> currentData.copy(gender = value)
            "latitude" -> currentData.copy(latitude = value)
            "longitude" -> currentData.copy(longitude = value)
            else -> currentData
        }
        _recipientData.value = updatedData
        currentRecipientData = updatedData
    }

    fun saveRecipientData() {
        val user = auth.currentUser ?: return
        val emailKey = user.email?.replace(".", "_") ?: return

        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    database.child(emailKey).setValue(currentRecipientData).addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            _toastMessage.value = "Data saved successfully"
                            _isEditing.value = false
                        } else {
                            _toastMessage.value = "Failed to save data"
                        }
                    }
                }
            } catch (e: Exception) {
                _toastMessage.value = "Failed to save data: ${e.message}"
            }
        }
    }

    fun uploadImage(uri: Uri, type: String) {
        val user = auth.currentUser ?: run {
            _toastMessage.value = "No user logged in"
            return
        }

        val emailKey = user.email?.replace(".", "_") ?: run {
            _toastMessage.value = "Invalid email"
            return
        }

        viewModelScope.launch {
            try {
                _toastMessage.value = "Starting upload..."
                _uploadProgress.value = 0.0

                val imageRef = storageReference.child("$emailKey/$type/${UUID.randomUUID()}")

                withContext(Dispatchers.IO) {
                    imageRef.putFile(uri)
                        .addOnProgressListener { taskSnapshot ->
                            val progress = (100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount)
                            _uploadProgress.value = progress
                            _toastMessage.value = "Upload is $progress% done"
                        }
                        .addOnSuccessListener {
                            _toastMessage.value = "Upload completed, getting download URL..."
                            _uploadProgress.value = 100.0

                            imageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                                if (type == "profile") {
                                    val updatedData = _recipientData.value?.copy(
                                        profileImageUrl = downloadUri.toString()
                                    ) ?: RecipientData(profileImageUrl = downloadUri.toString())
                                    _recipientData.value = updatedData
                                    currentRecipientData = updatedData
                                    _profileImageUri.value = downloadUri
                                    saveRecipientData()
                                }
                                _toastMessage.value = "Image uploaded successfully"
                            }.addOnFailureListener { e ->
                                _toastMessage.value = "Failed to get download URL: ${e.message}"
                            }
                        }
                        .addOnFailureListener { e ->
                            _toastMessage.value = "Upload failed: ${e.message}"
                            _uploadProgress.value = 0.0
                        }
                }
            } catch (e: Exception) {
                _toastMessage.value = "Upload error: ${e.message}"
                _uploadProgress.value = 0.0
            }
        }
    }

    fun toggleEditMode() {
        _isEditing.value = !_isEditing.value
    }

    fun showToast(message: String) {
        _toastMessage.value = message
    }

    fun clearToastMessage() {
        _toastMessage.value = null
    }

    fun clearUploadProgress() {
        _uploadProgress.value = 0.0
    }
}
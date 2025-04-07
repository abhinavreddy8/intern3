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

data class HospitalData(
    val hospitalName: String = "",
    val hospitalId: String = "",
    val address: String = "",
    val phoneNumber: String = "",
    val email: String = "",
    val website: String = "",
    val emergencyContact: String = "",
    val logoUrl: String = "",
    val specializations: String = "",
    val organTransplantTypes: String = "",
    val transplantTeam: String = "",
    val bedsAvailable: String = "",
    val icuFacilities: String = "",
    val operatingRooms: String = "",
    val certifications: String = "",
    val accreditations: String = ""
)

class HospitalHomeViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance().reference.child("hospital")
    private lateinit var storage: FirebaseStorage
    private lateinit var storageReference: StorageReference

    private val _hospitalData = MutableStateFlow<HospitalData?>(null)
    val hospitalData: StateFlow<HospitalData?> = _hospitalData.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    private val _isEditing = MutableStateFlow(false)
    val isEditing: StateFlow<Boolean> = _isEditing.asStateFlow()

    private val _logoImageUri = MutableStateFlow<Uri?>(null)
    val logoImageUri: StateFlow<Uri?> = _logoImageUri.asStateFlow()

    private val _uploadProgress = MutableStateFlow<Double>(0.0)
    val uploadProgress: StateFlow<Double> = _uploadProgress.asStateFlow()

    // Maintain current hospital data for editing
    private var currentHospitalData = HospitalData()

    fun initializeStorage(storageApp: FirebaseApp) {
        storage = FirebaseStorage.getInstance(storageApp)
        storageReference = storage.reference
    }

    fun fetchHospitalData() {
        val user = auth.currentUser ?: return
        val emailKey = user.email?.replace(".", "_") ?: return

        viewModelScope.launch {
            try {
                val data = withContext(Dispatchers.IO) {
                    fetchDataFromFirebase(emailKey)
                }
                currentHospitalData = data
                _hospitalData.value = data
                // Fetch logo image after getting hospital data
                fetchLogoImage()
            } catch (e: Exception) {
                _toastMessage.value = "Failed to load data: ${e.message}"
            }
        }
    }

    private suspend fun fetchDataFromFirebase(emailKey: String): HospitalData {
        return suspendCancellableCoroutine { continuation ->
            database.child(emailKey).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val data = HospitalData(
                            hospitalName = snapshot.child("hospitalName").value?.toString() ?: "",
                            hospitalId = snapshot.child("hospitalId").value?.toString() ?: "",
                            address = snapshot.child("address").value?.toString() ?: "",
                            phoneNumber = snapshot.child("phoneNumber").value?.toString() ?: "",
                            email = snapshot.child("email").value?.toString() ?: "",
                            website = snapshot.child("website").value?.toString() ?: "",
                            emergencyContact = snapshot.child("emergencyContact").value?.toString() ?: "",
                            logoUrl = snapshot.child("logoUrl").value?.toString() ?: "",
                            specializations = snapshot.child("specializations").value?.toString() ?: "",
                            organTransplantTypes = snapshot.child("organTransplantTypes").value?.toString() ?: "",
                            transplantTeam = snapshot.child("transplantTeam").value?.toString() ?: "",
                            bedsAvailable = snapshot.child("bedsAvailable").value?.toString() ?: "",
                            icuFacilities = snapshot.child("icuFacilities").value?.toString() ?: "",
                            operatingRooms = snapshot.child("operatingRooms").value?.toString() ?: "",
                            certifications = snapshot.child("certifications").value?.toString() ?: "",
                            accreditations = snapshot.child("accreditations").value?.toString() ?: ""
                        )
                        continuation.resume(data, null)
                    } else {
                        continuation.resume(HospitalData(), null)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    continuation.resumeWithException(Exception(error.message))
                }
            })
        }
    }

    fun fetchLogoImage() {
        val user = auth.currentUser ?: return
        val emailKey = user.email?.replace(".", "_") ?: return

        viewModelScope.launch {
            try {
                val logoUrl = _hospitalData.value?.logoUrl
                if (!logoUrl.isNullOrEmpty()) {
                    val imageRef = storage.getReferenceFromUrl(logoUrl)
                    imageRef.downloadUrl.addOnSuccessListener { uri ->
                        _logoImageUri.value = uri
                    }.addOnFailureListener { e ->
                        _toastMessage.value = "Failed to load logo image: ${e.message}"
                    }
                }
            } catch (e: Exception) {
                _toastMessage.value = "Error fetching logo image: ${e.message}"
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
        val currentData = _hospitalData.value ?: HospitalData()
        val updatedData = when (field) {
            "hospitalName" -> currentData.copy(hospitalName = value)
            "hospitalId" -> currentData.copy(hospitalId = value)
            "address" -> currentData.copy(address = value)
            "phoneNumber" -> currentData.copy(phoneNumber = value)
            "email" -> currentData.copy(email = value)
            "website" -> currentData.copy(website = value)
            "emergencyContact" -> currentData.copy(emergencyContact = value)
            "specializations" -> currentData.copy(specializations = value)
            "organTransplantTypes" -> currentData.copy(organTransplantTypes = value)
            "transplantTeam" -> currentData.copy(transplantTeam = value)
            "bedsAvailable" -> currentData.copy(bedsAvailable = value)
            "icuFacilities" -> currentData.copy(icuFacilities = value)
            "operatingRooms" -> currentData.copy(operatingRooms = value)
            "certifications" -> currentData.copy(certifications = value)
            "accreditations" -> currentData.copy(accreditations = value)
            else -> currentData
        }
        _hospitalData.value = updatedData
        currentHospitalData = updatedData
    }

    fun saveHospitalData() {
        val user = auth.currentUser ?: return
        val emailKey = user.email?.replace(".", "_") ?: return

        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    database.child(emailKey).setValue(currentHospitalData).addOnCompleteListener { task ->
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
                                if (type == "logo") {
                                    val updatedData = _hospitalData.value?.copy(
                                        logoUrl = downloadUri.toString()
                                    ) ?: HospitalData(logoUrl = downloadUri.toString())
                                    _hospitalData.value = updatedData
                                    currentHospitalData = updatedData
                                    _logoImageUri.value = downloadUri
                                    saveHospitalData()
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

    fun clearToastMessage() {
        _toastMessage.value = null
    }

    fun clearUploadProgress() {
        _uploadProgress.value = 0.0
    }
}
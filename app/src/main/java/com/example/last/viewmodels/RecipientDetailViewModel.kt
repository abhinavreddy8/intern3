package com.example.last.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.suspendCancellableCoroutine
import java.text.SimpleDateFormat
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class RecipientdetailViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance().reference.child("recipient")
    private val storage = FirebaseStorage.getInstance().reference

    private val _recipientDetails = MutableStateFlow<RecipientDetails?>(null)
    val recipientDetails: StateFlow<RecipientDetails?> = _recipientDetails

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    data class RecipientDetails(
        val id: String = "",
        val fullName: String = "",
        val dob: String? = null,
        val age: String? = null,
        val gender: String? = null,
        val contactNumber: String? = null,
        val email: String? = null,
        val address: String? = null,
        val bloodTypeNeeded: String? = null,
        val requiredOrgan: String? = null,
        val medicalCondition: String? = null,
        val medicalHistory: String? = null,
        val medications: String? = null,
        val allergies: String? = null,
        val urgencyLevel: String? = null,
        val hospitalName: String? = null,
        val doctorName: String? = null,
        val hospitalContact: String? = null,
        val currentLocation: String? = null,
        val profileImageUrl: String? = null,
        val medicalReportsUrl: String? = null
    )

    fun fetchRecipientDetails(recipientId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                val snapshot = getRecipientData(recipientId)

                if (snapshot.exists()) {
                    val dob = snapshot.child("dob").getValue(String::class.java)
                    val age = calculateAge(dob)

                    _recipientDetails.value = RecipientDetails(
                        id = recipientId,
                        fullName = snapshot.child("fullName").getValue(String::class.java) ?: "",
                        dob = dob,
                        age = age,
                        gender = snapshot.child("gender").getValue(String::class.java),
                        contactNumber = snapshot.child("contactNumber").getValue(String::class.java),
                        email = snapshot.child("email").getValue(String::class.java),
                        address = snapshot.child("address").getValue(String::class.java),
                        bloodTypeNeeded = snapshot.child("bloodTypeNeeded").getValue(String::class.java),
                        requiredOrgan = snapshot.child("requiredOrgan").getValue(String::class.java),
                        medicalCondition = snapshot.child("medicalCondition").getValue(String::class.java),
                        medicalHistory = snapshot.child("medicalHistory").getValue(String::class.java),
                        medications = snapshot.child("medications").getValue(String::class.java),
                        allergies = snapshot.child("allergies").getValue(String::class.java),
                        urgencyLevel = snapshot.child("urgencyLevel").getValue(String::class.java),
                        hospitalName = snapshot.child("hospitalName").getValue(String::class.java),
                        doctorName = snapshot.child("doctorName").getValue(String::class.java),
                        hospitalContact = snapshot.child("hospitalContact").getValue(String::class.java),
                        currentLocation = snapshot.child("currentLocation").getValue(String::class.java),
                        profileImageUrl = snapshot.child("profileImageUrl").getValue(String::class.java)
                    )

                    fetchMedicalReports(recipientId)
                } else {
                    _error.value = "Recipient not found"
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "An unknown error occurred"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun getRecipientData(recipientId: String): DataSnapshot =
        suspendCancellableCoroutine { continuation ->
            val recipientRef = database.child(recipientId)
            val valueEventListener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    continuation.resume(snapshot)
                }

                override fun onCancelled(error: DatabaseError) {
                    continuation.resumeWithException(Exception(error.message))
                }
            }

            recipientRef.addListenerForSingleValueEvent(valueEventListener)

            continuation.invokeOnCancellation {
                recipientRef.removeEventListener(valueEventListener)
            }
        }

    private fun calculateAge(dobString: String?): String? {
        if (dobString.isNullOrEmpty()) return null
        try {
            val dateFormat = SimpleDateFormat("MM/dd/yyyy", Locale.US)
            val birthDate = dateFormat.parse(dobString) ?: return null
            val today = Calendar.getInstance()
            val birthCalendar = Calendar.getInstance()
            birthCalendar.time = birthDate

            var age = today.get(Calendar.YEAR) - birthCalendar.get(Calendar.YEAR)
            if (today.get(Calendar.DAY_OF_YEAR) < birthCalendar.get(Calendar.DAY_OF_YEAR)) {
                age--
            }
            return age.toString()
        } catch (e: Exception) {
            return null
        }
    }

    private suspend fun fetchMedicalReports(recipientId: String) {
        try {
            val reportsRef = storage
                .child("recipients")
                .child(recipientId)
                .child("medical_reports")

            val reportsUrl = try {
                reportsRef.downloadUrl.await().toString()
            } catch (e: Exception) {
                null
            }

            _recipientDetails.value = _recipientDetails.value?.copy(
                medicalReportsUrl = reportsUrl
            )
        } catch (e: Exception) {
            // Silently fail
        }
    }

    fun isDoctorOrHospital(): Boolean {
        // Implement logic to check if current user is doctor/hospital
        // This could check user type in Firebase Auth or database
        return true // Placeholder - adjust based on your auth system
    }
}
// DonorDetailViewModel.kt
package com.example.last.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.suspendCancellableCoroutine
import java.text.SimpleDateFormat
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class DonorDetailViewModel : ViewModel() {
    private val TAG = "DonorDetailViewModel"
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance().reference
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
    private val storageReference: StorageReference = storage.reference

    private val _donorDetails = MutableStateFlow<DonorDetails?>(null)
    val donorDetails: StateFlow<DonorDetails?> = _donorDetails

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    // Add status message for user feedback (similar to DonorNotificationViewModel)
    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage

    // Add contact request status to track if a request is already sent
    private val _contactRequestStatus = MutableStateFlow<String?>(null)
    val contactRequestStatus: StateFlow<String?> = _contactRequestStatus

    data class DonorDetails(
        val id: String = "",
        val fullName: String = "",
        val dob: String? = null,
        val age: String? = null,
        val gender: String? = null,
        val contactNumber: String? = null,
        val email: String? = null,
        val address: String? = null,
        val bloodGroup: String = "",
        val organAvailable: String? = null,
        val medicalHistory: String? = null,
        val surgeries: String? = null,
        val medications: String? = null,
        val allergies: String? = null,
        val lifestyle: String? = null,
        val geneticDisorders: String? = null,
        val emergencyContact: String? = null,
        val currentLocation: String? = null,
        val preferredLocation: String? = null,
        val profileImageUrl: String? = null,
        val medicalReportUrl: String? = null,
        val surgeryReportUrl: String? = null
    )

    fun fetchDonorDetails(donorId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _statusMessage.value = null

            try {
                Log.d(TAG, "Fetching donor details for ID: $donorId")
                // Get donor data
                val snapshot = getDonorData(donorId)

                if (snapshot.exists()) {
                    val dob = snapshot.child("dob").getValue(String::class.java)
                    val age = calculateAge(dob)

                    _donorDetails.value = DonorDetails(
                        id = donorId,
                        fullName = snapshot.child("fullName").getValue(String::class.java) ?: "",
                        dob = dob,
                        age = age,
                        gender = snapshot.child("gender").getValue(String::class.java),
                        contactNumber = snapshot.child("contactNumber").getValue(String::class.java),
                        email = snapshot.child("email").getValue(String::class.java),
                        address = snapshot.child("address").getValue(String::class.java),
                        bloodGroup = snapshot.child("bloodGroup").getValue(String::class.java) ?: "",
                        organAvailable = snapshot.child("organAvailable").getValue(String::class.java),
                        medicalHistory = snapshot.child("medicalHistory").getValue(String::class.java),
                        surgeries = snapshot.child("surgeries").getValue(String::class.java),
                        medications = snapshot.child("medications").getValue(String::class.java),
                        allergies = snapshot.child("allergies").getValue(String::class.java),
                        lifestyle = snapshot.child("lifestyle").getValue(String::class.java),
                        geneticDisorders = snapshot.child("geneticDisorders").getValue(String::class.java),
                        emergencyContact = snapshot.child("emergencyContact").getValue(String::class.java),
                        currentLocation = snapshot.child("currentLocation").getValue(String::class.java),
                        preferredLocation = snapshot.child("preferredLocation").getValue(String::class.java),
                        profileImageUrl = snapshot.child("profileImageUrl").getValue(String::class.java)
                    )
                    Log.d(TAG, "Donor details fetched: ${_donorDetails.value}")

                    // Fetch additional image URLs if needed
                    fetchAdditionalImages(donorId)

                    // Check if there's already a contact request
                    checkExistingContactRequest(donorId)
                } else {
                    _error.value = "Donor not found"
                    Log.e(TAG, "Donor not found with ID: $donorId")
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "An unknown error occurred"
                Log.e(TAG, "Error fetching donor details: ${e.message}", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun getDonorData(donorId: String): DataSnapshot = suspendCancellableCoroutine { continuation ->
        val donorRef = database.child("donor").child(donorId)
        val valueEventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d(TAG, "Donor data retrieved successfully")
                continuation.resume(snapshot)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Database error: ${error.message}")
                continuation.resumeWithException(Exception(error.message))
            }
        }

        donorRef.addListenerForSingleValueEvent(valueEventListener)

        continuation.invokeOnCancellation {
            donorRef.removeEventListener(valueEventListener)
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
            Log.e(TAG, "Error calculating age: ${e.message}")
            return null
        }
    }

    private suspend fun fetchAdditionalImages(donorId: String) {
        try {
            Log.d(TAG, "Fetching additional images for donor: $donorId")
            // Fetch medical report URL
            val medicalReportRef = storageReference
                .child("donors")
                .child(donorId)
                .child("medical_reports")

            val medicalReportUrl = try {
                medicalReportRef.downloadUrl.await().toString()
            } catch (e: Exception) {
                Log.d(TAG, "No medical report available: ${e.message}")
                null
            }

            // Fetch surgery report URL
            val surgeryReportRef = storageReference
                .child("donors")
                .child(donorId)
                .child("surgery_reports")

            val surgeryReportUrl = try {
                surgeryReportRef.downloadUrl.await().toString()
            } catch (e: Exception) {
                Log.d(TAG, "No surgery report available: ${e.message}")
                null
            }

            // Update donor details with image URLs
            _donorDetails.value = _donorDetails.value?.copy(
                medicalReportUrl = medicalReportUrl,
                surgeryReportUrl = surgeryReportUrl
            )
        } catch (e: Exception) {
            // Silently fail for additional images
            Log.w(TAG, "Error fetching additional images: ${e.message}")
        }
    }

    private suspend fun checkExistingContactRequest(donorId: String) {
        val currentUserId = getCurrentUserId() ?: return

        try {
            Log.d(TAG, "Checking existing contact requests")
            val contactRequestsRef = database.child("contactRequests")
            val query = contactRequestsRef
                .orderByChild("recipientId")
                .equalTo(currentUserId)

            val snapshot = suspendCancellableCoroutine<DataSnapshot> { continuation ->
                val valueEventListener = object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        continuation.resume(snapshot)
                    }

                    override fun onCancelled(error: DatabaseError) {
                        continuation.resumeWithException(Exception(error.message))
                    }
                }

                query.addListenerForSingleValueEvent(valueEventListener)

                continuation.invokeOnCancellation {
                    query.removeEventListener(valueEventListener)
                }
            }

            // Check if there's an existing request for this donor
            var existingStatus: String? = null

            for (requestSnapshot in snapshot.children) {
                val requestDonorId = requestSnapshot.child("donorId").getValue(String::class.java)
                if (requestDonorId == donorId) {
                    existingStatus = requestSnapshot.child("status").getValue(String::class.java)
                    break
                }
            }

            _contactRequestStatus.value = existingStatus
            Log.d(TAG, "Contact request status: $existingStatus")

        } catch (e: Exception) {
            Log.e(TAG, "Error checking existing contact requests: ${e.message}")
            // Don't update status on error - keep as null
        }
    }

    fun sendContactRequest(donorId: String) {
        viewModelScope.launch {
            try {
                _statusMessage.value = null
                val currentUserId = getCurrentUserId() ?: throw Exception("User not authenticated")
                Log.d(TAG, "Sending contact request from $currentUserId to donor $donorId")

                val contactRequestRef = database.child("contactRequests").push()

                val contactRequest = hashMapOf(
                    "recipientId" to currentUserId,
                    "donorId" to donorId,
                    "status" to "pending",
                    "timestamp" to ServerValue.TIMESTAMP
                )

                contactRequestRef.setValue(contactRequest).await()

                // Update local state
                _contactRequestStatus.value = "pending"
                _statusMessage.value = "Contact request sent successfully"

                // Send a notification to the donor
                sendNotificationToDonor(donorId, currentUserId)

            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to send contact request"
                Log.e(TAG, "Error sending contact request: ${e.message}", e)
            }
        }
    }

    private suspend fun sendNotificationToDonor(donorId: String, recipientId: String) {
        try {
            Log.d(TAG, "Sending notification to donor: $donorId")
            val notificationsRef = database.child("notifications")
                .child(donorId)
                .push()

            val recipientSnapshot = database.child("recipient")
                .child(recipientId)
                .get()
                .await()

            val recipientName = recipientSnapshot.child("fullName").getValue(String::class.java) ?: "A recipient"

            val notification = hashMapOf(
                "type" to "contact_request",
                "senderId" to recipientId,
                "message" to "$recipientName has requested your contact information",
                "read" to false,
                "timestamp" to ServerValue.TIMESTAMP
            )

            notificationsRef.setValue(notification).await()
            Log.d(TAG, "Notification sent successfully to donor: $donorId")
        } catch (e: Exception) {
            // Silently fail notification sending
            Log.e(TAG, "Failed to send notification: ${e.message}", e)
        }
    }

    private fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }

    // Clear error message
    fun clearError() {
        _error.value = null
    }

    // Clear status message
    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    // Import for ServerValue
    object ServerValue {
        val TIMESTAMP: Any = com.google.firebase.database.ServerValue.TIMESTAMP
    }
}
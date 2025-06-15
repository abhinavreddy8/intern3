package com.example.last.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import android.util.Log

data class RequestData(
    val requestId: String = "",
    val recipientId: String = "",
    val recipientName: String = "",
    val hospitalId: String = "",
    val hospitalName: String = "",
    val description: String = "",
    val status: String = "pending",
    val timestamp: Long = 0,
    val response: String? = null
)

class HospitalRequestViewModel : ViewModel() {
    private val _requests = MutableStateFlow<List<RequestData>>(emptyList())
    val requests: StateFlow<List<RequestData>> = _requests.asStateFlow()

    var isLoading by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)

    private val database = FirebaseDatabase.getInstance().reference
    private val auth = FirebaseAuth.getInstance()

    fun fetchRequests() {
        val currentUser = auth.currentUser ?: return
        isLoading = true
        error = null

        database.child("recipient_requests").child(currentUser.uid)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val requestsList = mutableListOf<RequestData>()

                    for (requestSnapshot in snapshot.children) {
                        val requestId = requestSnapshot.child("requestId").getValue(String::class.java) ?: ""
                        val recipientId = requestSnapshot.child("recipientId").getValue(String::class.java) ?: ""
                        val recipientName = requestSnapshot.child("recipientName").getValue(String::class.java) ?: ""
                        val hospitalId = requestSnapshot.child("hospitalId").getValue(String::class.java) ?: ""
                        val hospitalName = requestSnapshot.child("hospitalName").getValue(String::class.java) ?: ""
                        val description = requestSnapshot.child("description").getValue(String::class.java) ?: ""
                        val status = requestSnapshot.child("status").getValue(String::class.java) ?: "pending"
                        val timestamp = requestSnapshot.child("timestamp").getValue(Long::class.java) ?: 0L
                        val response = requestSnapshot.child("response").getValue(String::class.java)

                        requestsList.add(
                            RequestData(
                                requestId, recipientId, recipientName, hospitalId,
                                hospitalName, description, status, timestamp, response
                            )
                        )
                    }

                    // Sort by timestamp (newest first)
                    requestsList.sortByDescending { it.timestamp }

                    _requests.value = requestsList
                    isLoading = false
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    error = databaseError.message
                    isLoading = false
                }
            })
    }

    /**
     * Updates the status of a request and sends a notification to the recipient
     * @param requestId ID of the request to update
     * @param newStatus New status ("accepted" or "rejected")
     * @param responseMessage Optional response message to the recipient
     */
    fun updateRequestStatus(requestId: String, newStatus: String, responseMessage: String) {
        val currentUser = auth.currentUser ?: return

        // Find the request in the current list
        val request = _requests.value.find { it.requestId == requestId } ?: return

        // Update the request status in the database
        val requestUpdates = HashMap<String, Any>()
        requestUpdates["status"] = newStatus
        requestUpdates["response"] = responseMessage

        Log.d("HospitalRequestViewModel", "Updating request $requestId to status: $newStatus")

        // Update in recipient's database
        database.child("recipient_requests")
            .child(request.recipientId)
            .child(requestId)
            .updateChildren(requestUpdates)
            .addOnSuccessListener {
                Log.d("HospitalRequestViewModel", "Request status updated successfully")
            }
            .addOnFailureListener { e ->
                Log.e("HospitalRequestViewModel", "Error updating request status", e)
                error = "Failed to update request status: ${e.message}"
            }

        // Also update in hospital's database if needed
        database.child("hospital_requests")
            .child(currentUser.uid)
            .child(requestId)
            .updateChildren(requestUpdates)

        // Create and send notification to the recipient
        createRecipientNotification(request, newStatus, responseMessage)
    }

    /**
     * Creates a notification for the recipient about their request status
     */
    private fun createRecipientNotification(request: RequestData, status: String, responseMessage: String) {
        val currentUser = auth.currentUser ?: return
        val notificationId = database.child("recipient_notifications").push().key ?: return

        val notification = hashMapOf(
            "notificationId" to notificationId,
            "hospitalName" to request.hospitalName,
            "status" to status,
            "timestamp" to System.currentTimeMillis(),
            "responseMessage" to responseMessage,
            "requestId" to request.requestId,
            "read" to false
        )

        Log.d("HospitalRequestViewModel", "Creating notification for recipient ${request.recipientId}")

        database.child("recipient_notifications")
            .child(request.recipientId)
            .child(notificationId)
            .setValue(notification)
            .addOnSuccessListener {
                Log.d("HospitalRequestViewModel", "Notification sent successfully")
            }
            .addOnFailureListener { e ->
                Log.e("HospitalRequestViewModel", "Error sending notification", e)
            }
    }
}
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

class HospitalRequestHandlerViewModel : ViewModel() {
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

        // Assuming the hospital ID is stored in the user's custom claims or in a separate node
        // For simplicity, we'll use the UID directly here
        val hospitalId = currentUser.uid

        database.child("hospital_requests").child(hospitalId)
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

    fun respondToRequest(requestId: String, status: String, response: String) {
        val currentUser = auth.currentUser ?: return
        val hospitalId = currentUser.uid

        // Get the request details first
        database.child("hospital_requests").child(hospitalId).child(requestId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val recipientId = snapshot.child("recipientId").getValue(String::class.java) ?: ""
                        val hospitalName = snapshot.child("hospitalName").getValue(String::class.java) ?: ""
                        val requestDescription = snapshot.child("description").getValue(String::class.java) ?: ""

                        // Update the request status in hospital_requests
                        val updatedValues = mapOf(
                            "status" to status,
                            "response" to response
                        )

                        database.child("hospital_requests").child(hospitalId).child(requestId)
                            .updateChildren(updatedValues)

                        // Update the request status in recipient_requests
                        database.child("recipient_requests").child(recipientId).child(requestId)
                            .updateChildren(updatedValues)

                        // Create notification for the recipient
                        val notificationId = database.child("recipient_notifications").child(recipientId).push().key ?: return

                        val notification = mapOf(
                            "notificationId" to notificationId,
                            "title" to "Request ${status.capitalize()}",
                            "content" to "Your request to $hospitalName has been $status.\nResponse: $response",
                            "type" to status,
                            "requestId" to requestId,
                            "hospitalId" to hospitalId,
                            "hospitalName" to hospitalName,
                            "requestDescription" to requestDescription,
                            "timestamp" to System.currentTimeMillis()
                        )

                        database.child("recipient_notifications").child(recipientId).child(notificationId)
                            .setValue(notification)
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    error = databaseError.message
                }
            })
    }
}
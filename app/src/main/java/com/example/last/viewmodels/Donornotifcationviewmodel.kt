    package com.example.last.viewmodels

    import android.util.Log
    import androidx.lifecycle.ViewModel
    import androidx.lifecycle.viewModelScope
    import com.google.firebase.auth.FirebaseAuth
    import com.google.firebase.database.DataSnapshot
    import com.google.firebase.database.DatabaseError
    import com.google.firebase.database.FirebaseDatabase
    import com.google.firebase.database.GenericTypeIndicator
    import com.google.firebase.database.ServerValue
    import com.google.firebase.database.ValueEventListener
    import kotlinx.coroutines.flow.MutableStateFlow
    import kotlinx.coroutines.flow.StateFlow
    import kotlinx.coroutines.launch
    import kotlinx.coroutines.suspendCancellableCoroutine
    import kotlinx.coroutines.tasks.await
    import kotlin.coroutines.resume
    import kotlin.coroutines.resumeWithException

    class DonorNotificationViewModel : ViewModel() {
        private val auth = FirebaseAuth.getInstance()
        private val database = FirebaseDatabase.getInstance().reference

        private val _requests = MutableStateFlow<List<ContactRequest>>(emptyList())
        val requests: StateFlow<List<ContactRequest>> = _requests

        private val _isLoading = MutableStateFlow(false)
        val isLoading: StateFlow<Boolean> = _isLoading

        private val _error = MutableStateFlow<String?>(null)
        val error: StateFlow<String?> = _error

        // Add status message for user feedback
        private val _statusMessage = MutableStateFlow<String?>(null)
        val statusMessage: StateFlow<String?> = _statusMessage

        data class ContactRequest(
            val id: String,
            val recipientId: String,
            val donorId: String,
            val status: String,
            val timestamp: Long,
            val recipientName: String
        )

        // Initialize real-time listener
        init {
            Log.d("DonorNotificationViewModel", "Initializing with real-time listener")
            setupRealTimeListener()
        }

        // Manual fetch function (useful for initial load and retries)
        fun fetchContactRequests() {
            val currentUserId = auth.currentUser?.uid ?: run {
                _error.value = "Please sign in to view contact requests"
                Log.e("DonorNotificationViewModel", "User not authenticated")
                return
            }

            Log.d("DonorNotificationViewModel", "Fetching requests for user: $currentUserId")

            viewModelScope.launch {
                _isLoading.value = true
                _error.value = null
                _statusMessage.value = null

                try {
                    val contactRequestsRef = database.child("contactRequests")
                    val query = contactRequestsRef.orderByChild("donorId")
                    val snapshot = getContactRequestsData(query)
                    val requestList = mutableListOf<ContactRequest>()

                    Log.d("DonorNotificationViewModel", "Raw snapshot data: ${snapshot.value}")

                    for (requestSnapshot in snapshot.children) {
                        val id = requestSnapshot.key ?: continue

                        // Don't use HashMap directly, we directly access the fields we need
                        Log.d("DonorNotificationViewModel", "Processing snapshot for $id")

                        val recipientId = requestSnapshot.child("recipientId").getValue(String::class.java) ?: continue
                        val donorId = requestSnapshot.child("donorId").getValue(String::class.java) ?: continue
                        val status = requestSnapshot.child("status").getValue(String::class.java) ?: "pending"
                        val timestamp = requestSnapshot.child("timestamp").getValue(Long::class.java) ?: System.currentTimeMillis()

                        Log.d("DonorNotificationViewModel", "Processing request: ID=$id, Status=$status, RecipientID=$recipientId")

                        val recipientName = getRecipientName(recipientId)
                        Log.d("DonorNotificationViewModel", "Recipient name: $recipientName")

                        // Include both pending and other status requests for visibility
                        requestList.add(
                            ContactRequest(
                                id = id,
                                recipientId = recipientId,
                                donorId = donorId,
                                status = status,
                                timestamp = timestamp,
                                recipientName = recipientName
                            )
                        )
                    }

                    // Sort by timestamp (newest first)
                    _requests.value = requestList.sortedByDescending { it.timestamp }
                    Log.d("DonorNotificationViewModel", "Total requests loaded: ${requestList.size}")

                    if (requestList.isEmpty()) {
                        _statusMessage.value = "No contact requests found"
                    }
                } catch (e: Exception) {
                    _error.value = e.message ?: "Failed to load contact requests"
                    Log.e("DonorNotificationViewModel", "Error fetching requests: ${e.message}", e)
                } finally {
                    _isLoading.value = false
                }
            }
        }

        private fun setupRealTimeListener() {
            val currentUserId = auth.currentUser?.uid ?: run {
                Log.e("DonorNotificationViewModel", "Cannot setup real-time listener: User not authenticated")
                return
            }

            Log.d("DonorNotificationViewModel", "Setting up real-time listener for user: $currentUserId")

            val contactRequestsRef = database.child("contactRequests")
            val query = contactRequestsRef.orderByChild("donorId").equalTo(currentUserId)

            query.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    Log.d("DonorNotificationViewModel", "Real-time update received, data changed")
                    if (!snapshot.exists()) {
                        Log.d("DonorNotificationViewModel", "No contact requests found in real-time update")
                        _requests.value = emptyList()
                        return
                    }

                    viewModelScope.launch {
                        val requestList = mutableListOf<ContactRequest>()
                        for (requestSnapshot in snapshot.children) {
                            val id = requestSnapshot.key ?: continue
                            val recipientId = requestSnapshot.child("recipientId").getValue(String::class.java) ?: continue
                            val donorId = requestSnapshot.child("donorId").getValue(String::class.java) ?: continue
                            val status = requestSnapshot.child("status").getValue(String::class.java) ?: "pending"
                            val timestamp = requestSnapshot.child("timestamp").getValue(Long::class.java) ?: System.currentTimeMillis()

                            // Get recipient name for each request
                            val recipientName = getRecipientName(recipientId)
                            Log.d("DonorNotificationViewModel", "Real-time update - Request: $id, Status: $status, Recipient: $recipientName")

                            // Include all requests for visibility, not just pending ones
                            requestList.add(
                                ContactRequest(
                                    id = id,
                                    recipientId = recipientId,
                                    donorId = donorId,
                                    status = status,
                                    timestamp = timestamp,
                                    recipientName = recipientName
                                )
                            )
                        }
                        _requests.value = requestList.sortedByDescending { it.timestamp }
                        Log.d("DonorNotificationViewModel", "Real-time update: ${requestList.size} requests loaded")
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    _error.value = error.message
                    Log.e("DonorNotificationViewModel", "Real-time listener cancelled: ${error.message}")
                }
            })
        }

        private suspend fun getContactRequestsData(query: com.google.firebase.database.Query): DataSnapshot =
            suspendCancellableCoroutine { continuation ->
                val valueEventListener = object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        Log.d("DonorNotificationViewModel", "Single event query result: ${snapshot.exists()}")
                        continuation.resume(snapshot)
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e("DonorNotificationViewModel", "Database error: ${error.message}")
                        continuation.resumeWithException(Exception(error.message))
                    }
                }
                query.addListenerForSingleValueEvent(valueEventListener)
                continuation.invokeOnCancellation {
                    query.removeEventListener(valueEventListener)
                }
            }

        private suspend fun getRecipientName(recipientId: String): String {
            return try {
                val snapshot = database.child("recipient").child(recipientId).get().await()
                val name = snapshot.child("fullName").getValue(String::class.java)
                Log.d("DonorNotificationViewModel", "Retrieved recipient name for $recipientId: $name")
                name ?: "Unknown Recipient"
            } catch (e: Exception) {
                Log.e("DonorNotificationViewModel", "Failed to get recipient name: ${e.message}")
                "Unknown Recipient"
            }
        }

        fun acceptRequest(request: ContactRequest) {
            viewModelScope.launch {
                try {
                    _statusMessage.value = null

                    Log.d("DonorNotificationViewModel", "Accepting request: ${request.id}")
                    database.child("contactRequests")
                        .child(request.id)
                        .child("status")
                        .setValue("accepted")
                        .await()

                    // Update contact sharing permissions in database
                    updateContactSharingPermission(request.recipientId, request.donorId, true)

                    // Send notification
                    sendAcceptanceNotification(request.recipientId)

                    _statusMessage.value = "Request accepted"
                    Log.d("DonorNotificationViewModel", "Request accepted: ${request.id}")
                } catch (e: Exception) {
                    _error.value = e.message ?: "Failed to accept request"
                    Log.e("DonorNotificationViewModel", "Accept error: ${e.message}")
                }
            }
        }

        fun rejectRequest(request: ContactRequest) {
            viewModelScope.launch {
                try {
                    _statusMessage.value = null

                    Log.d("DonorNotificationViewModel", "Rejecting request: ${request.id}")
                    database.child("contactRequests")
                        .child(request.id)
                        .child("status")
                        .setValue("rejected")
                        .await()

                    _statusMessage.value = "Request rejected"
                    Log.d("DonorNotificationViewModel", "Request rejected: ${request.id}")
                } catch (e: Exception) {
                    _error.value = e.message ?: "Failed to reject request"
                    Log.e("DonorNotificationViewModel", "Reject error: ${e.message}")
                }
            }
        }

        private suspend fun updateContactSharingPermission(recipientId: String, donorId: String, granted: Boolean) {
            try {
                val sharingPermissionsRef = database.child("contactSharingPermissions")
                    .child("${donorId}_$recipientId")

                val permissionData = hashMapOf(
                    "donorId" to donorId,
                    "recipientId" to recipientId,
                    "granted" to granted,
                    "timestamp" to ServerValue.TIMESTAMP
                )

                sharingPermissionsRef.setValue(permissionData).await()
                Log.d("DonorNotificationViewModel", "Contact sharing permission updated: donor=$donorId, recipient=$recipientId, granted=$granted")
            } catch (e: Exception) {
                Log.e("DonorNotificationViewModel", "Failed to update sharing permissions: ${e.message}")
                // Don't rethrow, treat as non-critical
            }
        }

        private suspend fun sendAcceptanceNotification(recipientId: String) {
            try {
                val notificationsRef = database.child("notifications").child(recipientId).push()
                val currentUser = auth.currentUser
                val donorName = currentUser?.displayName ?: "A donor"

                // Get donor details for richer notification
                val donorId = currentUser?.uid
                val donorDetails = if (donorId != null) {
                    try {
                        val donorSnapshot = database.child("donor").child(donorId).get().await()
                        donorSnapshot.child("fullName").getValue(String::class.java) ?: donorName
                    } catch (e: Exception) {
                        donorName
                    }
                } else {
                    donorName
                }

                val notification = hashMapOf(
                    "type" to "request_accepted",
                    "senderId" to auth.currentUser?.uid,
                    "message" to "$donorDetails has accepted your contact request. You can now see their contact information.",
                    "read" to false,
                    "timestamp" to ServerValue.TIMESTAMP
                )

                notificationsRef.setValue(notification).await()
                Log.d("DonorNotificationViewModel", "Acceptance notification sent to: $recipientId")
            } catch (e: Exception) {
                Log.e("DonorNotificationViewModel", "Failed to send notification: ${e.message}")
                // Don't rethrow, treat as non-critical
            }
        }

        // Clear error message
        fun clearError() {
            _error.value = null
        }

        // Clear status message
        fun clearStatusMessage() {
            _statusMessage.value = null
        }
    }
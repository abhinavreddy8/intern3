package com.example.last.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import android.util.Log

data class NotificationData(
    val notificationId: String = "",
    val hospitalName: String = "",
    val status: String = "",
    val timestamp: Long = 0,
    val responseMessage: String? = null,
    val requestId: String = "",
    val type: NotificationType = NotificationType.HOSPITAL
)

enum class NotificationType {
    HOSPITAL, DONOR
}

class RecipientNotificationsViewModel : ViewModel() {
    private val _notifications = MutableStateFlow<List<NotificationData>>(emptyList())
    val notifications: StateFlow<List<NotificationData>> = _notifications.asStateFlow()

    var isLoading by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)

    private val database = FirebaseDatabase.getInstance().reference
    private val auth = FirebaseAuth.getInstance()

    fun fetchNotifications() {
        val currentUser = auth.currentUser ?: return
        isLoading = true
        error = null

        Log.d("RecipientNotificationsVM", "Fetching notifications for user: ${currentUser.uid}")

        // Fetching hospital notifications
        database.child("recipient_notifications").child(currentUser.uid)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val notificationList = mutableListOf<NotificationData>()

                    for (notificationSnapshot in snapshot.children) {
                        val notificationId = notificationSnapshot.child("notificationId").getValue(String::class.java) ?: ""
                        val hospitalName = notificationSnapshot.child("hospitalName").getValue(String::class.java) ?: ""
                        val status = notificationSnapshot.child("status").getValue(String::class.java) ?: ""
                        val timestamp = notificationSnapshot.child("timestamp").getValue(Long::class.java) ?: 0L
                        val responseMessage = notificationSnapshot.child("responseMessage").getValue(String::class.java)
                        val requestId = notificationSnapshot.child("requestId").getValue(String::class.java) ?: ""

                        val notification = NotificationData(
                            notificationId = notificationId,
                            hospitalName = hospitalName,
                            status = status,
                            timestamp = timestamp,
                            responseMessage = responseMessage,
                            requestId = requestId,
                            type = NotificationType.HOSPITAL
                        )

                        notificationList.add(notification)
                        Log.d("RecipientNotificationsVM", "Found hospital notification: $notificationId from $hospitalName")
                    }

                    // Fetching donor requests
                    fetchDonorRequests(currentUser.uid, notificationList)
                }

                override fun onCancelled(error: DatabaseError) {
                    this@RecipientNotificationsViewModel.error = error.message
                    isLoading = false
                    Log.e("RecipientNotificationsVM", "Error loading notifications: ${error.message}")
                }
            })
    }

    private fun fetchDonorRequests(userId: String, notificationList: MutableList<NotificationData>) {
        // Fetching donor requests
        database.child("contactRequests").child(userId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (requestSnapshot in snapshot.children) {
                        val notificationId = requestSnapshot.child("requestId").getValue(String::class.java) ?: ""
                        val status = requestSnapshot.child("status").getValue(String::class.java) ?: ""
                        val timestamp = requestSnapshot.child("timestamp").getValue(Long::class.java) ?: 0L
                        val responseMessage = requestSnapshot.child("responseMessage").getValue(String::class.java)
                        val requestId = requestSnapshot.child("requestId").getValue(String::class.java) ?: ""

                        val notification = NotificationData(
                            notificationId = notificationId,
                            hospitalName = "Donor Request",
                            status = status,
                            timestamp = timestamp,
                            responseMessage = responseMessage,
                            requestId = requestId,
                            type = NotificationType.DONOR
                        )

                        notificationList.add(notification)
                        Log.d("RecipientNotificationsVM", "Found donor request notification: $notificationId")
                    }

                    // Sort by timestamp (newest first)
                    notificationList.sortByDescending { it.timestamp }
                    _notifications.value = notificationList
                    isLoading = false
                    Log.d("RecipientNotificationsVM", "Loaded ${notificationList.size} notifications")
                }

                override fun onCancelled(error: DatabaseError) {
                    this@RecipientNotificationsViewModel.error = error.message
                    isLoading = false
                    Log.e("RecipientNotificationsVM", "Error loading donor requests: ${error.message}")
                }
            })
    }

    fun markAsRead(notificationId: String) {
        val currentUser = auth.currentUser ?: return

        database.child("recipient_notifications")
            .child(currentUser.uid)
            .child(notificationId)
            .child("read")
            .setValue(true)
            .addOnSuccessListener {
                Log.d("RecipientNotificationsVM", "Notification marked as read: $notificationId")
            }
            .addOnFailureListener { e ->
                Log.e("RecipientNotificationsVM", "Error marking notification as read", e)
            }
    }
}

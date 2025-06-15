// DonorSearchViewModel.kt
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
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference

data class RecipientData(
    val id: String = "",
    val name: String = "",
    val bloodType: String = "",
    val location: String = "",
    val imageUrl: String = "",
    val organNeeded: String = ""
)

class Donorsearchviewmodel : ViewModel() {
    var recipients by mutableStateOf<List<RecipientData>>(emptyList())
    var isLoading by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)

    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance().reference.child("recipient")
    private lateinit var storage: FirebaseStorage
    private lateinit var storageReference: StorageReference

    init {
        fetchRecipients()
    }

    fun fetchRecipients() {
        isLoading = true
        error = null

        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val recipientsList = mutableListOf<RecipientData>()

                for (recipientSnapshot in snapshot.children) {
                    val id = recipientSnapshot.key ?: ""
                    val name = recipientSnapshot.child("fullName").getValue(String::class.java) ?: ""
                    val bloodType = recipientSnapshot.child("bloodGroup").getValue(String::class.java) ?: ""
                    val location = recipientSnapshot.child("location").getValue(String::class.java) ?: ""
                    val imageUrl = recipientSnapshot.child("imageUrl").getValue(String::class.java) ?: ""
                    val organNeeded = recipientSnapshot.child("organNeeded").getValue(String::class.java) ?: ""
                    recipientsList.add(RecipientData(id, name, bloodType, location, imageUrl,organNeeded))
                }

                recipients = recipientsList
                isLoading = false
            }

            override fun onCancelled(error: DatabaseError) {
                this@Donorsearchviewmodel.error = error.message
                isLoading = false
            }
        })
    }

    fun initializeStorage(appId: String, apiKey: String, projectId: String, bucketUrl: String) {
        storage = FirebaseStorage.getInstance()
        storageReference = storage.reference
    }
}
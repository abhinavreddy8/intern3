package com.example.last.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage

data class HospitalData(
    val id: String,
    val name: String,
    val address: String,
    val contact: String,
    val imageUrl: String,
    val specialties: String
)

class RecipientSearchhosviewmodel: ViewModel() {
    var hospitals by mutableStateOf<List<HospitalData>>(emptyList())
    var isLoading by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)

    private val database = FirebaseDatabase.getInstance().reference.child("hospital")
    private lateinit var storage: FirebaseStorage

    init {
        fetchHospitals()
    }

    fun fetchHospitals() {
        isLoading = true
        error = null

        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val hospitalsList = mutableListOf<HospitalData>()

                for (hospitalSnapshot in snapshot.children) {
                    val id = hospitalSnapshot.key ?: ""
                    val name = hospitalSnapshot.child("name").getValue(String::class.java) ?: ""
                    val address = hospitalSnapshot.child("address").getValue(String::class.java) ?: ""
                    val contact = hospitalSnapshot.child("phoneNumber").getValue(String::class.java) ?: ""
                    val imageUrl = hospitalSnapshot.child("logoUrl").getValue(String::class.java) ?: ""
                    val specialties = hospitalSnapshot.child("specializations").getValue(String::class.java) ?: ""

                    hospitalsList.add(HospitalData(id, name, address, contact, imageUrl, specialties))
                }

                hospitals = hospitalsList
                isLoading = false
            }

            override fun onCancelled(error: DatabaseError) {
                this@RecipientSearchhosviewmodel.error = error.message
                isLoading = false
            }
        })
    }

    fun initializeStorage(appId: String, apiKey: String, projectId: String, bucketUrl: String) {
        storage = FirebaseStorage.getInstance()
    }
}
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

data class HospitallData(
    val id: String,
    val name: String,
    val address: String,
    val contact: String,
    val imageUrl: String,
    val specialties: String
)

class DonorSearchHospitalViewModel : ViewModel() {
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

                snapshot.children.forEach { hospitalSnapshot ->
                    hospitalSnapshot.key?.let { id ->
                        hospitalsList.add(
                            HospitalData(
                                id = id,
                                name = hospitalSnapshot.child("name").getValue(String::class.java) ?: "",
                                address = hospitalSnapshot.child("address").getValue(String::class.java) ?: "",
                                contact = hospitalSnapshot.child("phoneNumber").getValue(String::class.java) ?: "",
                                imageUrl = hospitalSnapshot.child("imageUrl").getValue(String::class.java) ?: "",
                                specialties = hospitalSnapshot.child("specializations").getValue(String::class.java) ?: ""
                            )
                        )
                    }
                }

                hospitals = hospitalsList
                isLoading = false
            }

            override fun onCancelled(error: DatabaseError) {
                this@DonorSearchHospitalViewModel.error = error.message
                isLoading = false
            }
        })
    }

    fun initializeStorage() {
        storage = FirebaseStorage.getInstance()
    }
}
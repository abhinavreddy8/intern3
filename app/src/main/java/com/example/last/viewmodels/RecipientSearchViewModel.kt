package com.example.last.viewmodels

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.last.DonorApiService
import com.example.last.DonorRequest
import com.example.last.Retrofitclient
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.*

data class DonorData(
    val id: String,
    val name: String,
    val bloodType: String,
    val location: String,
    val imageUrl: String,
    val organDonating: String,
    val requestStatus: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val distance: Double? = null,
    val clusterGroup: Int = -1
)

class RecipientSearchViewModel : ViewModel() {
    var donors by mutableStateOf<List<DonorData>>(emptyList())
    var nearbyDonors by mutableStateOf<List<DonorData>>(emptyList())
    var isLoading by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)
    var locationInitialized by mutableStateOf(false)
    var toastMessage by mutableStateOf<String?>(null)
    var isNearbySearchActive by mutableStateOf(false)

    private var currentLatitude: Double? = null
    private var currentLongitude: Double? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var context: Context? = null

    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance().reference
    private val donorsRef = database.child("donor")
    private val contactRequestsRef = database.child("contactRequests")
    private lateinit var storage: FirebaseStorage
    private lateinit var storageReference: StorageReference
    private val apiService: DonorApiService = Retrofitclient.donorApiService

    init {
        fetchDonors()
    }

    fun setContext(context: Context) {
        this.context = context
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        Log.d("RecipientSearchVM", "Context set for location client")
    }

    fun showToast(message: String) {
        toastMessage = message
        Log.d("RecipientSearchVM", message)
    }

    fun updateField(field: String, value: String) {
        when (field) {
            "latitude" -> {
                currentLatitude = value.toDoubleOrNull()
                if (currentLatitude != null && currentLongitude != null) {
                    locationInitialized = true
                    showToast("Current location initialized: $currentLatitude, $currentLongitude")
                    Log.d("RecipientSearchVM", "Current location updated: lat=$currentLatitude, lon=$currentLongitude")
                    if (isNearbySearchActive) {
                        findNearbyDonorsWithLocation()
                    }
                } else {
                    Log.d("RecipientSearchVM", "Latitude updated but location not complete: $currentLatitude")
                }
            }
            "longitude" -> {
                currentLongitude = value.toDoubleOrNull()
                if (currentLatitude != null && currentLongitude != null) {
                    locationInitialized = true
                    showToast("Current location initialized: $currentLatitude, $currentLongitude")
                    Log.d("RecipientSearchVM", "Current location updated: lat=$currentLatitude, lon=$currentLongitude")
                    if (isNearbySearchActive) {
                        findNearbyDonorsWithLocation()
                    }
                } else {
                    Log.d("RecipientSearchVM", "Longitude updated but location not complete: $currentLongitude")
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun fetchLocation(onComplete: (Boolean) -> Unit = {}) {
        context?.let { ctx ->
            try {
                isLoading = true
                showToast("Getting location...")
                Log.d("RecipientSearchVM", "Fetching current location")
                fusedLocationClient.lastLocation
                    .addOnSuccessListener { location: Location? ->
                        if (location != null) {
                            showToast("Location obtained: ${location.latitude}, ${location.longitude}")
                            Log.d("RecipientSearchVM", "Location obtained: ${location.latitude}, ${location.longitude}")
                            updateField("latitude", location.latitude.toString())
                            updateField("longitude", location.longitude.toString())
                            isLoading = false
                            onComplete(true)
                        } else {
                            showToast("Location is null. Requesting location updates.")
                            Log.d("RecipientSearchVM", "Location is null, requesting location updates")
                            requestLocationUpdates(onComplete)
                        }
                    }
                    .addOnFailureListener { e ->
                        showToast("Failed to get location: ${e.message}")
                        Log.d("RecipientSearchVM", "Failed to get location: ${e.message}")
                        requestLocationUpdates(onComplete)
                    }
            } catch (e: Exception) {
                showToast("Error in location fetch: ${e.message}")
                Log.d("RecipientSearchVM", "Error in location fetch: ${e.message}")
                isLoading = false
                onComplete(false)
            }
        } ?: run {
            error = "Context not initialized"
            showToast(error!!)
            Log.d("RecipientSearchVM", "Context not initialized for location fetch")
            onComplete(false)
        }
    }

    @SuppressLint("MissingPermission")
    private fun requestLocationUpdates(onComplete: (Boolean) -> Unit) {
        context?.let { ctx ->
            try {
                val locationRequest = LocationRequest.create().apply {
                    priority = LocationRequest.PRIORITY_HIGH_ACCURACY
                    interval = 10000
                    fastestInterval = 5000
                    numUpdates = 1
                }

                val locationCallback = object : LocationCallback() {
                    override fun onLocationResult(locationResult: LocationResult) {
                        super.onLocationResult(locationResult)
                        locationResult.lastLocation?.let { location ->
                            showToast("Location update received: ${location.latitude}, ${location.longitude}")
                            Log.d("RecipientSearchVM", "Location update received: ${location.latitude}, ${location.longitude}")
                            updateField("latitude", location.latitude.toString())
                            updateField("longitude", location.longitude.toString())
                            fusedLocationClient.removeLocationUpdates(this)
                            isLoading = false
                            onComplete(true)
                        } ?: run {
                            showToast("Location update failed: No location received")
                            Log.d("RecipientSearchVM", "Location update failed: No location received")
                            isLoading = false
                            onComplete(false)
                        }
                    }
                }

                fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    ctx.mainLooper
                )
                showToast("Requested location updates")
                Log.d("RecipientSearchVM", "Requested location updates")
            } catch (e: Exception) {
                showToast("Failed to request location updates: ${e.message}")
                Log.d("RecipientSearchVM", "Failed to request location updates: ${e.message}")
                isLoading = false
                onComplete(false)
            }
        } ?: run {
            error = "Context not initialized"
            showToast(error!!)
            Log.d("RecipientSearchVM", "Context not initialized for location updates")
            onComplete(false)
        }
    }

    fun fetchDonors() {
        isLoading = true
        error = null
        Log.d("RecipientSearchVM", "Fetching donors from Firebase")

        val currentRecipientId = auth.currentUser?.uid ?: return

        contactRequestsRef.orderByChild("recipientId").equalTo(currentRecipientId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(requestsSnapshot: DataSnapshot) {
                    val donorRequestStatusMap = mutableMapOf<String, String>()

                    for (requestSnapshot in requestsSnapshot.children) {
                        val donorId = requestSnapshot.child("donorId").getValue(String::class.java) ?: continue
                        val status = requestSnapshot.child("status").getValue(String::class.java) ?: "pending"
                        donorRequestStatusMap[donorId] = status
                    }

                    donorsRef.addValueEventListener(object : ValueEventListener {
                        override fun onDataChange(donorsSnapshot: DataSnapshot) {
                            val donorsList = mutableListOf<DonorData>()
                            showToast("Found ${donorsSnapshot.childrenCount} donors in database")
                            Log.d("RecipientSearchVM", "Fetched ${donorsSnapshot.childrenCount} donors from Firebase")

                            for (donorSnapshot in donorsSnapshot.children) {
                                val id = donorSnapshot.key ?: ""
                                val name = donorSnapshot.child("fullName").getValue(String::class.java) ?: ""
                                val bloodType = donorSnapshot.child("bloodGroup").getValue(String::class.java) ?: ""
                                val location = donorSnapshot.child("location").getValue(String::class.java) ?: ""
                                val imageUrl = donorSnapshot.child("ProfileImageUrl").getValue(String::class.java) ?: ""
                                val organDonating = donorSnapshot.child("organAvailable").getValue(String::class.java) ?: ""
                                val latitudeStr = donorSnapshot.child("latitude").getValue(String::class.java)
                                val longitudeStr = donorSnapshot.child("longitude").getValue(String::class.java)
                                val latitude = latitudeStr?.toDoubleOrNull()
                                val longitude = longitudeStr?.toDoubleOrNull()
                                val requestStatus = donorRequestStatusMap[id]

                                showToast("Donor: $name, Lat: $latitude, Lon: $longitude")
                                Log.d("RecipientSearchVM", "Donor: $name, Lat: $latitude, Lon: $longitude (from strings: $latitudeStr, $longitudeStr)")

                                if (latitude == null || longitude == null || !latitude.isFinite() || !longitude.isFinite()) {
                                    showToast("Donor $name has no valid location data")
                                    Log.d("RecipientSearchVM", "Donor $name has no valid location data: lat=$latitude (from $latitudeStr), lon=$longitude (from $longitudeStr)")
                                }

                                donorsList.add(
                                    DonorData(
                                        id = id,
                                        name = name,
                                        bloodType = bloodType,
                                        location = location,
                                        imageUrl = imageUrl,
                                        organDonating = organDonating,
                                        requestStatus = requestStatus,
                                        latitude = latitude,
                                        longitude = longitude
                                    )
                                )
                            }

                            donors = donorsList
                            isLoading = false
                            showToast("Loaded ${donors.size} donors total")
                            Log.d("RecipientSearchVM", "Loaded ${donors.size} donors into state")

                            if (isNearbySearchActive && locationInitialized) {
                                Log.d("RecipientSearchVM", "Triggering nearby donors filter after donor fetch")
                                findNearbyDonorsWithLocation()
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            this@RecipientSearchViewModel.error = error.message
                            showToast(error.message)
                            Log.d("RecipientSearchVM", "Donor fetch cancelled: ${error.message}")
                            isLoading = false
                        }
                    })
                }

                override fun onCancelled(error: DatabaseError) {
                    this@RecipientSearchViewModel.error = error.message
                    showToast(error.message)
                    Log.d("RecipientSearchVM", "Contact requests fetch cancelled: ${error.message}")
                    isLoading = false
                }
            })
    }

    fun findNearbyDonors() {
        isNearbySearchActive = true
        nearbyDonors = emptyList()
        Log.d("RecipientSearchVM", "Find nearby donors triggered, isNearbySearchActive=$isNearbySearchActive")

        if (currentLatitude == null || currentLongitude == null) {
            showToast("Fetching current location for nearby search...")
            Log.d("RecipientSearchVM", "Current location not available, fetching location")
            fetchLocation { success ->
                if (success) {
                    Log.d("RecipientSearchVM", "Location fetched successfully, proceeding to API call")
                    findNearbyDonorsWithLocation()
                } else {
                    error = "Unable to get current location"
                    showToast(error!!)
                    Log.d("RecipientSearchVM", "Failed to fetch location, resetting nearby search")
                    isNearbySearchActive = false
                    nearbyDonors = emptyList()
                    isLoading = false
                }
            }
        } else {
            Log.d("RecipientSearchVM", "Current location available, proceeding to API call")
            findNearbyDonorsWithLocation()
        }
    }

    private fun findNearbyDonorsWithLocation() {
        if (currentLatitude == null || currentLongitude == null) {
            error = "Current location not available"
            showToast(error!!)
            Log.d("RecipientSearchVM", "findNearbyDonorsWithLocation failed: Current location not available")
            isNearbySearchActive = false
            isLoading = false
            nearbyDonors = emptyList()
            return
        }

        isLoading = true
        showToast("Searching for nearby donors using API at: $currentLatitude, $currentLongitude")
        Log.d("RecipientSearchVM", "Calling API for nearby donors with location: $currentLatitude, $currentLongitude")

        viewModelScope.launch {
            try {
                val request = DonorRequest(latitude = currentLatitude!!, longitude = currentLongitude!!)
                val response = apiService.findNearbyDonors(request)
                showToast("API response: ${response.nearbyDonors} donors in cluster ${response.cluster}")
                Log.d("RecipientSearchVM", "API response: ${response.nearbyDonors} donors in cluster ${response.cluster}, modelUsed=${response.modelUsed}, modelStatus=${response.modelStatus}")

                // Assign clusters to donors based on their coordinates
                val donorsWithCluster = donors.map { donor ->
                    if (donor.latitude != null && donor.longitude != null && donor.latitude.isFinite() && donor.longitude.isFinite()) {
                        try {
                            val donorRequest = DonorRequest(donor.latitude, donor.longitude)
                            val donorResponse = apiService.findNearbyDonors(donorRequest)
                            Log.d("RecipientSearchVM", "Donor ${donor.name} assigned cluster: ${donorResponse.cluster}")
                            donor.copy(clusterGroup = donorResponse.cluster)
                        } catch (e: Exception) {
                            Log.d("RecipientSearchVM", "Error getting cluster for donor ${donor.name}: ${e.message}")
                            donor
                        }
                    } else {
                        donor
                    }
                }

                // Filter donors in the same cluster as the user and within 10 km
                val maxDistanceInKm = 10.0
                val filteredNearbyDonors = donorsWithCluster.filter { donor ->
                    if (donor.clusterGroup != null && donor.clusterGroup == response.cluster &&
                        donor.latitude != null && donor.longitude != null) {
                        val distance = calculateHaversineDistance(
                            currentLatitude!!,
                            currentLongitude!!,
                            donor.latitude,
                            donor.longitude
                        )
                        Log.d("RecipientSearchVM", "Donor ${donor.name} distance: $distance km")
                        distance <= maxDistanceInKm
                    } else {
                        false
                    }
                }

                nearbyDonors = filteredNearbyDonors
                isLoading = false
                showToast("Found ${filteredNearbyDonors.size} donors in cluster ${response.cluster} within ${maxDistanceInKm}km")
                Log.d("RecipientSearchVM", "Populated nearbyDonors with ${filteredNearbyDonors.size} donors in cluster ${response.cluster}")

                if (filteredNearbyDonors.isEmpty()) {
                    showToast("No donors found in your cluster within ${maxDistanceInKm}km")
                    Log.d("RecipientSearchVM", "No nearby donors found in cluster ${response.cluster} within ${maxDistanceInKm}km")
                } else {
                    filteredNearbyDonors.forEach { donor ->
                        val distance = calculateHaversineDistance(
                            currentLatitude!!,
                            currentLongitude!!,
                            donor.latitude!!,
                            donor.longitude!!
                        )
                        showToast("Nearby donor: ${donor.name}, cluster: ${donor.clusterGroup}, distance: $distance km")
                        Log.d("RecipientSearchVM", "Nearby donor: ${donor.name}, cluster: ${donor.clusterGroup}, distance: $distance km")
                    }
                }
            } catch (e: Exception) {
                error = "API call failed: ${e.message}"
                showToast(error!!)
                Log.d("RecipientSearchVM", "API call failed: ${e.message}")
                isNearbySearchActive = false
                isLoading = false
                nearbyDonors = emptyList()
            }
        }
    }

    private fun calculateHaversineDistance(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Double {
        val r = 6371.0
        val latDistance = Math.toRadians(lat2 - lat1)
        val lonDistance = Math.toRadians(lon2 - lon1)
        val a = sin(latDistance / 2) * sin(latDistance / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(lonDistance / 2) * sin(lonDistance / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        val distance = r * c
        Log.d("RecipientSearchVM", "Haversine distance calculated: $distance km between ($lat1, $lon1) and ($lat2, $lon2)")
        return distance
    }

    fun resetNearbySearch() {
        isNearbySearchActive = false
        nearbyDonors = emptyList()
        showToast("Reset nearby search")
        Log.d("RecipientSearchVM", "Nearby search reset, showing all donors")
    }

    fun initializeStorage(appId: String, apiKey: String, projectId: String, bucketUrl: String) {
        storage = FirebaseStorage.getInstance()
        storageReference = storage.reference
        Log.d("RecipientSearchVM", "Firebase Storage initialized")
    }
}
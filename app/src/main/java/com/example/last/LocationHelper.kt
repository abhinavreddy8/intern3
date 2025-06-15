package com.example.last.utils

import android.content.Context
import android.location.Location
import androidx.compose.runtime.mutableStateOf
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Helper class to manage location operations
 */
class LocationHelper(private val context: Context) {

    private val fusedLocationClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }

    val isLocationLoading = mutableStateOf(false)
    val lastLocation = mutableStateOf<Location?>(null)
    val locationError = mutableStateOf<String?>(null)

    /**
     * Request current location with high accuracy
     */
    suspend fun getCurrentLocation(): Location? {
        isLocationLoading.value = true
        locationError.value = null

        return try {
            val cancellationTokenSource = CancellationTokenSource()
            val location = fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                cancellationTokenSource.token
            ).await()

            lastLocation.value = location
            isLocationLoading.value = false
            location
        } catch (e: Exception) {
            locationError.value = "Failed to get location: ${e.message}"
            isLocationLoading.value = false
            null
        }
    }

    /**
     * Calculate distance between two coordinates in kilometers
     */
    fun calculateDistance(
        lat1: Double, lng1: Double,
        lat2: Double, lng2: Double
    ): Float {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lng1, lat2, lng2, results)
        // Convert from meters to kilometers
        return results[0] / 1000
    }

    /**
     * Format location coordinates to string representation
     */
    fun formatLocationCoordinates(latitude: Double, longitude: Double): String {
        return "${String.format("%.6f", latitude)}, ${String.format("%.6f", longitude)}"
    }

    /**
     * Parse location string to coordinates
     * Expected format: "latitude, longitude"
     */
    fun parseLocationString(locationString: String): Pair<Double, Double>? {
        return try {
            val parts = locationString.split(",").map { it.trim() }
            if (parts.size == 2) {
                val lat = parts[0].toDouble()
                val lng = parts[1].toDouble()
                Pair(lat, lng)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}
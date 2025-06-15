package com.example.last.viewmodels

import android.Manifest
import android.content.Context
import android.location.Location
import androidx.annotation.RequiresPermission
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Task

class LocationViewModel : ViewModel() {
    var currentLatitude by mutableStateOf(0.0)
    var currentLongitude by mutableStateOf(0.0)
    var isLocationReady by mutableStateOf(false)
    var locationError by mutableStateOf<String?>(null)

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    fun getCurrentLocation(context: Context, onLocationReceived: (Double, Double) -> Unit) {
        try {
            val fusedLocationClient: FusedLocationProviderClient =
                LocationServices.getFusedLocationProviderClient(context)

            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    location?.let {
                        currentLatitude = it.latitude
                        currentLongitude = it.longitude
                        isLocationReady = true
                        onLocationReceived(it.latitude, it.longitude)
                    } ?: run {
                        locationError = "Unable to get current location"
                    }
                }
                .addOnFailureListener { e ->
                    locationError = "Location error: ${e.message}"
                }
        } catch (e: Exception) {
            locationError = "Error accessing location services: ${e.message}"
        }
    }
}
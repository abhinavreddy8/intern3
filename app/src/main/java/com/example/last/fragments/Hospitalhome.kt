package com.example.last

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.last.fragments.HospitalHomeViewModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
@Preview
@Composable
fun Hospitalhome(
    viewModel: HospitalHomeViewModel = viewModel()
) {
    var isEditing by remember { mutableStateOf(false) }
    val hospitalData by viewModel.hospitalData.collectAsState(initial = null)
    val toastMessage by viewModel.toastMessage.collectAsState(initial = null)
    val context = LocalContext.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    // Permission state
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    // Show toast messages from ViewModel
    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearToastMessage()
        }
    }

    // Permission launcher
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasLocationPermission = isGranted
        if (isGranted) {
            viewModel.showToast("Permission granted, fetching location...")
            fetchLocation(fusedLocationClient, context, viewModel)
        } else {
            viewModel.showToast("Location permission denied")
        }
    }

    // Image picker launcher
    val logoImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.uploadImage(it, "logo") }
    }

    val certificateImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.uploadImage(it, "certificates") }
    }

    LaunchedEffect(Unit) {
        viewModel.fetchHospitalData()
        viewModel.fetchLogoImage()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Hospital Logo Section
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(hospitalData?.logoUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = "Hospital Logo",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        Button(
            onClick = { logoImageLauncher.launch("image/*") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Text("Upload Logo")
        }

        // Hospital Information Section
        SectionHeader(text = "Hospital Details")

        HospitalTextField(
            value = hospitalData?.hospitalName ?: "",
            onValueChange = { viewModel.updateField("hospitalName", it) },
            label = "Hospital Name",
            enabled = isEditing
        )

        HospitalTextField(
            value = hospitalData?.hospitalId ?: "",
            onValueChange = { viewModel.updateField("hospitalId", it) },
            label = "Hospital ID",
            enabled = isEditing
        )

        HospitalTextField(
            value = hospitalData?.address ?: "",
            onValueChange = { viewModel.updateField("address", it) },
            label = "Address",
            enabled = isEditing,
            singleLine = false
        )

        // Contact Information
        SectionHeader(text = "Contact Information")

        HospitalTextField(
            value = hospitalData?.phoneNumber ?: "",
            onValueChange = { viewModel.updateField("phoneNumber", it) },
            label = "Phone Number",
            enabled = isEditing
        )

        HospitalTextField(
            value = hospitalData?.email ?: "",
            onValueChange = { viewModel.updateField("email", it) },
            label = "Email Address",
            enabled = isEditing
        )

        HospitalTextField(
            value = hospitalData?.website ?: "",
            onValueChange = { viewModel.updateField("website", it) },
            label = "Website",
            enabled = isEditing
        )

        HospitalTextField(
            value = hospitalData?.emergencyContact ?: "",
            onValueChange = { viewModel.updateField("emergencyContact", it) },
            label = "Emergency Contact",
            enabled = isEditing
        )

        // Hospital Services Section
        SectionHeader(text = "Hospital Services")

        HospitalTextField(
            value = hospitalData?.specializations ?: "",
            onValueChange = { viewModel.updateField("specializations", it) },
            label = "Specializations",
            enabled = isEditing,
            singleLine = false
        )

        HospitalTextField(
            value = hospitalData?.organTransplantTypes ?: "",
            onValueChange = { viewModel.updateField("organTransplantTypes", it) },
            label = "Organ Transplant Types",
            enabled = isEditing,
            singleLine = false
        )

        HospitalTextField(
            value = hospitalData?.transplantTeam ?: "",
            onValueChange = { viewModel.updateField("transplantTeam", it) },
            label = "Transplant Team",
            enabled = isEditing,
            singleLine = false
        )

        // Facility Information Section
        SectionHeader(text = "Facility Information")

        HospitalTextField(
            value = hospitalData?.bedsAvailable ?: "",
            onValueChange = { viewModel.updateField("bedsAvailable", it) },
            label = "Beds Available",
            enabled = isEditing
        )

        HospitalTextField(
            value = hospitalData?.icuFacilities ?: "",
            onValueChange = { viewModel.updateField("icuFacilities", it) },
            label = "ICU Facilities",
            enabled = isEditing,
            singleLine = false
        )

        HospitalTextField(
            value = hospitalData?.operatingRooms ?: "",
            onValueChange = { viewModel.updateField("operatingRooms", it) },
            label = "Operating Rooms",
            enabled = isEditing
        )

        // Location Section
        SectionHeader(text = "Location Details")

        // Current coordinates display
        HospitalTextField(
            value = hospitalData?.latitude ?: "",
            onValueChange = { /* Read-only field */ },
            label = "Latitude",
            enabled = false
        )

        HospitalTextField(
            value = hospitalData?.longitude ?: "",
            onValueChange = { /* Read-only field */ },
            label = "Longitude",
            enabled = false
        )

        // Fetch Location Button
        Button(
            onClick = {
                viewModel.showToast("Location button clicked")
                if (hasLocationPermission) {
                    viewModel.showToast("Fetching location...")
                    fetchLocation(fusedLocationClient, context, viewModel)
                } else {
                    viewModel.showToast("Requesting location permission...")
                    locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            // Allow location fetching even when not in edit mode
            enabled = true
        ) {
            Text("Fetch Current Location")
        }

        HospitalTextField(
            value = hospitalData?.currentLocation ?: "",
            onValueChange = { viewModel.updateField("currentLocation", it) },
            label = "Current Location (Address)",
            enabled = isEditing
        )

        // Certification Section
        SectionHeader(text = "Certifications & Licenses")

        HospitalTextField(
            value = hospitalData?.certifications ?: "",
            onValueChange = { viewModel.updateField("certifications", it) },
            label = "Certifications",
            enabled = isEditing,
            singleLine = false
        )

        HospitalTextField(
            value = hospitalData?.accreditations ?: "",
            onValueChange = { viewModel.updateField("accreditations", it) },
            label = "Accreditations",
            enabled = isEditing,
            singleLine = false
        )

        Button(
            onClick = { certificateImageLauncher.launch("*/*") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Upload Certificates")
        }

        // Action Buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(
                onClick = {
                    if (isEditing) {
                        viewModel.saveHospitalData()
                    }
                    isEditing = !isEditing
                },
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            ) {
                Text(if (isEditing) "Submit" else "Edit")
            }

            if (isEditing) {
                Button(
                    onClick = { isEditing = false },
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Cancel")
                }
            }
        }
    }
}

@SuppressLint("MissingPermission")
fun fetchLocation(
    fusedLocationClient: FusedLocationProviderClient,
    context: Context,
    viewModel: HospitalHomeViewModel
) {
    try {
        viewModel.showToast("Getting location...")
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    viewModel.showToast("Location obtained: ${location.latitude}, ${location.longitude}")
                    // Update ViewModel with the location data
                    viewModel.updateField("latitude", location.latitude.toString())
                    viewModel.updateField("longitude", location.longitude.toString())
                } else {
                    viewModel.showToast("Location is null. Please ensure location services are enabled.")
                    // Try to request location updates as fallback
                    requestLocationUpdates(fusedLocationClient, context, viewModel)
                }
            }
            .addOnFailureListener { e ->
                viewModel.showToast("Failed to get location: ${e.message}")
            }
    } catch (e: Exception) {
        viewModel.showToast("Error in location fetch: ${e.message}")
    }
}

@SuppressLint("MissingPermission")
private fun requestLocationUpdates(
    fusedLocationClient: FusedLocationProviderClient,
    context: Context,
    viewModel: HospitalHomeViewModel
) {
    try {
        // Create location request
        val locationRequest = com.google.android.gms.location.LocationRequest.create().apply {
            priority = com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY
            interval = 10000
            fastestInterval = 5000
            numUpdates = 1
        }

        // Location callback
        val locationCallback = object : com.google.android.gms.location.LocationCallback() {
            override fun onLocationResult(locationResult: com.google.android.gms.location.LocationResult) {
                super.onLocationResult(locationResult)
                locationResult.lastLocation?.let { location ->
                    viewModel.showToast("Location update received: ${location.latitude}, ${location.longitude}")
                    viewModel.updateField("latitude", location.latitude.toString())
                    viewModel.updateField("longitude", location.longitude.toString())
                    // Remove updates after getting location
                    fusedLocationClient.removeLocationUpdates(this)
                }
            }
        }

        // Request location updates
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            context.mainLooper
        )

        viewModel.showToast("Requested location updates")
    } catch (e: Exception) {
        viewModel.showToast("Failed to request location updates: ${e.message}")
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
private fun HospitalTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    enabled: Boolean,
    singleLine: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        enabled = enabled,
        singleLine = singleLine,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    )
}
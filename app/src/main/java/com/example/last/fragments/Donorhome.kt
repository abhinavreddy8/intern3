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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.last.fragments.DonorHomeViewModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

@Composable
fun Donorhome(
    viewModel: DonorHomeViewModel = viewModel()
) {
    var isEditing by remember { mutableStateOf(false) }
    val donorData by viewModel.donorData.collectAsState(initial = null)
    val toastMessage by viewModel.toastMessage.collectAsState()
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

    // Image picker launchers
    val profileImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.uploadImage(it, "profile") }
    }

    val medicalReportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.uploadImage(it, "medical_reports") }
    }

    val surgeryReportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.uploadImage(it, "surgery_reports") }
    }

    LaunchedEffect(Unit) {
        viewModel.fetchDonorData()
        viewModel.fetchProfileImage()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Profile Image Section
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(donorData?.profileImageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = "Profile Image",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        Button(
            onClick = { profileImageLauncher.launch("image/*") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Text("Upload Profile")
        }

        // Personal Information Section
        SectionHeader(text = "Personal Information")

        DonorTextField(
            value = donorData?.fullName ?: "",
            onValueChange = { viewModel.updateField("fullName", it) },
            label = "Full Name",
            enabled = isEditing
        )

        DonorTextField(
            value = donorData?.dob ?: "",
            onValueChange = { viewModel.updateField("dob", it) },
            label = "Date of Birth",
            enabled = isEditing
        )

        // Gender Selection
        var selectedGender by remember { mutableStateOf("") }

        LaunchedEffect(donorData?.gender) {
            donorData?.gender?.let { selectedGender = it }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            listOf("Male", "Female", "Other").forEach { gender ->
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedGender == gender,
                        onClick = {
                            if (isEditing) {
                                selectedGender = gender
                                viewModel.updateField("gender", gender)
                            }
                        },
                        enabled = isEditing
                    )
                    Text(gender)
                }
            }
        }

        // Contact Information
        DonorTextField(
            value = donorData?.contactNumber ?: "",
            onValueChange = { viewModel.updateField("contactNumber", it) },
            label = "Contact Number",
            enabled = isEditing
        )

        DonorTextField(
            value = donorData?.email ?: "",
            onValueChange = { viewModel.updateField("email", it) },
            label = "Email",
            enabled = isEditing
        )

        DonorTextField(
            value = donorData?.address ?: "",
            onValueChange = { viewModel.updateField("address", it) },
            label = "Address",
            enabled = isEditing
        )

        // Medical Information Section
        SectionHeader(text = "Medical Information")

        DonorTextField(
            value = donorData?.bloodGroup ?: "",
            onValueChange = { viewModel.updateField("bloodGroup", it) },
            label = "Blood Group",
            enabled = isEditing
        )

        DonorTextField(
            value = donorData?.organAvailable ?: "",
            onValueChange = { viewModel.updateField("organAvailable", it) },
            label = "Organ(s) Available for Donation",
            enabled = isEditing
        )

        DonorTextField(
            value = donorData?.medicalHistory ?: "",
            onValueChange = { viewModel.updateField("medicalHistory", it) },
            label = "Medical History",
            enabled = isEditing,
            singleLine = false
        )

        DonorTextField(
            value = donorData?.surgeries ?: "",
            onValueChange = { viewModel.updateField("surgeries", it) },
            label = "Previous Surgeries",
            enabled = isEditing,
            singleLine = false
        )

        Button(
            onClick = { surgeryReportLauncher.launch("*/*") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Upload Surgery Reports")
        }

        DonorTextField(
            value = donorData?.medications ?: "",
            onValueChange = { viewModel.updateField("medications", it) },
            label = "Current Medications",
            enabled = isEditing
        )

        DonorTextField(
            value = donorData?.allergies ?: "",
            onValueChange = { viewModel.updateField("allergies", it) },
            label = "Allergies",
            enabled = isEditing
        )

        DonorTextField(
            value = donorData?.lifestyle ?: "",
            onValueChange = { viewModel.updateField("lifestyle", it) },
            label = "Lifestyle",
            enabled = isEditing
        )

        DonorTextField(
            value = donorData?.geneticDisorders ?: "",
            onValueChange = { viewModel.updateField("geneticDisorders", it) },
            label = "Genetic Disorders",
            enabled = isEditing
        )

        Button(
            onClick = { medicalReportLauncher.launch("*/*") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Upload Medical Reports")
        }

        // Emergency Contact Section
        SectionHeader(text = "Emergency Contact")

        DonorTextField(
            value = donorData?.emergencyContact ?: "",
            onValueChange = { viewModel.updateField("emergencyContact", it) },
            label = "Emergency Contact",
            enabled = isEditing
        )

        // Location Section
        SectionHeader(text = "Location Details")

        // Current coordinates display
        DonorTextField(
            value = donorData?.latitude?.toString() ?: "",
            onValueChange = { /* Read-only field */ },
            label = "Latitude",
            enabled = false
        )

        DonorTextField(
            value = donorData?.longitude?.toString() ?: "",
            onValueChange = { /* Read-only field */ },
            label = "Longitude",
            enabled = false
        )

        // Fetch Location Button
        Button(
            onClick = {
                if (hasLocationPermission) {
                    fetchLocation(fusedLocationClient, context, viewModel)
                } else {
                    locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            enabled = true
        ) {
            Text("Fetch Current Location")
        }

        DonorTextField(
            value = donorData?.currentLocation ?: "",
            onValueChange = { viewModel.updateField("currentLocation", it) },
            label = "Current Location (Address)",
            enabled = isEditing
        )

        DonorTextField(
            value = donorData?.preferredLocation ?: "",
            onValueChange = { viewModel.updateField("preferredLocation", it) },
            label = "Preferred Donation Locations",
            enabled = isEditing
        )

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
                        viewModel.saveDonorData()
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
    viewModel: DonorHomeViewModel
) {
    try {
        viewModel.showToast("Getting location...")
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    viewModel.showToast("Location obtained: ${location.latitude}, ${location.longitude}")
                    viewModel.updateField("latitude", location.latitude.toString())
                    viewModel.updateField("longitude", location.longitude.toString())
                } else {
                    viewModel.showToast("Location is null. Please ensure location services are enabled.")
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
    viewModel: DonorHomeViewModel
) {
    try {
        val locationRequest = com.google.android.gms.location.LocationRequest.create().apply {
            priority = com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY
            interval = 10000
            fastestInterval = 5000
            numUpdates = 1
        }

        val locationCallback = object : com.google.android.gms.location.LocationCallback() {
            override fun onLocationResult(locationResult: com.google.android.gms.location.LocationResult) {
                super.onLocationResult(locationResult)
                locationResult.lastLocation?.let { location ->
                    viewModel.showToast("Location update received: ${location.latitude}, ${location.longitude}")
                    viewModel.updateField("latitude", location.latitude.toString())
                    viewModel.updateField("longitude", location.longitude.toString())
                    fusedLocationClient.removeLocationUpdates(this)
                }
            }
        }

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
private fun DonorTextField(
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
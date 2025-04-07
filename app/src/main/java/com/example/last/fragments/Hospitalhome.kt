package com.example.last

import android.net.Uri
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
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.last.fragments.HospitalHomeViewModel

@Composable
fun Hospitalhome(
    viewModel: HospitalHomeViewModel = viewModel()
) {
    var isEditing by remember { mutableStateOf(false) }
    val hospitalData by viewModel.hospitalData.collectAsState(initial = null)
    val context = LocalContext.current

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
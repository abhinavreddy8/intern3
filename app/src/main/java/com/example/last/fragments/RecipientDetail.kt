package com.example.last.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.last.R
import com.example.last.viewmodels.RecipientdetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipientDetailScreen(
    recipientId: String,
    viewModel: RecipientdetailViewModel = viewModel(),
    onBackPressed: () -> Unit
) {
    val recipientDetails by viewModel.recipientDetails.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    LaunchedEffect(recipientId) {
        viewModel.fetchRecipientDetails(recipientId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recipient Details") },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                error != null -> {
                    Text(
                        text = "Error: ${error ?: "Unknown error occurred"}",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp)
                    )
                }
                recipientDetails != null -> {
                    RecipientDetailsContent(
                        recipientDetails = recipientDetails!!,
                        viewModel = viewModel
                    )
                }
                else -> {
                    Text(
                        text = "No recipient information available",
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun RecipientDetailsContent(
    recipientDetails: RecipientdetailViewModel.RecipientDetails,
    viewModel: RecipientdetailViewModel
) {
    val context = LocalContext.current
    val showContactDialog = remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Profile Image
        Box(
            modifier = Modifier
                .size(140.dp)
                .padding(bottom = 16.dp)
        ) {
            val painter = if (!recipientDetails.profileImageUrl.isNullOrEmpty()) {
                rememberAsyncImagePainter(
                    ImageRequest.Builder(context)
                        .data(recipientDetails.profileImageUrl)
                        .crossfade(true)
                        .build()
                )
            } else {
                painterResource(id = R.drawable.recipient)
            }

            Image(
                painter = painter,
                contentDescription = "Recipient profile",
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        }

        // Recipient Name
        Text(
            text = recipientDetails.fullName,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        // Medical Condition Badge
        Card(
            modifier = Modifier
                .padding(vertical = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = recipientDetails.medicalCondition ?: "Medical condition",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Basic Info Section
        InfoSection(title = "Personal Information") {
            InfoRow(label = "Age", value = recipientDetails.age ?: "Not specified")
            InfoRow(label = "Gender", value = recipientDetails.gender ?: "Not specified")

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "Location",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = recipientDetails.currentLocation ?: "Location not specified",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        // Medical Information Section
        InfoSection(title = "Medical Information") {
            InfoRow(label = "Required Organ", value = recipientDetails.requiredOrgan ?: "Not specified")
            InfoRow(label = "Blood Type Needed", value = recipientDetails.bloodTypeNeeded ?: "Not specified")
            InfoRow(label = "Medical History", value = recipientDetails.medicalHistory ?: "No medical history provided")
            InfoRow(label = "Current Medications", value = recipientDetails.medications ?: "None")
            InfoRow(label = "Allergies", value = recipientDetails.allergies ?: "None")
            InfoRow(label = "Urgency Level", value = recipientDetails.urgencyLevel ?: "Not specified")
        }

        // Hospital Information
        InfoSection(title = "Hospital Information") {
            InfoRow(label = "Treating Hospital", value = recipientDetails.hospitalName ?: "Not specified")
            InfoRow(label = "Doctor in Charge", value = recipientDetails.doctorName ?: "Not specified")
            InfoRow(label = "Hospital Contact", value = recipientDetails.hospitalContact ?: "Not available")
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Contact Button (for doctors/hospitals)
        if (viewModel.isDoctorOrHospital()) {
            Button(
                onClick = { showContactDialog.value = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(28.dp)
            ) {
                Text(
                    text = "Contact Recipient",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Contact Dialog
    if (showContactDialog.value) {
        AlertDialog(
            onDismissRequest = { showContactDialog.value = false },
            title = { Text("Contact Recipient") },
            text = {
                Column {
                    Text("You can contact the recipient directly:")
                    Spacer(modifier = Modifier.height(8.dp))
                    recipientDetails.contactNumber?.let {
                        ContactInfoRow(
                            icon = Icons.Default.Call,
                            label = "Phone:",
                            value = it
                        )
                    }
                    recipientDetails.email?.let {
                        ContactInfoRow(
                            icon = Icons.Default.Email,
                            label = "Email:",
                            value = it
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showContactDialog.value = false }
                ) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
private fun ContactInfoRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}
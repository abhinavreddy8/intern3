// DonorDetailScreen.kt
package com.example.last.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.last.R
import com.example.last.viewmodels.DonorDetailViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DonorDetailScreen(
    donorId: String,
    viewModel: DonorDetailViewModel = viewModel(),
    onBackPressed: () -> Unit
) {
    val donorDetails by viewModel.donorDetails.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val contactRequestStatus by viewModel.contactRequestStatus.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    LaunchedEffect(donorId) {
        viewModel.fetchDonorDetails(donorId)
    }

    // Show snackbar for status messages
    LaunchedEffect(statusMessage) {
        statusMessage?.let { message ->
            scope.launch {
                snackbarHostState.showSnackbar(message)
                viewModel.clearStatusMessage()
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Donor Details") },
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
                donorDetails != null -> {
                    DonorDetailsContent(
                        donorDetails = donorDetails!!,
                        viewModel = viewModel,
                        contactRequestStatus = contactRequestStatus
                    )
                }
                else -> {
                    Text(
                        text = "No donor information available",
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
private fun DonorDetailsContent(
    donorDetails: DonorDetailViewModel.DonorDetails,
    viewModel: DonorDetailViewModel,
    contactRequestStatus: String?
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
            val painter = if (!donorDetails.profileImageUrl.isNullOrEmpty()) {
                rememberAsyncImagePainter(
                    ImageRequest.Builder(context)
                        .data(donorDetails.profileImageUrl)
                        .crossfade(true)
                        .build()
                )
            } else {
                painterResource(id = R.drawable.donor)
            }

            Image(
                painter = painter,
                contentDescription = "Donor profile",
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        }

        // Donor Name
        Text(
            text = donorDetails.fullName,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        // Blood Type Badge
        Card(
            modifier = Modifier
                .padding(vertical = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = donorDetails.bloodGroup,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Basic Info Section
        InfoSection(title = "Personal Information") {
            InfoRow(label = "Age", value = donorDetails.age ?: "Not specified")
            InfoRow(label = "Gender", value = donorDetails.gender ?: "Not specified")

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
                    text = donorDetails.currentLocation ?: "Location not specified",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        // Medical Information Section
        InfoSection(title = "Medical Information") {
            InfoRow(label = "Organ Donating", value = donorDetails.organAvailable ?: "Not specified")
            InfoRow(label = "Medical History", value = donorDetails.medicalHistory ?: "No medical history provided")
            InfoRow(label = "Previous Surgeries", value = donorDetails.surgeries ?: "None")
            InfoRow(label = "Current Medications", value = donorDetails.medications ?: "None")
            InfoRow(label = "Allergies", value = donorDetails.allergies ?: "None")
            InfoRow(label = "Genetic Disorders", value = donorDetails.geneticDisorders ?: "None")
        }

        // Location Preferences
        InfoSection(title = "Donation Preferences") {
            InfoRow(label = "Preferred Locations", value = donorDetails.preferredLocation ?: "Not specified")
            InfoRow(label = "Lifestyle", value = donorDetails.lifestyle ?: "Not specified")
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Contact Button - Show different states based on contactRequestStatus
        when (contactRequestStatus) {
            "pending" -> {
                OutlinedButton(
                    onClick = { },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text(
                        text = "Request Pending",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
            "approved" -> {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Show contact information if request was approved
                    donorDetails.contactNumber?.let { phone ->
                        Button(
                            onClick = { /* Handle call */ },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Call,
                                contentDescription = "Call",
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Call: $phone",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    donorDetails.email?.let { email ->
                        Button(
                            onClick = { /* Handle email */ },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            ),
                            shape = RoundedCornerShape(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = "Email",
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Email: $email",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }
            else -> {
                // No request or request was rejected/expired
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
                        text = "Request Contact",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }

    // Contact Request Dialog
    if (showContactDialog.value) {
        AlertDialog(
            onDismissRequest = { showContactDialog.value = false },
            title = { Text("Request Contact Information") },
            text = {
                Text("Do you want to request contact information from this donor? A notification will be sent to the donor.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.sendContactRequest(donorDetails.id)
                        showContactDialog.value = false
                    }
                ) {
                    Text("Send Request")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showContactDialog.value = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun InfoSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                content()
            }
        }
    }
}

@Composable
fun InfoRow(
    label: String,
    value: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 2.dp, bottom = 6.dp)
        )

        Divider(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            thickness = 0.5.dp
        )
    }
}
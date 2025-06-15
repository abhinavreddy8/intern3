package com.example.last.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.last.DonorRoutes
import com.example.last.viewmodels.HospitalSearchViewModel
import android.util.Log
import androidx.compose.foundation.background

@Composable
fun HospitalSearchScreen(
    viewModel: HospitalSearchViewModel = viewModel(),
    navigateToDonorDetail: (String) -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }

    // Permission launcher for location
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.findNearbyDonors()
        } else {
            viewModel.showToast("Location permission denied")
        }
    }

    // Observe toast messages
    viewModel.toastMessage?.let { message ->
        LaunchedEffect(message) {
            snackbarHostState.showSnackbar(message)
            viewModel.toastMessage = null
        }
    }

    // Initialize context when screen first loads
    LaunchedEffect(Unit) {
        viewModel.setContext(context)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
        ) {
            Text(
                text = "Search Donors",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    color = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            OutlinedTextField(
                value = searchQuery,
                onValueChange = {
                    searchQuery = it
                    viewModel.resetNearbySearch()
                    Log.d("HospitalSearchScreen", "Search query updated: $searchQuery, nearby search reset")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .shadow(4.dp, RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surface),
                placeholder = {
                    Text(
                        "Search by name, blood type or location",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                shape = RoundedCornerShape(24.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        Log.d("HospitalSearchScreen", "Find Nearby Donors button clicked")
                        permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = "Nearby",
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Text(
                        "Find Nearby Donors",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                if (viewModel.isNearbySearchActive) {
                    Button(
                        onClick = {
                            viewModel.resetNearbySearch()
                            Log.d("HospitalSearchScreen", "Show All Donors button clicked")
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        )
                    ) {
                        Text(
                            "Show All Donors",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Nearby Donors Count: ${viewModel.nearbyDonors.size}",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (viewModel.isNearbySearchActive) {
                Text(
                    text = "Showing nearby donors (10km radius)",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            when {
                viewModel.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 4.dp
                        )
                    }
                }
                viewModel.error != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Error: ${viewModel.error}",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.errorContainer,
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(16.dp)
                        )
                    }
                }
                else -> {
                    val displayedDonors = if (viewModel.isNearbySearchActive) {
                        viewModel.nearbyDonors.filter { donor ->
                            if (searchQuery.isBlank()) true
                            else {
                                val query = searchQuery.lowercase()
                                donor.name.lowercase().contains(query) ||
                                        donor.bloodType.lowercase().contains(query) ||
                                        donor.location.lowercase().contains(query) ||
                                        donor.organDonating.lowercase().contains(query)
                            }
                        }.also {
                            Log.d("HospitalSearchScreen", "Displaying ${it.size} nearby donors, isNearbySearchActive=${viewModel.isNearbySearchActive}")
                        }
                    } else {
                        viewModel.donors.filter { donor ->
                            if (searchQuery.isBlank()) true
                            else {
                                val query = searchQuery.lowercase()
                                donor.name.lowercase().contains(query) ||
                                        donor.bloodType.lowercase().contains(query) ||
                                        donor.location.lowercase().contains(query) ||
                                        donor.organDonating.lowercase().contains(query)
                            }
                        }.also {
                            Log.d("HospitalSearchScreen", "Displaying ${it.size} all donors, isNearbySearchActive=${viewModel.isNearbySearchActive}")
                        }
                    }

                    if (displayedDonors.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (viewModel.isNearbySearchActive) {
                                    "No nearby donors found within 10km radius"
                                } else if (searchQuery.isNotBlank()) {
                                    "No donors found matching your search"
                                } else {
                                    "No donors available"
                                },
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Log.d("HospitalSearchScreen", "Displayed donors empty, message shown")
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(displayedDonors) { donor ->
                                DonorCard(
                                    donor = donor,
                                    onClick = { navigateToDonorDetail(donor.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HospitalSearchDonors(navController: NavController) {
    val viewModel: HospitalSearchViewModel = viewModel()

    LaunchedEffect(Unit) {
        viewModel.initializeStorage(
            appId = "1:924036427672:android:75e24d5ebe6dd3f35cc5ed",
            apiKey = "AIzaSyBLVNr5M0sHOTtGpqBvn8ula-knHx0vxvc",
            projectId = "socialmedia-b9148",
            bucketUrl = "socialmedia-b9148.appspot.com"
        )
    }

    HospitalSearchScreen(
        viewModel = viewModel,
        navigateToDonorDetail = { donorId ->
            navController.navigate(DonorRoutes.DonorDetail.createRoute(donorId))
        }
    )
}
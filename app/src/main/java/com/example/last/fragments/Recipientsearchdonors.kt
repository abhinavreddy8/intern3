package com.example.last.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.last.R
import com.example.last.RecipientScreen
import com.example.last.viewmodels.DonorData
import com.example.last.viewmodels.RecipientSearchViewModel

@Composable
fun RecipientSearchScreen(
    viewModel: RecipientSearchViewModel = viewModel(),
    navigateToDonorDetail: (String) -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.setContext(context)
    }

    LaunchedEffect(viewModel.toastMessage) {
        viewModel.toastMessage?.let { message ->
            android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
            viewModel.showToast("") // Clear the message after showing
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Text(
            text = "Find Donors",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                color = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Enhanced Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
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

        Spacer(modifier = Modifier.height(16.dp))

        // Enhanced Buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(
                onClick = { viewModel.findNearbyDonors() },
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
                    .height(48.dp),
                enabled = !viewModel.isLoading,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text("Find Donors", fontSize = 14.sp)
            }

            Button(
                onClick = { viewModel.resetNearbySearch() },
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp)
                    .height(48.dp),
                enabled = viewModel.isNearbySearchActive && !viewModel.isLoading,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary
                )
            ) {
                Text("Show All Donors", fontSize = 14.sp)
            }
        }

        // Donors List
        if (viewModel.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 4.dp
                )
            }
        } else if (viewModel.error != null) {
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
        } else {
            val donorsToShow = if (viewModel.isNearbySearchActive) {
                viewModel.nearbyDonors
            } else {
                viewModel.donors.filter { donor ->
                    val query = searchQuery.lowercase()
                    donor.name.lowercase().contains(query) ||
                            donor.bloodType.lowercase().contains(query) ||
                            donor.location.lowercase().contains(query) ||
                            donor.organDonating.lowercase().contains(query)
                }
            }

            if (donorsToShow.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (viewModel.isNearbySearchActive) {
                            "No nearby donors found"
                        } else {
                            "No donors found"
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(donorsToShow) { donor ->
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

@Composable
fun Recipientsearchdonors(navController: NavController) {
    val viewModel: RecipientSearchViewModel = viewModel()

    LaunchedEffect(Unit) {
        viewModel.initializeStorage(
            appId = "1:924036427672:android:75e24d5ebe6dd3f35cc5ed",
            apiKey = "AIzaSyBLVNr5M0sHOTtGpqBvn8ula-knHx0vxvc",
            projectId = "socialmedia-b9148",
            bucketUrl = "socialmedia-b9148.appspot.com"
        )
    }

    RecipientSearchScreen(
        viewModel = viewModel,
        navigateToDonorDetail = { donorId ->
            navController.navigate(RecipientScreen.RecipientDonorDetail.createRoute(donorId))
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DonorCard(
    donor: DonorData,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Donor Image
            val painter = if (donor.imageUrl.isNotEmpty()) {
                rememberAsyncImagePainter(
                    ImageRequest.Builder(LocalContext.current)
                        .data(donor.imageUrl)
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
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = donor.name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Blood Type: ${donor.bloodType}",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Organ Donating: ${donor.organDonating}",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = donor.location,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                donor.distance?.let { distance ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Distance: ${String.format("%.2f", distance)} km",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Right Side - Chips and Call Button
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(start = 8.dp)
            ) {
                ElevatedAssistChip(
                    onClick = { },
                    label = {
                        Text(
                            text = donor.bloodType,
                            style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp)
                        )
                    },
                    colors = AssistChipDefaults.elevatedAssistChipColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        labelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )

                donor.requestStatus?.let { status ->
                    Spacer(modifier = Modifier.height(8.dp))

                    val (chipColor, chipText) = when (status) {
                        "pending" -> Pair(
                            MaterialTheme.colorScheme.secondaryContainer,
                            "Pending"
                        )
                        "accepted" -> Pair(
                            MaterialTheme.colorScheme.primaryContainer,
                            "Accepted"
                        )
                        "rejected" -> Pair(
                            MaterialTheme.colorScheme.errorContainer,
                            "Declined"
                        )
                        else -> Pair(
                            MaterialTheme.colorScheme.surfaceVariant,
                            status.replaceFirstChar { it.uppercase() }
                        )
                    }

                    SuggestionChip(
                        onClick = { },
                        label = {
                            Text(
                                text = chipText,
                                style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp)
                            )
                        },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = chipColor,
                            labelColor = when (status) {
                                "pending" -> MaterialTheme.colorScheme.onSecondaryContainer
                                "accepted" -> MaterialTheme.colorScheme.onPrimaryContainer
                                "rejected" -> MaterialTheme.colorScheme.error
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    )

                    if (status == "accepted") {
                        Spacer(modifier = Modifier.height(8.dp))
                        IconButton(
                            onClick = { /* Call functionality here */ },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Call,
                                contentDescription = "Call donor",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}
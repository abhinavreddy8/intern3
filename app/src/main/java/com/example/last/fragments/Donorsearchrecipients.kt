// DonorSearchScreen.kt
package com.example.last.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.last.R
import com.example.last.Screen
import com.example.last.viewmodels.Donorsearchviewmodel
import com.example.last.viewmodels.RecipientData

@Composable
fun DonorSearchScreen(
    viewModel: Donorsearchviewmodel = viewModel(),
    navigateToRecipientDetail: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Find Recipients",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            placeholder = { Text("Search by name, blood type or organ") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            shape = RoundedCornerShape(24.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Recipients list
        if (viewModel.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (viewModel.error != null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Error: ${viewModel.error}",
                    color = MaterialTheme.colorScheme.error
                )
            }
        } else {
            val filteredRecipients = viewModel.recipients.filter { recipient ->
                val query = searchQuery.lowercase()
                recipient.name.lowercase().contains(query) ||
                        recipient.bloodType.lowercase().contains(query) ||
                        recipient.location.lowercase().contains(query)

            }

            if (filteredRecipients.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No recipients found")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredRecipients) { recipient ->
                        RecipientCard(
                            recipient = recipient,
                            onClick = { navigateToRecipientDetail(recipient.id) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipientCard(
    recipient: RecipientData,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Recipient image
            val painter = if (recipient.imageUrl.isNotEmpty()) {
                rememberAsyncImagePainter(
                    ImageRequest.Builder(LocalContext.current)
                        .data(recipient.imageUrl)
                        .crossfade(true)
                        .build()
                )
            } else {
                painterResource(id = R.drawable.donor)
            }

            Image(
                painter = painter,
                contentDescription = "Recipient profile",
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = recipient.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Blood Type: ${recipient.bloodType}",
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Organ Needed: ${recipient.organNeeded}",
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = recipient.location,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            ElevatedAssistChip(
                onClick = { },
                label = {
                    Text(
                        text = recipient.bloodType,
                        style = MaterialTheme.typography.labelMedium
                    )
                },
                colors = AssistChipDefaults.elevatedAssistChipColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    labelColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}


// Add this function to use the ViewModel and UI in your navigation
@Composable
fun DonorSearchRecipientScreen(navController: NavController) {
    val viewModel: Donorsearchviewmodel = viewModel()

    LaunchedEffect(Unit) {
        viewModel.initializeStorage(
            appId = "1:924036427672:android:75e24d5ebe6dd3f35cc5ed",
            apiKey = "AIzaSyBLVNr5M0sHOTtGpqBvn8ula-knHx0vxvc",
            projectId = "socialmedia-b9148",
            bucketUrl = "socialmedia-b9148.appspot.com"
        )
    }

    DonorSearchScreen(
        viewModel = viewModel,
        navigateToRecipientDetail = { recipientId ->
            navController.navigate(Screen.RecipientDetail.createRoute(recipientId))
        }
    )
}

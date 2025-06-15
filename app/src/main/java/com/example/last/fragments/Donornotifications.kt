package com.example.last.screens

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.last.viewmodels.DonorNotificationViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DonorNotificationScreen(
    viewModel: DonorNotificationViewModel = viewModel(),
    navigateBack: () -> Unit = {}
) {
    val requests by viewModel.requests.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()

    // Track if any notifications are shown
    val hasNotifications = requests.isNotEmpty()

    // Auto-dismiss status messages after a delay
    LaunchedEffect(statusMessage) {
        if (statusMessage != null) {
            delay(3000)
            viewModel.clearStatusMessage()
        }
    }

    // Trigger fetch when screen starts
    LaunchedEffect(Unit) {
        Log.d("DonorNotificationScreen", "Screen started, triggering fetchContactRequests")
        viewModel.fetchContactRequests()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notifications",
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text("Contact Requests")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.fetchContactRequests() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                error != null -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = error ?: "Unknown error",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { viewModel.fetchContactRequests() }) {
                            Text("Retry")
                        }
                    }
                }
                !hasNotifications -> {
                    EmptyNotificationsPlaceholder()
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        items(requests) { request ->
                            ContactRequestItem(
                                request = request,
                                onAccept = { viewModel.acceptRequest(request) },
                                onReject = { viewModel.rejectRequest(request) }
                            )
                        }
                    }
                }
            }

            // Status message snackbar
            AnimatedVisibility(
                visible = statusMessage != null,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                statusMessage?.let {
                    Snackbar(
                        modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        Text(statusMessage ?: "")
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyNotificationsPlaceholder() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Notifications,
            contentDescription = "No notifications",
            modifier = Modifier
                .size(64.dp)
                .padding(bottom = 16.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
        )
        Text(
            text = "No pending contact requests",
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
        Spacer(height = 8.dp)
        Text(
            text = "When recipients request your contact information, they will appear here",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactRequestItem(
    request: DonorNotificationViewModel.ContactRequest,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy 'at' h:mm a", Locale.US)
    val formattedDate = dateFormat.format(Date(request.timestamp))

    val backgroundColor = when (request.status) {
        "accepted" -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        "rejected" -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        else -> MaterialTheme.colorScheme.surface
    }

    Card(
        modifier = Modifier
            .fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = request.recipientName,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                val statusChipColor = when (request.status) {
                    "accepted" -> MaterialTheme.colorScheme.primary
                    "rejected" -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.secondary
                }

                val statusText = when (request.status) {
                    "pending" -> "Pending"
                    "accepted" -> "Accepted"
                    "rejected" -> "Declined"
                    else -> request.status.replaceFirstChar { it.uppercase() }
                }

                if (request.status != "pending") {
                    SuggestionChip(
                        onClick = { },
                        label = { Text(statusText) },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = statusChipColor.copy(alpha = 0.2f),
                            labelColor = statusChipColor
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Has requested your contact information",
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = formattedDate,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )

            if (request.status == "pending") {
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = onReject,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Text("Decline")
                    }

                    Button(
                        onClick = onAccept,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("Accept")
                    }
                }
            }
        }
    }
}

@Composable
fun Spacer(height: Dp) {
    Spacer(modifier = Modifier.height(height))
}
package com.example.last.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.last.viewmodels.HospitalRequestHandlerViewModel
import com.example.last.viewmodels.RequestData
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HospitalRequestHandlerScreen(
    viewModel: HospitalRequestHandlerViewModel = viewModel()
) {
    val requests by viewModel.requests.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.fetchRequests()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Recipient Requests",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        when {
            viewModel.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            viewModel.error != null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Error: ${viewModel.error}",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            requests.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No recipient requests to process.")
                }
            }
            else -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(requests) { request ->
                        HospitalRequestCard(
                            request = request,
                            onAccept = { response ->
                                viewModel.respondToRequest(request.requestId, "accepted", response)
                            },
                            onReject = { response ->
                                viewModel.respondToRequest(request.requestId, "rejected", response)
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HospitalRequestCard(
    request: RequestData,
    onAccept: (String) -> Unit,
    onReject: (String) -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    val formattedDate = dateFormat.format(Date(request.timestamp))

    var expanded by remember { mutableStateOf(false) }
    var responseDialogShown by remember { mutableStateOf(false) }
    var responseType by remember { mutableStateOf("") }
    var responseText by remember { mutableStateOf("") }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header with recipient name and timestamp
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "From: ${request.recipientName}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Status chip
            Box(
                modifier = Modifier
                    .padding(vertical = 4.dp)
            ) {
                val (color, textColor) = when (request.status) {
                    "pending" -> MaterialTheme.colorScheme.tertiary to MaterialTheme.colorScheme.onTertiary
                    "accepted" -> MaterialTheme.colorScheme.primary to MaterialTheme.colorScheme.onPrimary
                    "rejected" -> MaterialTheme.colorScheme.error to MaterialTheme.colorScheme.onError
                    else -> MaterialTheme.colorScheme.tertiary to MaterialTheme.colorScheme.onTertiary
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = color,
                    modifier = Modifier.wrapContentWidth()
                ) {
                    Text(
                        text = request.status.capitalize(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = textColor,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Description
            Text(
                text = "Request Description:",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )

            if (expanded) {
                Text(
                    text = request.description,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            } else {
                Text(
                    text = request.description,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            // Response display if already responded
            if (request.response != null && request.status != "pending") {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Your Response:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )

                Text(
                    text = request.response,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action buttons
            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (request.status == "pending") {
                    // Only show accept/reject buttons for pending requests
                    Button(
                        onClick = {
                            responseType = "accepted"
                            responseDialogShown = true
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Accept")
                    }

                    Button(
                        onClick = {
                            responseType = "rejected"
                            responseDialogShown = true
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Reject")
                    }
                } else {
                    // Show a "Show more/less" button for already processed requests
                    Spacer(modifier = Modifier.weight(1f)) // Push the button to the end
                    TextButton(
                        onClick = { expanded = !expanded }
                    ) {
                        Text(text = if (expanded) "Show Less" else "Show More")
                    }
                }
            }


            // Response dialog
            if (responseDialogShown) {
                AlertDialog(
                    onDismissRequest = { responseDialogShown = false },
                    title = {
                        Text(
                            text = if (responseType == "accepted") "Accept Request" else "Reject Request"
                        )
                    },
                    text = {
                        Column {
                            Text("Please provide a response to the recipient:")
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = responseText,
                                onValueChange = { responseText = it },
                                label = { Text("Response") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 3
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (responseText.isNotEmpty()) {
                                    when (responseType) {
                                        "accepted" -> onAccept(responseText)
                                        "rejected" -> onReject(responseText)
                                    }
                                    responseDialogShown = false
                                    responseText = ""
                                }
                            }
                        ) {
                            Text("Submit")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { responseDialogShown = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}
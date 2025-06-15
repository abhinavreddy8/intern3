package com.example.last.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.last.viewmodels.NotificationData
import com.example.last.viewmodels.RecipientNotificationsViewModel
import java.text.SimpleDateFormat
import java.util.*
import android.util.Log
import com.example.last.viewmodels.NotificationType

class Recipientnotifications : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                MaterialTheme {
                    RecipientNotificationsScreen()
                }
            }
        }
    }
}

@Composable
fun NotificationCard(notification: NotificationData) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    val formattedDate = dateFormat.format(Date(notification.timestamp))

    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Status indicator at the top
            Box(
                modifier = Modifier
                    .padding(bottom = 8.dp)
            ) {
                val (color, textColor) = when (notification.status.lowercase()) {
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
                        text = notification.status.uppercase(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = textColor,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }

            // Notification message
            Text(
                text = if (notification.type == NotificationType.HOSPITAL) {
                    "${notification.hospitalName} has ${notification.status.lowercase()} your request."
                } else {
                    "You have a new donor request with status ${notification.status.lowercase()}."
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Timestamp
            Text(
                text = "At: $formattedDate",
                style = MaterialTheme.typography.bodySmall
            )

            // Show response if available
            if (notification.responseMessage != null) {
                if (expanded) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Response Message:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = notification.responseMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                } else {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = notification.responseMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Show more/less button for responses
                TextButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(text = if (expanded) "Show Less" else "Show More")
                }
            }
        }
    }
}

@Composable
fun RecipientNotificationsScreen(viewModel: RecipientNotificationsViewModel = viewModel()) {
    val notifications by viewModel.notifications.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.fetchNotifications()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "My Notifications",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        when {
            viewModel.isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            viewModel.error != null -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Error: ${viewModel.error}",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            notifications.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No notifications yet.")
                }
            }
            else -> {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(notifications) { notification ->
                        NotificationCard(notification)
                    }
                }
            }
        }
    }
}

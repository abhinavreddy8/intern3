package com.example.last.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.last.R
import com.example.last.viewmodels.HospitalDetailViewModel
import com.example.last.viewmodels.ReviewData
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HospitalDetailScreen(
    hospitalId: String,
    viewModel: HospitalDetailViewModel = viewModel(),
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    var showRequestDialog by remember { mutableStateOf(false) }
    var requestDescription by remember { mutableStateOf("") }
    var showConfirmationSnackbar by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) }
    var showAddReviewDialog by remember { mutableStateOf(false) }
    var reviewRating by remember { mutableStateOf(0f) }
    var reviewComment by remember { mutableStateOf("") }
    var showReviewConfirmation by remember { mutableStateOf(false) }

    LaunchedEffect(hospitalId) {
        viewModel.fetchHospitalDetails(hospitalId)
    }

    // Classify reviews when available
    LaunchedEffect(viewModel.reviews) {
        if (viewModel.reviews.isNotEmpty()) {
            viewModel.classifyReviews()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Hospital Details") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            painter = painterResource(id = R.drawable.hospital),
                            contentDescription = "Back"
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
        ) {
            when {
                viewModel.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                viewModel.error != null -> {
                    Text(
                        text = "Error: ${viewModel.error}",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp)
                    )
                }
                viewModel.hospital != null -> {
                    val hospital = viewModel.hospital!!

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp)
                        ) {
                            // Hospital Image
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .padding(bottom = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = if (hospital.imageUrl.isNotEmpty()) {
                                        rememberAsyncImagePainter(
                                            ImageRequest.Builder(context)
                                                .data(hospital.imageUrl)
                                                .build()
                                        )
                                    } else {
                                        painterResource(id = R.drawable.hospital)
                                    },
                                    contentDescription = "Hospital Image",
                                    modifier = Modifier
                                        .size(160.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            }

                            // Hospital Information
                            Text(
                                text = hospital.name,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Text(
                                        text = "Details",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )

                                    Text(
                                        text = "Address: ${hospital.address}",
                                        style = MaterialTheme.typography.bodyLarge,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )

                                    Text(
                                        text = "Contact: ${hospital.contact}",
                                        style = MaterialTheme.typography.bodyLarge,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )

                                    Text(
                                        text = "Specialties: ${hospital.specialties}",
                                        style = MaterialTheme.typography.bodyLarge,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                }
                            }

                            // Reviews Section
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Text(
                                        text = "Reviews",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )

                                    // Tabs for Positive and Negative Reviews
                                    TabRow(selectedTabIndex = selectedTab) {
                                        Tab(
                                            selected = selectedTab == 0,
                                            onClick = { selectedTab = 0 },
                                            text = {
                                                Text(
                                                    "Positive (${viewModel.positiveReviews.size})",
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        )
                                        Tab(
                                            selected = selectedTab == 1,
                                            onClick = { selectedTab = 1 },
                                            text = {
                                                Text(
                                                    "Negative (${viewModel.negativeReviews.size})",
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Add Review Button
                                    Button(
                                        onClick = { showAddReviewDialog = true },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    ) {
                                        Text("Add Your Review")
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Show different reviews based on selected tab
                                    val reviewsToShow = if (selectedTab == 0) {
                                        viewModel.positiveReviews
                                    } else {
                                        viewModel.negativeReviews
                                    }

                                    if (reviewsToShow.isEmpty()) {
                                        Text(
                                            text = "No ${if (selectedTab == 0) "positive" else "negative"} reviews yet",
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    } else {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(max = 300.dp)
                                        ) {
                                            reviewsToShow.forEach { review ->
                                                ReviewItem(
                                                    review = review,
                                                    canDelete = review.userId == FirebaseAuth.getInstance().currentUser?.uid,
                                                    onDeleteClick = { viewModel.deleteReview(review.id) }
                                                )
                                                Divider()
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            // Send Request Button
                            Button(
                                onClick = { showRequestDialog = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(24.dp)
                            ) {
                                Text(
                                    text = "Send Request",
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                        }
                    }

                    // Request Dialog
                    if (showRequestDialog) {
                        AlertDialog(
                            onDismissRequest = { showRequestDialog = false },
                            title = { Text("Send Request to ${viewModel.hospital?.name}") },
                            text = {
                                Column {
                                    Text("Please provide details about your request:")
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = requestDescription,
                                        onValueChange = { requestDescription = it },
                                        label = { Text("Description") },
                                        modifier = Modifier.fillMaxWidth(),
                                        minLines = 3
                                    )
                                }
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        if (requestDescription.isNotEmpty()) {
                                            viewModel.sendHospitalRequest(requestDescription)
                                            showRequestDialog = false
                                            requestDescription = ""
                                            showConfirmationSnackbar = true
                                        }
                                    }
                                ) {
                                    Text("Send")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showRequestDialog = false }) {
                                    Text("Cancel")
                                }
                            }
                        )
                    }

                    // Add Review Dialog
                    if (showAddReviewDialog) {
                        AlertDialog(
                            onDismissRequest = { showAddReviewDialog = false },
                            title = { Text("Add Review for ${viewModel.hospital?.name}") },
                            text = {
                                Column {
                                    Text("Rate your experience:")
                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Rating slider
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Slider(
                                            value = reviewRating,
                                            onValueChange = { reviewRating = it },
                                            valueRange = 0f..5f,
                                            steps = 9,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = "%.1f".format(reviewRating),
                                            style = MaterialTheme.typography.bodyLarge,
                                            modifier = Modifier.padding(start = 8.dp)
                                        )
                                    }

                                    // Star display
                                    Row(
                                        modifier = Modifier
                                            .padding(vertical = 8.dp)
                                            .align(Alignment.CenterHorizontally)
                                    ) {
                                        val fullStars = reviewRating.toInt()
                                        val hasHalfStar = reviewRating - fullStars >= 0.5f

                                        repeat(fullStars) {
                                            Icon(
                                                painter = painterResource(id = R.drawable.hospital), // Replace with star icon
                                                contentDescription = "Star",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                        }

                                        if (hasHalfStar) {
                                            Icon(
                                                painter = painterResource(id = R.drawable.hospital), // Replace with half star icon
                                                contentDescription = "Half Star",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                        }

                                        repeat(5 - fullStars - (if (hasHalfStar) 1 else 0)) {
                                            Icon(
                                                painter = painterResource(id = R.drawable.hospital), // Replace with empty star icon
                                                contentDescription = "Empty Star",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Text("Write your review:")
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = reviewComment,
                                        onValueChange = { reviewComment = it },
                                        label = { Text("Review") },
                                        modifier = Modifier.fillMaxWidth(),
                                        minLines = 3
                                    )
                                }
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        if (reviewRating > 0 && reviewComment.isNotEmpty()) {
                                            viewModel.addReview(reviewRating, reviewComment)
                                            showAddReviewDialog = false
                                            reviewRating = 0f
                                            reviewComment = ""
                                            showReviewConfirmation = true
                                        }
                                    }
                                ) {
                                    Text("Submit")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showAddReviewDialog = false }) {
                                    Text("Cancel")
                                }
                            }
                        )
                    }

                    // Confirmation Snackbar for Request
                    if (showConfirmationSnackbar) {
                        Snackbar(
                            modifier = Modifier
                                .padding(16.dp)
                                .align(Alignment.BottomCenter),
                            action = {
                                TextButton(onClick = { showConfirmationSnackbar = false }) {
                                    Text("Dismiss")
                                }
                            }
                        ) {
                            Text("Request sent successfully!")
                        }

                        LaunchedEffect(showConfirmationSnackbar) {
                            kotlinx.coroutines.delay(3000)
                            showConfirmationSnackbar = false
                        }
                    }

                    // Confirmation Snackbar for Review
                    if (showReviewConfirmation) {
                        Snackbar(
                            modifier = Modifier
                                .padding(16.dp)
                                .align(Alignment.BottomCenter),
                            action = {
                                TextButton(onClick = { showReviewConfirmation = false }) {
                                    Text("Dismiss")
                                }
                            }
                        ) {
                            Text("Review submitted successfully!")
                        }

                        LaunchedEffect(showReviewConfirmation) {
                            kotlinx.coroutines.delay(3000)
                            showReviewConfirmation = false
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReviewItem(
    review: ReviewData,
    canDelete: Boolean = false,
    onDeleteClick: () -> Unit = {}
) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    val formattedDate = dateFormat.format(Date(review.timestamp))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = review.userName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (canDelete) {
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.hospital), // Replace with delete icon
                            contentDescription = "Delete Review",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.padding(vertical = 4.dp)
        ) {
            val fullStars = review.rating.toInt()
            val hasHalfStar = review.rating - fullStars >= 0.5f

            repeat(fullStars) {
                Icon(
                    painter = painterResource(id = R.drawable.hospital), // Replace with star icon
                    contentDescription = "Star",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(2.dp))
            }

            if (hasHalfStar) {
                Icon(
                    painter = painterResource(id = R.drawable.hospital), // Replace with half star icon
                    contentDescription = "Half Star",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(2.dp))
            }

            repeat(5 - fullStars - (if (hasHalfStar) 1 else 0)) {
                Icon(
                    painter = painterResource(id = R.drawable.hospital), // Replace with empty star icon
                    contentDescription = "Empty Star",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(2.dp))
            }
        }

        Text(
            text = review.comment,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
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
import com.example.last.fragments.DonorHomeViewModel

@Composable
fun Donorhome(
    viewModel: DonorHomeViewModel = viewModel()
) {
    var isEditing by remember { mutableStateOf(false) }
    val donorData by viewModel.donorData.collectAsState(initial = null)
    val context = LocalContext.current

    // Image picker launchers
    val profileImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.uploadImage(it, "profile") }
    }

    val medicalReportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.uploadImage(it, "medical_reports") }
    }

    val surgeryReportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.uploadImage(it, "surgery_reports") }
    }

    LaunchedEffect(Unit) {
        viewModel.fetchDonorData()
        viewModel.fetchProfileImage()

    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Profile Image Section
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(donorData?.profileImageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = "Profile Image",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        Button(
            onClick = { profileImageLauncher.launch("image/*") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Text("Upload Profile")
        }

        // Personal Information Section
        SectionHeader(text = "Personal Information")

        DonorTextField(
            value = donorData?.fullName ?: "",
            onValueChange = { viewModel.updateField("fullName", it) },
            label = "Full Name",
            enabled = isEditing
        )

        DonorTextField(
            value = donorData?.dob ?: "",
            onValueChange = { viewModel.updateField("dob", it) },
            label = "Date of Birth",
            enabled = isEditing
        )

        // Gender Selection
        // Gender Selection
        // Gender Selection
        var selectedGender by remember { mutableStateOf("") }

        LaunchedEffect(donorData?.gender) {
            donorData?.gender?.let { selectedGender = it }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            listOf("Male", "Female", "Other").forEach { gender ->
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedGender == gender,
                        onClick = {
                            if (isEditing) {
                                selectedGender = gender
                                viewModel.updateField("gender", gender) // Update Firebase
                            }
                        },
                        enabled = isEditing
                    )
                    Text(gender)
                }
            }
        }



        // Contact Information
        DonorTextField(
            value = donorData?.contactNumber ?: "",
            onValueChange = { viewModel.updateField("contactNumber", it) },
            label = "Contact Number",
            enabled = isEditing
        )

        DonorTextField(
            value = donorData?.email ?: "",
            onValueChange = { viewModel.updateField("email", it) },
            label = "Email",
            enabled = isEditing
        )

        DonorTextField(
            value = donorData?.address ?: "",
            onValueChange = { viewModel.updateField("address", it) },
            label = "Address",
            enabled = isEditing
        )

        // Medical Information Section
        SectionHeader(text = "Medical Information")

        DonorTextField(
            value = donorData?.bloodGroup ?: "",
            onValueChange = { viewModel.updateField("bloodGroup", it) },
            label = "Blood Group",
            enabled = isEditing
        )

        DonorTextField(
            value = donorData?.organAvailable ?: "",
            onValueChange = { viewModel.updateField("organAvailable", it) },
            label = "Organ(s) Available for Donation",
            enabled = isEditing
        )

        DonorTextField(
            value = donorData?.medicalHistory ?: "",
            onValueChange = { viewModel.updateField("medicalHistory", it) },
            label = "Medical History",
            enabled = isEditing,
            singleLine = false
        )

        DonorTextField(
            value = donorData?.surgeries ?: "",
            onValueChange = { viewModel.updateField("surgeries", it) },
            label = "Previous Surgeries",
            enabled = isEditing,
            singleLine = false
        )

        Button(
            onClick = { surgeryReportLauncher.launch("*/*") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Upload Surgery Reports")
        }

        DonorTextField(
            value = donorData?.medications ?: "",
            onValueChange = { viewModel.updateField("medications", it) },
            label = "Current Medications",
            enabled = isEditing
        )

        DonorTextField(
            value = donorData?.allergies ?: "",
            onValueChange = { viewModel.updateField("allergies", it) },
            label = "Allergies",
            enabled = isEditing
        )

        DonorTextField(
            value = donorData?.lifestyle ?: "",
            onValueChange = { viewModel.updateField("lifestyle", it) },
            label = "Lifestyle",
            enabled = isEditing
        )

        DonorTextField(
            value = donorData?.geneticDisorders ?: "",
            onValueChange = { viewModel.updateField("geneticDisorders", it) },
            label = "Genetic Disorders",
            enabled = isEditing
        )

        Button(
            onClick = { medicalReportLauncher.launch("*/*") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Upload Medical Reports")
        }

        // Emergency Contact Section
        SectionHeader(text = "Emergency Contact")

        DonorTextField(
            value = donorData?.emergencyContact ?: "",
            onValueChange = { viewModel.updateField("emergencyContact", it) },
            label = "Emergency Contact",
            enabled = isEditing
        )

        // Location Section
        SectionHeader(text = "Location Details")

        DonorTextField(
            value = donorData?.currentLocation ?: "",
            onValueChange = { viewModel.updateField("currentLocation", it) },
            label = "Current Location",
            enabled = isEditing
        )

        DonorTextField(
            value = donorData?.preferredLocation ?: "",
            onValueChange = { viewModel.updateField("preferredLocation", it) },
            label = "Preferred Donation Locations",
            enabled = isEditing
        )

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
                        viewModel.saveDonorData()
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
private fun DonorTextField(
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
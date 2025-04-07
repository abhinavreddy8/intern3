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
import com.example.last.fragments.RecipientHomeViewModel

@Composable
fun Recipienthome(
    viewModel: RecipientHomeViewModel = viewModel()
) {
    var isEditing by remember { mutableStateOf(false) }
    val recipientData by viewModel.recipientData.collectAsState(initial = null)
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

    val diagnosticReportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.uploadImage(it, "diagnostic_reports") }
    }

    LaunchedEffect(Unit) {
        viewModel.fetchRecipientData()
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
                    .data(recipientData?.profileImageUrl)
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

        RecipientTextField(
            value = recipientData?.fullName ?: "",
            onValueChange = { viewModel.updateField("fullName", it) },
            label = "Full Name",
            enabled = isEditing
        )

        RecipientTextField(
            value = recipientData?.dob ?: "",
            onValueChange = { viewModel.updateField("dob", it) },
            label = "Date of Birth",
            enabled = isEditing
        )

        // Gender Selection
        var selectedGender by remember { mutableStateOf("") }

        LaunchedEffect(recipientData?.gender) {
            recipientData?.gender?.let { selectedGender = it }
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
        RecipientTextField(
            value = recipientData?.contactNumber ?: "",
            onValueChange = { viewModel.updateField("contactNumber", it) },
            label = "Contact Number",
            enabled = isEditing
        )

        RecipientTextField(
            value = recipientData?.email ?: "",
            onValueChange = { viewModel.updateField("email", it) },
            label = "Email",
            enabled = isEditing
        )

        RecipientTextField(
            value = recipientData?.address ?: "",
            onValueChange = { viewModel.updateField("address", it) },
            label = "Address",
            enabled = isEditing
        )

        // Medical Information Section
        SectionHeader(text = "Medical Information")

        RecipientTextField(
            value = recipientData?.bloodGroup ?: "",
            onValueChange = { viewModel.updateField("bloodGroup", it) },
            label = "Blood Group",
            enabled = isEditing
        )

        RecipientTextField(
            value = recipientData?.organNeeded ?: "",
            onValueChange = { viewModel.updateField("organNeeded", it) },
            label = "Organ(s) Needed",
            enabled = isEditing
        )

        RecipientTextField(
            value = recipientData?.urgencyLevel ?: "",
            onValueChange = { viewModel.updateField("urgencyLevel", it) },
            label = "Urgency Level",
            enabled = isEditing
        )

        RecipientTextField(
            value = recipientData?.medicalHistory ?: "",
            onValueChange = { viewModel.updateField("medicalHistory", it) },
            label = "Medical History",
            enabled = isEditing,
            singleLine = false
        )

        RecipientTextField(
            value = recipientData?.diagnosisDetails ?: "",
            onValueChange = { viewModel.updateField("diagnosisDetails", it) },
            label = "Diagnosis Details",
            enabled = isEditing,
            singleLine = false
        )

        Button(
            onClick = { diagnosticReportLauncher.launch("*/*") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Upload Diagnostic Reports")
        }

        RecipientTextField(
            value = recipientData?.medications ?: "",
            onValueChange = { viewModel.updateField("medications", it) },
            label = "Current Medications",
            enabled = isEditing
        )

        RecipientTextField(
            value = recipientData?.allergies ?: "",
            onValueChange = { viewModel.updateField("allergies", it) },
            label = "Allergies",
            enabled = isEditing
        )

        RecipientTextField(
            value = recipientData?.doctorName ?: "",
            onValueChange = { viewModel.updateField("doctorName", it) },
            label = "Doctor/Specialist Name",
            enabled = isEditing
        )

        RecipientTextField(
            value = recipientData?.hospitalName ?: "",
            onValueChange = { viewModel.updateField("hospitalName", it) },
            label = "Hospital Name",
            enabled = isEditing
        )

        Button(
            onClick = { medicalReportLauncher.launch("*/*") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Upload Medical Reports")
        }

        // Insurance Information
        SectionHeader(text = "Insurance Information")

        RecipientTextField(
            value = recipientData?.insuranceProvider ?: "",
            onValueChange = { viewModel.updateField("insuranceProvider", it) },
            label = "Insurance Provider",
            enabled = isEditing
        )

        RecipientTextField(
            value = recipientData?.insuranceId ?: "",
            onValueChange = { viewModel.updateField("insuranceId", it) },
            label = "Insurance ID",
            enabled = isEditing
        )

        // Emergency Contact Section
        SectionHeader(text = "Emergency Contact")

        RecipientTextField(
            value = recipientData?.emergencyContact ?: "",
            onValueChange = { viewModel.updateField("emergencyContact", it) },
            label = "Emergency Contact",
            enabled = isEditing
        )

        // Location Section
        SectionHeader(text = "Location Details")

        RecipientTextField(
            value = recipientData?.currentLocation ?: "",
            onValueChange = { viewModel.updateField("currentLocation", it) },
            label = "Current Location",
            enabled = isEditing
        )

        RecipientTextField(
            value = recipientData?.preferredHospitals ?: "",
            onValueChange = { viewModel.updateField("preferredHospitals", it) },
            label = "Preferred Hospitals",
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
                        viewModel.saveRecipientData()
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
private fun RecipientTextField(
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
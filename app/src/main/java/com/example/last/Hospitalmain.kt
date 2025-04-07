package com.example.last

//import Hospitalhome
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.example.last.fragments.*
import com.example.last.ui.theme.LastTheme
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

class Hospitalmain : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize the storage FirebaseApp
        initializeStorageFirebase()

        setContent {
            LastTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    HospitalApp()
                }
            }
        }
    }

    private fun initializeStorageFirebase() {
        val storageOptions = FirebaseOptions.Builder()
            .setProjectId("socialmedia-b9148") // Replace with your Firebase Storage project's ID
            .setApplicationId("1:924036427672:android:75e24d5ebe6dd3f35cc5ed") // Replace with your Firebase app's application ID
            .setApiKey("AIzaSyBLVNr5M0sHOTtGpqBvn8ula-knHx0vxvc") // Replace with your Firebase app's API key
            .setStorageBucket("socialmedia-b9148.appspot.com") // Replace with your Firebase Storage bucket URL
            .build()

        // Initialize Firebase with these options, giving it a different name
        FirebaseApp.initializeApp(this, storageOptions, "hospitalStorageApp")
    }
}

@Composable
fun HospitalApp() {
    val navController = rememberNavController()
    val hospitalHomeViewModel: HospitalHomeViewModel = viewModel()

    // Get the storage app instance
    val storageApp = remember {
        FirebaseApp.getInstance("hospitalStorageApp")
    }

    // Initialize storage in the ViewModel
    LaunchedEffect(Unit) {
        hospitalHomeViewModel.initializeStorage(storageApp)
    }

    Scaffold(
        bottomBar = { HospitalBottomNavigation(navController) }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = HospitalScreen.HospitalHome.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(HospitalScreen.HospitalHome.route) {
                Hospitalhome(viewModel = hospitalHomeViewModel)
            }
            composable(HospitalScreen.HospitalSearchDonors.route) { Hospitalsearchdonors() }
            composable(HospitalScreen.HospitalSearchRecipients.route) { Hospitalrequests() }
            composable(HospitalScreen.HospitalNotifications.route) { Hospitalacceptance() }
        }
    }
}

@Composable
fun HospitalBottomNavigation(navController: NavHostController) {
    val screens = listOf(
        HospitalScreen.HospitalHome,
        HospitalScreen.HospitalSearchDonors,
        HospitalScreen.HospitalSearchRecipients,
        HospitalScreen.HospitalNotifications
    )

    var selectedScreen by remember { mutableStateOf(HospitalScreen.HospitalHome.route) }

    NavigationBar {
        screens.forEach { screen ->
            NavigationBarItem(
                icon = {
                    Icon(
                        painter = painterResource(id = getHospitalIcon(screen.route)),
                        contentDescription = screen.route
                    )
                },
                label = { Text(screen.route.replaceFirstChar { it.uppercase() }) },
                selected = selectedScreen == screen.route,
                onClick = {
                    selectedScreen = screen.route
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}

@Composable
fun getHospitalIcon(route: String): Int {
    return when (route) {
        HospitalScreen.HospitalHome.route -> R.drawable.hospital
        HospitalScreen.HospitalSearchDonors.route -> R.drawable.hospital
        HospitalScreen.HospitalSearchRecipients.route -> R.drawable.hospital
        HospitalScreen.HospitalNotifications.route -> R.drawable.hospital
        else -> R.drawable.hospital
    }
}

sealed class HospitalScreen(val route: String) {
    object HospitalHome : HospitalScreen("hospitalhome")
    object HospitalSearchDonors : HospitalScreen("hospitalsearchdonors")
    object HospitalSearchRecipients : HospitalScreen("hospitalsearchrecipients")
    object HospitalNotifications : HospitalScreen("hospitalnotifications")
}
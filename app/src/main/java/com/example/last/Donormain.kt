package com.example.last

import Donorhome
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

class Donormain : ComponentActivity() {
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
                    DonorApp()
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
        FirebaseApp.initializeApp(this, storageOptions, "storageApp")
    }
}

@Composable
fun DonorApp() {
    val navController = rememberNavController()
    val donorHomeViewModel: DonorHomeViewModel = viewModel()

    // Get the storage app instance
    val storageApp = remember {
        FirebaseApp.getInstance("storageApp")
    }

    // Initialize storage in the ViewModel
    LaunchedEffect(Unit) {
        donorHomeViewModel.initializeStorage(storageApp)
    }

    Scaffold(
        bottomBar = { DonorBottomNavigation(navController) }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.DonorHome.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.DonorHome.route) {
                Donorhome(viewModel = donorHomeViewModel)
            }
            composable(Screen.DonorSearchHospitals.route) { Donorsearchhospitals() }
            composable(Screen.DonorSearchRecipients.route) { Donorsearchrecipients() }
            composable(Screen.DonorNotifications.route) { Donornotifications() }
        }
    }
}

@Composable
fun DonorBottomNavigation(navController: NavHostController) {
    val screens = listOf(
        Screen.DonorHome,
        Screen.DonorSearchHospitals,
        Screen.DonorSearchRecipients,
        Screen.DonorNotifications
    )

    var selectedScreen by remember { mutableStateOf(Screen.DonorHome.route) }

    NavigationBar {
        screens.forEach { screen ->
            NavigationBarItem(
                icon = {
                    Icon(
                        painter = painterResource(id = getIcon(screen.route)),
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
fun getIcon(route: String): Int {
    return when (route) {
        Screen.DonorHome.route -> R.drawable.donor
        Screen.DonorSearchHospitals.route -> R.drawable.donor
        Screen.DonorSearchRecipients.route -> R.drawable.donor
        Screen.DonorNotifications.route -> R.drawable.donor
        else -> R.drawable.donor
    }
}

sealed class Screen(val route: String) {
    object DonorHome : Screen("donorhome")
    object DonorSearchHospitals : Screen("donorsearchhospitals")
    object DonorSearchRecipients : Screen("donorsearchrecipients")
    object DonorNotifications : Screen("donornotifications")
}
package com.example.last

import Recipienthome
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

class Recipientmain : ComponentActivity() {
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
                    RecipientApp()
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
fun RecipientApp() {
    val navController = rememberNavController()
    val recipientHomeViewModel: RecipientHomeViewModel = viewModel()

    // Get the storage app instance
    val storageApp = remember {
        FirebaseApp.getInstance("storageApp")
    }

    // Initialize storage in the ViewModel
    LaunchedEffect(Unit) {
        recipientHomeViewModel.initializeStorage(storageApp)
    }

    Scaffold(
        bottomBar = { RecipientBottomNavigation(navController) }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = RecipientScreen.RecipientHome.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(RecipientScreen.RecipientHome.route) {
                Recipienthome(viewModel = recipientHomeViewModel)
            }
            composable(RecipientScreen.RecipientSearchDonors.route) { Recipientsearchdonors() }
            composable(RecipientScreen.RecipientSearchHospitals.route) { Recipientsearchhospitals() }
            composable(RecipientScreen.RecipientNotifications.route) { Recipientnotifications() }
        }
    }
}

@Composable
fun RecipientBottomNavigation(navController: NavHostController) {
    val screens = listOf(
        RecipientScreen.RecipientHome,
        RecipientScreen.RecipientSearchDonors,
        RecipientScreen.RecipientSearchHospitals,
        RecipientScreen.RecipientNotifications
    )

    var selectedScreen by remember { mutableStateOf(RecipientScreen.RecipientHome.route) }

    NavigationBar {
        screens.forEach { screen ->
            NavigationBarItem(
                icon = {
                    Icon(
                        painter = painterResource(id = getRecipientIcon(screen.route)),
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
fun getRecipientIcon(route: String): Int {
    return when (route) {
        RecipientScreen.RecipientHome.route -> R.drawable.recipient
        RecipientScreen.RecipientSearchDonors.route -> R.drawable.recipient
        RecipientScreen.RecipientSearchHospitals.route -> R.drawable.recipient
        RecipientScreen.RecipientNotifications.route -> R.drawable.recipient
        else -> R.drawable.recipient
    }
}

sealed class RecipientScreen(val route: String) {
    object RecipientHome : RecipientScreen("recipienthome")
    object RecipientSearchDonors : RecipientScreen("recipientsearchdonors")
    object RecipientSearchHospitals : RecipientScreen("recipientsearchhospitals")
    object RecipientNotifications : RecipientScreen("recipientnotifications")
}
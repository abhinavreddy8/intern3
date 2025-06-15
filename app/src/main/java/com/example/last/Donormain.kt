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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.last.fragments.*
import com.example.last.screens.DonorNotificationScreen
import com.example.last.ui.DonorSearchHospital
import com.example.last.ui.DonorSearchRecipientScreen
import com.example.last.ui.HospitalDetailScreen
//import com.example.last.ui.NotificationScreen
import com.example.last.ui.RecipientDetailScreen
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
            composable(Screen.DonorSearchHospitals.route) { DonorSearchHospital (
                navigateToHospitalDetail = { hospitalId ->
                    navController.navigate(HospitalRoutes.HospitalDetail.createRoute(hospitalId))
                }
            )

             }
            composable(Screen.DonorSearchRecipients.route) {
                DonorSearchRecipientScreen(
                    navController
                )
            }

            composable(
                route = Screen.RecipientDetail.route,
                arguments = listOf(navArgument("recipientId") { defaultValue = "" })
            ) { backStackEntry ->
                val recipientId = backStackEntry.arguments?.getString("recipientId") ?: ""
                RecipientDetailScreen(recipientId = recipientId, onBackPressed = { navController.popBackStack() })
            }
            composable(
                route = HospitalRoutes.HospitalDetail.route,
                arguments = listOf(navArgument("hospitalId") { type = NavType.StringType })
            ) { backStackEntry ->
                val hospitalId = backStackEntry.arguments?.getString("hospitalId") ?: ""
                HospitalDetailScreen(hospitalId = hospitalId, onBackClick = { navController.popBackStack() })
            }

            composable(Screen.DonorNotifications.route) {
                DonorNotificationScreen()
            }
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

    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route

    NavigationBar(
        tonalElevation = 4.dp,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
    ) {
        screens.forEach { screen ->
            NavigationBarItem(
                icon = {
                    Icon(
                        painter = painterResource(id = getIcon(screen.route)),
                        contentDescription = screen.route,
                        modifier = Modifier.size(24.dp) // Reduced icon size
                    )
                },
                selected = currentRoute == screen.route,
                onClick = {
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                alwaysShowLabel = false, // Hide labels
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                )
            )
        }
    }
}

@Composable
fun getIcon(route: String): Int {
    return when (route) {
        Screen.DonorHome.route -> R.drawable.home
        Screen.DonorSearchHospitals.route -> R.drawable.searchicon
        Screen.DonorSearchRecipients.route -> R.drawable.searchrecipient
        Screen.DonorNotifications.route -> R.drawable.notification
        else -> R.drawable.donor
    }
}

sealed class Screen(val route: String) {
    object DonorHome : Screen("donorhome")
    object DonorSearchHospitals : Screen("donorsearchhospitals")
    object DonorSearchRecipients : Screen("donorsearchrecipients")
    object DonorNotifications : Screen("donornotifications")
    object RecipientDetail : Screen("recipientdetail/{recipientId}") {
        fun createRoute(recipientId: String) = "recipientdetail/$recipientId"
    }
}
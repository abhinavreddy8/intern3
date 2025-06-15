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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.*
import androidx.navigation.compose.*
import com.example.last.fragments.*
import com.example.last.ui.RecipientSearchHospitals
import com.example.last.ui.Recipientsearchdonors
import com.example.last.ui.theme.LastTheme
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.example.last.ui.DonorDetailScreen
import com.example.last.ui.HospitalDetailScreen
import com.example.last.HospitalRoutes

class Recipientmain : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
            .setProjectId("socialmedia-b9148")
            .setApplicationId("1:924036427672:android:75e24d5ebe6dd3f35cc5ed")
            .setApiKey("")
            .setStorageBucket("")
            .build()

        FirebaseApp.initializeApp(this, storageOptions, "storageApp")
    }
}

@Composable
fun RecipientApp() {
    val navController = rememberNavController()
    val recipientHomeViewModel: RecipientHomeViewModel = viewModel()

    val storageApp = remember {
        FirebaseApp.getInstance("storageApp")
    }

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
            composable(RecipientScreen.RecipientSearchDonors.route) {
                Recipientsearchdonors(navController = navController)
            }
            composable(RecipientScreen.RecipientSearchHospitals.route) {
                RecipientSearchHospitals(navigateToHospitalDetail = { hospitalId ->
                    // Handle hospital detail navigation
                    navController.navigate(HospitalRoutes.HospitalDetail.createRoute(hospitalId))
                })
            }
            // Here's the fixed part for RecipientNotifications
            composable(RecipientScreen.RecipientNotifications.route) {
                // Make sure to provide the fragment directly from the package
                RecipientNotificationsScreen()
            }
            composable(
                route = DonorRoutes.DonorDetail.route,
                arguments = listOf(navArgument("donorId") { type = NavType.StringType })
            ) { backStackEntry ->
                val donorId = backStackEntry.arguments?.getString("donorId") ?: ""
                DonorDetailScreen(donorId = donorId, onBackPressed = { navController.popBackStack() })
            }
            composable(
                route = HospitalRoutes.HospitalDetail.route,
                arguments = listOf(navArgument("hospitalId") { type = NavType.StringType })
            ) { backStackEntry ->
                val hospitalId = backStackEntry.arguments?.getString("hospitalId") ?: ""
                HospitalDetailScreen(hospitalId = hospitalId, onBackClick = { navController.popBackStack() })
            }
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

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

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
                        painter = painterResource(id = getRecipientIcon(screen.route)),
                        contentDescription = screen.route,
                        modifier = Modifier.size(24.dp) // Reduced icon size
                    )
                },
                selected = currentRoute == screen.route,
                onClick = {
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                alwaysShowLabel = false,
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
fun getRecipientIcon(route: String): Int {
    return when (route) {
        RecipientScreen.RecipientHome.route -> R.drawable.home
        RecipientScreen.RecipientSearchDonors.route -> R.drawable.searchdonor
        RecipientScreen.RecipientSearchHospitals.route -> R.drawable.searchicon
        RecipientScreen.RecipientNotifications.route -> R.drawable.notification// Changed to a notification icon
        else -> R.drawable.recipient
    }
}

sealed class RecipientScreen(val route: String) {
    object RecipientHome : RecipientScreen("recipienthome")
    object RecipientSearchDonors : RecipientScreen("recipientsearchdonors")
    object RecipientSearchHospitals : RecipientScreen("recipientsearchhospitals")
    object RecipientNotifications : RecipientScreen("recipientnotifications")
    object RecipientDonorDetail : RecipientScreen("donorDetail/{donorId}") {
        fun createRoute(donorId: String) = "donorDetail/$donorId"
    }
}

package com.example.last

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
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.last.fragments.*
import com.example.last.ui.DonorDetailScreen
import com.example.last.ui.HospitalSearchDonors
import com.example.last.ui.theme.LastTheme
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import android.util.Log
import androidx.compose.ui.unit.dp
//import com.example.last.ui.HospitalNotificationsScreen
import com.example.last.ui.HospitalRequestHandlerScreen
import com.example.last.ui.HospitalSearchRecipientScreen
import com.example.last.ui.RecipientDetailScreen

//import com.example.last.ui.HospitalRequestHandlerScreen

class Hospitalmain : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
        try {
            val storageOptions = FirebaseOptions.Builder()
                .setProjectId("socialmedia-b9148")
                .setApplicationId("1:924036427672:android:75e24d5ebe6dd3f35cc5ed")
                .setApiKey("AIzaSyBLVNr5M0sHOTtGpqBvn8ula-knHx0vxvc")
                .setStorageBucket("socialmedia-b9148.appspot.com")
                .build()

            if (FirebaseApp.getApps(this).none { it.name == "hospitalStorageApp" }) {
                FirebaseApp.initializeApp(this, storageOptions, "hospitalStorageApp")
            }
        } catch (e: Exception) {
            Log.e("Hospitalmain", "Error initializing Firebase: ${e.message}")
        }
    }
}

@Composable
fun HospitalApp() {
    val navController = rememberNavController()
    val hospitalHomeViewModel: HospitalHomeViewModel = viewModel()

    val storageApp = remember {
        try {
            FirebaseApp.getInstance("hospitalStorageApp")
        } catch (e: Exception) {
            Log.e("HospitalApp", "Error getting Firebase app: ${e.message}")
            null
        }
    }

    LaunchedEffect(Unit) {
        storageApp?.let {
            hospitalHomeViewModel.initializeStorage(it)
        }
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
                Log.d("Navigation", "Navigating to HospitalHome")
                Hospitalhome(viewModel = hospitalHomeViewModel)
            }
            composable(HospitalScreen.HospitalSearchDonors.route) {
                Log.d("Navigation", "Navigating to HospitalSearchDonors")
                HospitalSearchDonors(navController)
            }
            composable(HospitalScreen.HospitalSearchRecipients.route) {
                Log.d("Navigation", "Navigating to HospitalSearchRecipients")
                HospitalSearchRecipientScreen(navController)
            }

            composable(
                route = Screen.RecipientDetail.route,
                arguments = listOf(navArgument("recipientId") { defaultValue = "" })
            ) { backStackEntry ->
                val recipientId = backStackEntry.arguments?.getString("recipientId") ?: ""
                RecipientDetailScreen(recipientId = recipientId, onBackPressed = { navController.popBackStack() })
            }
            composable(HospitalScreen.HospitalNotifications.route) {
                Log.d("Navigation", "Navigating to HospitalNotifications")

                HospitalRequestsScreen() // This matches the composable you shared

            }
            composable(
                route = DonorRoutes.DonorDetail.route,
                arguments = listOf(navArgument("donorId") { type = NavType.StringType })
            ) { backStackEntry ->
                DonorDetailScreen(
                    donorId = backStackEntry.arguments?.getString("donorId") ?: "",
                    onBackPressed = { navController.popBackStack() }
                )
            }
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
                        painter = painterResource(id = getHospitalIcon(screen.route)),
                        contentDescription = screen.route,
                        modifier = Modifier.size(24.dp) // Reduced icon size
                    )
                },
                selected = currentRoute == screen.route,
                onClick = {
                    Log.d("Navigation", "Bottom nav clicked: ${screen.route}")
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                alwaysShowLabel = false, // Hides label for minimal design
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
fun getHospitalIcon(route: String): Int {
    return when (route) {
        HospitalScreen.HospitalHome.route -> R.drawable.home
        HospitalScreen.HospitalSearchDonors.route -> R.drawable.searchdonor
        HospitalScreen.HospitalSearchRecipients.route -> R.drawable.searchrecipient
        HospitalScreen.HospitalNotifications.route -> R.drawable.notification
        else -> R.drawable.hospital
    }
}

sealed class HospitalScreen(val route: String) {
    object HospitalHome : HospitalScreen("hospitalhome")
    object HospitalSearchDonors : HospitalScreen("hospitalsearchdonors")
    object HospitalSearchRecipients : HospitalScreen("hospitalsearchrecipients")
    object HospitalNotifications : HospitalScreen("hospitalnotifications")
}


//object DonorRoutes {
//    object DonorDetail {
//        const val route = "donorDetail/{donorId}"
//    }
//}
package com.example.last.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.last.fragments.Hospitalrequests
import com.example.last.fragments.Recipientnotifications
import com.example.last.ui.HospitalDetailScreen
import com.example.last.ui.HospitalRequestHandlerScreen
//import com.example.last.ui.HospitalNotificationsScreen
//import com.example.last.ui.HospitalRequestHandlerScreen
import com.example.last.ui.RecipientSearchHospitals

@Composable
fun SetupNavigation(navController: NavHostController) {
    NavHost(navController = navController, startDestination = "search_hospitals") {
        // Recipient Hospital Search Screen
        composable("search_hospitals") {
            RecipientSearchHospitals { hospitalId ->
                navController.navigate("hospital_detail/$hospitalId")
            }
        }

        // Hospital Detail Screen
        composable(
            "hospital_detail/{hospitalId}",
            arguments = listOf(navArgument("hospitalId") { type = NavType.StringType })
        ) { backStackEntry ->
            val hospitalId = backStackEntry.arguments?.getString("hospitalId") ?: ""
            HospitalDetailScreen(
                hospitalId = hospitalId,
                onBackClick = { navController.popBackStack() }
            )
        }

        // Recipient Hospital Requests Screen
        composable("hospital_requests") {
            Hospitalrequests()
        }

        // Recipient Notifications Screen
        composable("recipient_notifications") {
            Recipientnotifications()
        }

        // Hospital-side Request Handler Screen
        composable("hospital_request_handler") {
            HospitalRequestHandlerScreen()
        }
    }
}
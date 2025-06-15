package com.example.last

sealed class DonorRoutes(val route: String) {
    object DonorDetail : DonorRoutes("donorDetail/{donorId}") {
        fun createRoute(donorId: String) = "donorDetail/$donorId"
    }
}
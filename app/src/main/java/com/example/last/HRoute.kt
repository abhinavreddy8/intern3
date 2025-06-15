package com.example.last

sealed class HospitalRoutes(val route: String) {
    object HospitalDetail : HospitalRoutes("hospitalDetail/{hospitalId}") {
        fun createRoute(hospitalId: String) = "hospitalDetail/$hospitalId"
    }
}

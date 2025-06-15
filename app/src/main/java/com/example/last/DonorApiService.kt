package com.example.last

import retrofit2.http.Body
import retrofit2.http.POST

interface DonorApiService {
    @POST("nearby-donors")
    suspend fun findNearbyDonors(@Body request: DonorRequest): NearbyDonorsResponse
}

data class DonorRequest(
    val latitude: Double,
    val longitude: Double
)

data class NearbyDonorsResponse(
    val cluster: Int,
    val nearbyDonors: Int,
    val modelUsed: Boolean,
    val modelStatus: String
)
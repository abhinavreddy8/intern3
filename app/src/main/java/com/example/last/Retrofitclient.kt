package com.example.last

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object Retrofitclient {
    private const val BASE_URL = "http://192.168.1.9:8080/api/"

    val donorApiService: DonorApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(DonorApiService::class.java)
    }
}
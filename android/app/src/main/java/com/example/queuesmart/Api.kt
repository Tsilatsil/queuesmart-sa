package com.example.queuesmart

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

// A blueprint for one branch, matching the JSON from your backend
data class Branch(
    val id: String,
    val name: String,
    val city: String,
    val province: String,
    val address: String,
    val opensAtMin: Int,
    val closesAtMin: Int,
    val slotCapacity: Int
)

// The questions the app can ask your API
interface QueueSmartApi {
    @GET("api/branches")
    suspend fun getBranches(): List<Branch>
}

// Builds the connection to your backend
object ApiClient {
    val api: QueueSmartApi = Retrofit.Builder()
        .baseUrl("http://localhost:3000/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(QueueSmartApi::class.java)
}
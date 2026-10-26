package com.example.queuesmart

import com.google.gson.Gson
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

// ---- Data blueprints (they match the JSON from your backend) ----

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

data class RequiredDocument(
    val id: String,
    val name: String,
    val note: String?
)

data class ServiceType(
    val id: String,
    val name: String,
    val description: String?,
    val durationMin: Int,
    val requiredDocuments: List<RequiredDocument>
)

data class User(
    val id: String,
    val name: String,
    val email: String,
    val role: String
)

data class AuthResponse(
    val token: String,
    val user: User
)

data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class Slot(
    val startsAt: String,
    val time: String,
    val capacity: Int,
    val remaining: Int,
    val available: Boolean
)

data class SlotsResponse(
    val branchId: String,
    val date: String,
    val closed: Boolean,
    val message: String?,
    val slots: List<Slot>
)

data class BookingRequest(
    val branchId: String,
    val serviceTypeId: String,
    val startsAt: String
)

data class BookedAppointment(
    val id: String,
    val reference: String,
    val startsAt: String,
    val status: String,
    val branch: String,
    val service: String
)

data class BookingResponse(
    val appointment: BookedAppointment
)

// A document in the "my bookings" list (this endpoint sends no id for documents)
data class DocumentItem(
    val name: String,
    val note: String?
)

data class MyAppointment(
    val id: String,
    val reference: String,
    val startsAt: String,
    val status: String,
    val branch: String,
    val address: String,
    val service: String,
    val requiredDocuments: List<DocumentItem>
)

data class CancelledAppointment(
    val id: String,
    val reference: String,
    val status: String
)

data class CancelResponse(
    val appointment: CancelledAppointment
)

data class ErrorBody(val error: String?)

// ---- The questions the app can ask your API ----

interface QueueSmartApi {
    @GET("api/branches")
    suspend fun getBranches(): List<Branch>

    @GET("api/services")
    suspend fun getServices(): List<ServiceType>

    @GET("api/slots")
    suspend fun getSlots(
        @Query("branchId") branchId: String,
        @Query("date") date: String
    ): SlotsResponse

    @POST("api/auth/register")
    suspend fun register(@Body body: RegisterRequest): AuthResponse

    @POST("api/auth/login")
    suspend fun login(@Body body: LoginRequest): AuthResponse

    // The token proves who is booking
    @POST("api/appointments")
    suspend fun book(
        @Header("Authorization") authorization: String,
        @Body body: BookingRequest
    ): BookingResponse

    @GET("api/my-appointments")
    suspend fun getMyAppointments(
        @Header("Authorization") authorization: String
    ): List<MyAppointment>

    @DELETE("api/appointments/{id}")
    suspend fun cancelAppointment(
        @Header("Authorization") authorization: String,
        @Path("id") id: String
    ): CancelResponse
}

// ---- Builds the connection to your backend ----

object ApiClient {
    val api: QueueSmartApi = Retrofit.Builder()
        .baseUrl("http://localhost:3000/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(QueueSmartApi::class.java)
}

// Turns an error into a message a person can read.
// If the backend sent {"error": "..."}, we show that text.
fun errorMessage(e: Exception): String {
    if (e is HttpException) {
        val body = e.response()?.errorBody()?.string()
        val parsed = try {
            Gson().fromJson(body, ErrorBody::class.java)
        } catch (_: Exception) {
            null
        }
        return parsed?.error ?: "Server error (${e.code()})"
    }
    return e.message ?: "Something went wrong"
}
package com.jeff.mosbookings.network

import com.jeff.mosbookings.models.RoomData
import com.jeff.mosbookings.models.AuthResponse
import retrofit2.Response
import retrofit2.http.*
import okhttp3.MultipartBody
import okhttp3.RequestBody

interface ApiService {
    @POST("api/auth/register")
    suspend fun register(@Body request: Map<String, String>): Response<AuthResponse>

    @POST("api/auth/login")
    suspend fun login(@Body request: Map<String, String>): Response<AuthResponse>

    @GET("api/rooms")
    suspend fun getRooms(): Response<List<RoomData>>

    @GET("api/rooms/{id}")
    suspend fun getRoomById(@Path("id") id: String): Response<RoomData>

    @POST("api/bookings/book")
    suspend fun bookRoom(@Header("Authorization") token: String, @Body request: Map<String, String>): Response<Map<String, String>>

    @POST("api/bookings/cancel")
    suspend fun cancelBooking(@Header("Authorization") token: String, @Body request: Map<String, String>): Response<Map<String, String>>

    @GET("api/bookings/my")
    suspend fun getUserBookings(@Header("Authorization") token: String): Response<List<Map<String, Any>>>

    // Admin routes
    @GET("api/bookings/all")
    suspend fun getAllBookings(
        @Header("Authorization") token: String,
        @Query("startDate") startDate: String? = null,
        @Query("endDate") endDate: String? = null
    ): Response<List<Map<String, Any>>>

    @Multipart
    @POST("api/rooms")
    suspend fun createRoom(
        @Header("Authorization") token: String,
        @Part images: List<MultipartBody.Part>,
        @Part("roomName") roomName: RequestBody,
        @Part("roomType") roomType: RequestBody,
        @Part("roomLocation") roomLocation: RequestBody,
        @Part("price") price: RequestBody,
        @Part("amenities") amenities: RequestBody,
        @Part("rating") rating: RequestBody,
        @Part("description") description: RequestBody,
        @Part("unavailableDates") unavailableDates: RequestBody
    ): Response<RoomData>

    @PUT("api/rooms/{id}")
    suspend fun updateRoom(@Header("Authorization") token: String, @Path("id") id: String, @Body room: RoomData): Response<RoomData>

    @DELETE("api/rooms/{id}")
    suspend fun deleteRoom(@Header("Authorization") token: String, @Path("id") id: String): Response<Map<String, String>>
}

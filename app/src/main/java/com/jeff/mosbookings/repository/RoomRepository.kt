package com.jeff.mosbookings.repository

import com.jeff.mosbookings.models.RoomData
import com.jeff.mosbookings.network.ApiService
import com.jeff.mosbookings.network.RetrofitInstance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.jeff.mosbookings.models.BookingData
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import android.content.Context
import android.net.Uri

class RoomRepository {
    private val apiService: ApiService = RetrofitInstance.api

    suspend fun getRooms(): List<RoomData>? {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getRooms()
                if (response.isSuccessful) {
                    response.body()
                } else {
                    null
                }
            } catch (e: Exception) {
                // Handle exceptions like no internet connection
                null
            }
        }
    }

    suspend fun uploadRoom(
        context: Context,
        imageUris: List<Uri>,
        roomName: String,
        roomType: String,
        roomLocation: String,
        price: Double,
        amenities: List<String>,
        rating: Float,
        description: String,
        unavailableDates: List<String>,
        token: String
    ): RoomData? {
        return withContext(Dispatchers.IO) {
            try {
                val contentResolver = context.contentResolver
                val images = imageUris.mapIndexed { idx, uri ->
                    val mimeType = contentResolver.getType(uri) ?: "image/jpeg"
                    val extension = when (mimeType) {
                        "image/png" -> "png"
                        "image/jpg", "image/jpeg" -> "jpg"
                        else -> "jpg"
                    }
                    val fileName = "image_${System.currentTimeMillis()}_$idx.$extension"
                    val inputStream = contentResolver.openInputStream(uri)
                    val bytes = inputStream?.readBytes() ?: ByteArray(0)
                    val requestFile = RequestBody.create(mimeType.toMediaTypeOrNull(), bytes)
                    MultipartBody.Part.createFormData("images", fileName, requestFile)
                }
                val roomNameBody = RequestBody.create("text/plain".toMediaTypeOrNull(), roomName)
                val roomTypeBody = RequestBody.create("text/plain".toMediaTypeOrNull(), roomType)
                val roomLocationBody = RequestBody.create("text/plain".toMediaTypeOrNull(), roomLocation)
                val priceBody = RequestBody.create("text/plain".toMediaTypeOrNull(), price.toString())
                val amenitiesBody = RequestBody.create("text/plain".toMediaTypeOrNull(), amenities.joinToString(","))
                val ratingBody = RequestBody.create("text/plain".toMediaTypeOrNull(), rating.toString())
                val descriptionBody = RequestBody.create("text/plain".toMediaTypeOrNull(), description)
                val unavailableDatesBody = RequestBody.create("text/plain".toMediaTypeOrNull(), unavailableDates.joinToString(","))
                val response = apiService.createRoom(
                    "Bearer $token",
                    images,
                    roomNameBody,
                    roomTypeBody,
                    roomLocationBody,
                    priceBody,
                    amenitiesBody,
                    ratingBody,
                    descriptionBody,
                    unavailableDatesBody
                )
                if (response.isSuccessful) {
                    response.body()
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    suspend fun bookRoom(roomId: String, date: String, token: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.bookRoom("Bearer $token", mapOf("roomId" to roomId, "date" to date))
                response.isSuccessful
            } catch (e: Exception) {
                false
            }
        }
    }

    suspend fun getUserBookings(token: String): List<BookingData> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getUserBookings("Bearer $token")
                if (response.isSuccessful && response.body() != null) {
                    response.body()!!.mapNotNull { map ->
                        val roomId = map["roomId"] as? String ?: return@mapNotNull null
                        val roomName = map["roomName"] as? String ?: "-"
                        val date = map["date"] as? String ?: "-"
                        val user = map["user"] as? String ?: "-"
                        BookingData(roomId, roomName, date, user)
                    }
                } else {
                    emptyList()
                }
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    suspend fun getAllBookings(token: String, startDate: String? = null, endDate: String? = null): List<BookingData> {
        return withContext(Dispatchers.IO) {
            try {
                println("RoomRepository: Starting getAllBookings call")
                println("RoomRepository: Token: ${token.take(20)}...")
                println("RoomRepository: startDate: $startDate, endDate: $endDate")
                
                val response = apiService.getAllBookings("Bearer $token", startDate, endDate)
                println("RoomRepository: API Response code: ${response.code()}")
                println("RoomRepository: API Response body: ${response.body()}")
                
                if (response.isSuccessful && response.body() != null) {
                    val bookings = response.body()!!
                    println("RoomRepository: Raw bookings from API: $bookings")
                    
                    val parsedBookings = bookings.mapNotNull { map ->
                        println("RoomRepository: Processing booking map: $map")
                        
                        // Extract roomId - it might be a string or an object
                        val roomId = when (val roomIdValue = map["roomId"]) {
                            is String -> roomIdValue
                            is Map<*, *> -> roomIdValue["_id"]?.toString()
                            else -> null
                        }
                        
                        val roomName = map["roomName"]?.toString() ?: "-"
                        val date = map["date"]?.toString() ?: "-"
                        
                        // Extract user email - it might be a string or an object
                        val user = when (val userValue = map["userId"]) {
                            is String -> userValue
                            is Map<*, *> -> {
                                val email = userValue["email"]?.toString()
                                val name = userValue["name"]?.toString()
                                println("RoomRepository: userId is Map, email: $email, name: $name")
                                email ?: name ?: "-"
                            }
                            else -> {
                                println("RoomRepository: userId is neither String nor Map: $userValue")
                                "-"
                            }
                        }
                        
                        println("RoomRepository: Parsed booking: roomId=$roomId, roomName=$roomName, date=$date, user=$user")
                        
                        if (roomId != null) {
                            BookingData(roomId, roomName, date, user)
                        } else {
                            println("RoomRepository: Skipping booking due to null roomId")
                            null
                        }
                    }
                    
                    println("RoomRepository: Final parsed bookings: ${parsedBookings.size}")
                    parsedBookings
                } else {
                    println("RoomRepository: API call failed: ${response.code()} - ${response.errorBody()?.string()}")
                    emptyList()
                }
            } catch (e: Exception) {
                println("RoomRepository: Exception in getAllBookings: ${e.message}")
                e.printStackTrace()
                emptyList()
            }
        }
    }

    suspend fun cancelBooking(roomId: String, date: String, token: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.cancelBooking("Bearer $token", mapOf("roomId" to roomId, "date" to date))
                response.isSuccessful
            } catch (e: Exception) {
                false
            }
        }
    }
}

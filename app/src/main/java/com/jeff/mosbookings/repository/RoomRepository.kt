package com.jeff.mosbookings.repository

import android.util.Log
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
    private val TAG = "RoomRepository"

    suspend fun getRooms(): List<RoomData>? {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "🏠 GET /api/rooms - Fetching all rooms")
                val startTime = System.currentTimeMillis()
                
                val response = apiService.getRooms()
                val duration = System.currentTimeMillis() - startTime
                
                Log.d(TAG, "🏠 GET /api/rooms - Response received in ${duration}ms")
                Log.d(TAG, "🏠 GET /api/rooms - Status: ${response.code()}")
                
                if (response.isSuccessful) {
                    val rooms = response.body()
                    Log.d(TAG, "🏠 GET /api/rooms - Success: Retrieved ${rooms?.size ?: 0} rooms")
                    rooms?.forEachIndexed { index, room ->
                        Log.d(TAG, "🏠 Room $index: ${room.roomName} (${room.roomType}) - ${room.images.size} images")
                    }
                    rooms
                } else {
                    Log.e(TAG, "🏠 GET /api/rooms - Failed: ${response.code()} - ${response.errorBody()?.string()}")
                    null
                }
            } catch (e: Exception) {
                Log.e(TAG, "🏠 GET /api/rooms - Exception: ${e.message}", e)
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
                Log.d(TAG, "📤 POST /api/rooms - Creating new room: $roomName")
                Log.d(TAG, "📤 Room details: Type=$roomType, Location=$roomLocation, Price=$price, Rating=$rating")
                Log.d(TAG, "📤 Images: ${imageUris.size} files, Amenities: ${amenities.size} items")
                
                val startTime = System.currentTimeMillis()
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
                    Log.d(TAG, "📤 Processing image $idx: $fileName (${bytes.size} bytes)")
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
                
                val duration = System.currentTimeMillis() - startTime
                Log.d(TAG, "📤 POST /api/rooms - Response received in ${duration}ms")
                Log.d(TAG, "📤 POST /api/rooms - Status: ${response.code()}")
                
                if (response.isSuccessful) {
                    val room = response.body()
                    Log.d(TAG, "📤 POST /api/rooms - Success: Room created with ID ${room?.id}")
                    room
                } else {
                    Log.e(TAG, "📤 POST /api/rooms - Failed: ${response.code()} - ${response.errorBody()?.string()}")
                    null
                }
            } catch (e: Exception) {
                Log.e(TAG, "📤 POST /api/rooms - Exception: ${e.message}", e)
                null
            }
        }
    }

    suspend fun bookRoom(roomId: String, date: String, token: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "📅 POST /api/bookings/book - Booking room: $roomId for date: $date")
                val startTime = System.currentTimeMillis()
                
                val response = apiService.bookRoom("Bearer $token", mapOf("roomId" to roomId, "date" to date))
                val duration = System.currentTimeMillis() - startTime
                
                Log.d(TAG, "📅 POST /api/bookings/book - Response received in ${duration}ms")
                Log.d(TAG, "📅 POST /api/bookings/book - Status: ${response.code()}")
                
                if (response.isSuccessful) {
                    Log.d(TAG, "📅 POST /api/bookings/book - Success: Room booked successfully")
                    true
                } else {
                    Log.e(TAG, "📅 POST /api/bookings/book - Failed: ${response.code()} - ${response.errorBody()?.string()}")
                    false
                }
            } catch (e: Exception) {
                Log.e(TAG, "📅 POST /api/bookings/book - Exception: ${e.message}", e)
                false
            }
        }
    }

    suspend fun getUserBookings(token: String): List<BookingData> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "📋 GET /api/bookings/my - Fetching user bookings")
                Log.d(TAG, "📋 Token: ${token.take(20)}...")
                val startTime = System.currentTimeMillis()
                
                val response = apiService.getUserBookings("Bearer $token")
                val duration = System.currentTimeMillis() - startTime
                
                Log.d(TAG, "📋 GET /api/bookings/my - Response received in ${duration}ms")
                Log.d(TAG, "📋 GET /api/bookings/my - Status: ${response.code()}")
                
                if (response.isSuccessful && response.body() != null) {
                    val rawBookings = response.body()!!
                    Log.d(TAG, "📋 GET /api/bookings/my - Raw response: ${rawBookings.size} items")
                    
                    val bookings = rawBookings.mapNotNull { map ->
                        Log.d(TAG, "📋 Processing user booking: $map")
                        
                        // Extract roomId - it might be a string or an object
                        val roomId = when (val roomIdValue = map["roomId"]) {
                            is String -> roomIdValue
                            is Map<*, *> -> roomIdValue["_id"]?.toString()
                            else -> null
                        }
                        
                        val roomName = map["roomName"]?.toString() ?: "-"
                        val date = map["date"]?.toString() ?: "-"
                        val user = map["userId"]?.toString() ?: "-"
                        
                        Log.d(TAG, "📋 Parsed booking: roomId=$roomId, roomName=$roomName, date=$date, user=$user")
                        
                        if (roomId != null) {
                            BookingData(roomId, roomName, date, user)
                        } else {
                            Log.w(TAG, "📋 Skipping booking due to null roomId")
                            null
                        }
                    }
                    Log.d(TAG, "📋 GET /api/bookings/my - Success: Retrieved ${bookings.size} bookings")
                    bookings
                } else {
                    Log.e(TAG, "📋 GET /api/bookings/my - Failed: ${response.code()} - ${response.errorBody()?.string()}")
                    emptyList()
                }
            } catch (e: Exception) {
                Log.e(TAG, "📋 GET /api/bookings/my - Exception: ${e.message}", e)
                emptyList()
            }
        }
    }

    suspend fun getAllBookings(token: String, startDate: String? = null, endDate: String? = null): List<BookingData> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "📊 GET /api/bookings/all - Fetching all bookings")
                Log.d(TAG, "📊 Token: ${token.take(20)}...")
                Log.d(TAG, "📊 Filters: startDate=$startDate, endDate=$endDate")
                val startTime = System.currentTimeMillis()
                
                val response = apiService.getAllBookings("Bearer $token", startDate, endDate)
                val duration = System.currentTimeMillis() - startTime
                
                Log.d(TAG, "📊 GET /api/bookings/all - Response received in ${duration}ms")
                Log.d(TAG, "📊 GET /api/bookings/all - Status: ${response.code()}")
                
                if (response.isSuccessful && response.body() != null) {
                    val rawBookings = response.body()!!
                    Log.d(TAG, "📊 GET /api/bookings/all - Raw response: ${rawBookings.size} items")
                    
                    val parsedBookings = rawBookings.mapNotNull { map ->
                        Log.d(TAG, "📊 Processing booking: $map")
                        
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
                                Log.d(TAG, "📊 User details: email=$email, name=$name")
                                email ?: name ?: "-"
                            }
                            else -> {
                                Log.w(TAG, "📊 Unknown user format: $userValue")
                                "-"
                            }
                        }
                        
                        Log.d(TAG, "📊 Parsed booking: roomId=$roomId, roomName=$roomName, date=$date, user=$user")
                        
                        if (roomId != null) {
                            BookingData(roomId, roomName, date, user)
                        } else {
                            Log.w(TAG, "📊 Skipping booking due to null roomId")
                            null
                        }
                    }
                    
                    Log.d(TAG, "📊 GET /api/bookings/all - Success: Retrieved ${parsedBookings.size} bookings")
                    parsedBookings
                } else {
                    Log.e(TAG, "📊 GET /api/bookings/all - Failed: ${response.code()} - ${response.errorBody()?.string()}")
                    emptyList()
                }
            } catch (e: Exception) {
                Log.e(TAG, "📊 GET /api/bookings/all - Exception: ${e.message}", e)
                emptyList()
            }
        }
    }

    suspend fun cancelBooking(roomId: String, date: String, token: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "❌ POST /api/bookings/cancel - Cancelling booking: roomId=$roomId, date=$date")
                val startTime = System.currentTimeMillis()
                
                val response = apiService.cancelBooking("Bearer $token", mapOf("roomId" to roomId, "date" to date))
                val duration = System.currentTimeMillis() - startTime
                
                Log.d(TAG, "❌ POST /api/bookings/cancel - Response received in ${duration}ms")
                Log.d(TAG, "❌ POST /api/bookings/cancel - Status: ${response.code()}")
                
                if (response.isSuccessful) {
                    Log.d(TAG, "❌ POST /api/bookings/cancel - Success: Booking cancelled")
                    true
                } else {
                    Log.e(TAG, "❌ POST /api/bookings/cancel - Failed: ${response.code()} - ${response.errorBody()?.string()}")
                    false
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ POST /api/bookings/cancel - Exception: ${e.message}", e)
                false
            }
        }
    }
}

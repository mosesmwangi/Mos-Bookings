package com.jeff.mosbookings.models

// Updated RoomData model for richer room info and date-based availability
data class RoomData(
    val id: String,
    val images: List<String>, // List of image URIs/URLs
    val roomName: String,
    val roomType: String, // e.g., Single, Double, Suite
    val roomLocation: String,
    val price: Double,
    val amenities: List<String>, // e.g., Wifi, TV, AC
    val rating: Float, // 1.0 - 5.0
    val description: String,
    val unavailableDates: List<String>, // e.g., ["2024-07-10", "2024-07-11"]
)
package com.jeff.mosbookings.models

data class RoomData(
    val roomImage: String?,
    val roomName: String,
    val roomLocation: String,
    val roomPrice: String,
    val roomAvailability: Boolean,
    val roomDescription: String,
    val checkinTime: String,
    val checkOutTime: String,
    val checkinDate: String,
    val checkoutDate: String
)
package com.jeff.mosbookings.models

data class BookingData(
    val roomId: String,
    val roomName: String,
    val date: String,
    val user: String // For now, can be a dummy user or device id
)

object SharedData {
    val roomsList = arrayListOf(
        RoomData(
            id = "1",
            images = listOf("android.resource://com.jeff.mosbookings/drawable/hotel", "android.resource://com.jeff.mosbookings/drawable/room1"),
            roomName = "Deluxe Suite",
            roomType = "Suite",
            roomLocation = "Nairobi, CBD",
            price = 12000.0,
            amenities = listOf("WiFi", "TV", "AC", "Breakfast"),
            rating = 4.8f,
            description = "Spacious suite with city view, king bed, and modern amenities.",
            unavailableDates = listOf("2024-07-10", "2024-07-11", "2024-07-15")
        ),
        RoomData(
            id = "2",
            images = listOf("android.resource://com.jeff.mosbookings/drawable/hotel", "android.resource://com.jeff.mosbookings/drawable/room2"),
            roomName = "Standard Room",
            roomType = "Single",
            roomLocation = "Nairobi, Westlands",
            price = 7000.0,
            amenities = listOf("WiFi", "TV"),
            rating = 4.2f,
            description = "Comfortable single room ideal for business travelers.",
            unavailableDates = listOf("2024-07-12", "2024-07-13")
        ),
        RoomData(
            id = "3",
            images = listOf("android.resource://com.jeff.mosbookings/drawable/hotel", "android.resource://com.jeff.mosbookings/drawable/room3"),
            roomName = "Family Room",
            roomType = "Double",
            roomLocation = "Nairobi, Kilimani",
            price = 9500.0,
            amenities = listOf("WiFi", "TV", "AC", "Kitchenette"),
            rating = 4.5f,
            description = "Spacious room for families, includes kitchenette and two double beds.",
            unavailableDates = listOf("2024-07-14")
        )
    )
    val bookingsList = arrayListOf<BookingData>()
}
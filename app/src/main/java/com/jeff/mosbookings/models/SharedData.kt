package com.jeff.mosbookings.models

object SharedData {
    val roomsList = arrayListOf(
        RoomData(
            roomImage = null,
            roomName = "Banana Suite",
            roomLocation = "Nairobi",
            roomPrice = "3000",
            roomAvailability = true,
            roomDescription = "Spacious room with king bed and city view",
            checkinTime = "12:00 PM",
            checkOutTime = "10:00 AM",
            checkinDate = "2025-08-01",
            checkoutDate = "2025-08-02"
        ),
        RoomData(
            roomImage = null,
            roomName = "Mango Deluxe",
            roomLocation = "Mombasa",
            roomPrice = "2500",
            roomAvailability = false,
            roomDescription = "Cozy beachside room",
            checkinTime = "1:00 PM",
            checkOutTime = "11:00 AM",
            checkinDate = "2025-08-03",
            checkoutDate = "2025-08-04"
        )
    )
}
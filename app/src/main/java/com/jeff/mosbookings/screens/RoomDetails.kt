package com.jeff.mosbookings.screens

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.jeff.mosbookings.databinding.ActivityRoomDetailsBinding

class RoomDetails : AppCompatActivity() {
    private lateinit var binding: ActivityRoomDetailsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRoomDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val image = intent.getStringExtra("roomImage")
        val name = intent.getStringExtra("roomName")
        val location = intent.getStringExtra("roomLocation")
        val price = intent.getStringExtra("roomPrice")
        val availability = intent.getBooleanExtra("roomAvailability", true)
        val description = intent.getStringExtra("roomDescription")
        val checkinTime = intent.getStringExtra("checkinTime")
        val checkoutTime = intent.getStringExtra("checkOutTime")
        val checkinDate = intent.getStringExtra("checkinDate")
        val checkoutDate = intent.getStringExtra("checkoutDate")

//        Glide.with(this).load(image).into(binding.roomImageView)
//        binding.roomName.text = name
//        binding.roomLocation.text = location
//        binding.roomPrice.text = "Ksh $price"
//        binding.roomAvailability.text = if (availability) "Available" else "Booked"
//        binding.roomDescription.text = description
//        binding.checkinTime.text = "Check-in: $checkinDate at $checkinTime"
//        binding.checkoutTime.text = "Check-out: $checkoutDate at $checkoutTime"
    }
}
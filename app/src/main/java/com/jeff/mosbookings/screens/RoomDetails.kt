package com.jeff.mosbookings.screens

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.jeff.mosbookings.R
import com.jeff.mosbookings.databinding.ActivityRoomDetailsBinding


class RoomDetails : AppCompatActivity() {
    private lateinit var binding: ActivityRoomDetailsBinding

    @SuppressLint("UseCompatLoadingForDrawables")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRoomDetailsBinding.inflate(layoutInflater)
        supportActionBar?.hide()
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(0, systemBars.top, 0, systemBars.bottom)
            insets
        }

        // Get intent extras
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

        // Bind data to UI
        Glide.with(this)
            .load(image)
            .placeholder(R.drawable.placeholder_room)
            .error(R.drawable.placeholder_room)
            .into(binding.roomImage)

        binding.roomName.setText(name ?: "N/A")
        binding.roomLocation.setText(location ?: "N/A")
        binding.roomPrice.setText(price?.let { "Ksh $it" } ?: "N/A")
        binding.roomAvailability.text = if (availability) "Available" else "Booked"
        binding.roomAvailability.background = getDrawable(
            if (availability) R.drawable.availability_tag_background
            else R.drawable.availability_tag_background_booked
        )
        binding.roomDescription.setText(description ?: "N/A")
        binding.checkinTime.setText(
            if (checkinDate != null && checkinTime != null) "Check-in: $checkinDate at $checkinTime"
            else "N/A"
        )
        binding.checkoutTime.setText(
            if (checkoutDate != null && checkoutTime != null) "Check-out: $checkoutDate at $checkoutTime"
            else "N/A"
        )
        binding.checkinDate.setText(checkinDate ?: "N/A")
        binding.checkoutDate.setText(checkoutDate ?: "N/A")

        // Handle back icon click
        binding.backIcon.setOnClickListener {
            finish()
        }
    }
}
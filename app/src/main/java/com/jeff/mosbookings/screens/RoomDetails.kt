package com.jeff.mosbookings.screens

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.jeff.mosbookings.R
import com.jeff.mosbookings.adapters.ImageSliderAdapter
import com.jeff.mosbookings.databinding.ActivityRoomDetailsBinding
import com.jeff.mosbookings.models.SharedData
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import com.jeff.mosbookings.repository.RoomRepository
import android.widget.Toast
import com.jeff.mosbookings.models.RoomData
import android.os.Handler
import android.os.Looper
import com.google.android.material.tabs.TabLayoutMediator

class RoomDetails : AppCompatActivity() {
    private lateinit var binding: ActivityRoomDetailsBinding
    private lateinit var imageSliderAdapter: ImageSliderAdapter
    private val handler = Handler(Looper.getMainLooper())
    private var currentRoom: RoomData? = null

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Toast.makeText(this, "RoomDetails opened", Toast.LENGTH_SHORT).show()
        binding = ActivityRoomDetailsBinding.inflate(layoutInflater)
        supportActionBar?.hide()
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(0, systemBars.top, 0, systemBars.bottom)
            insets
        }

        // Get roomId from intent
        val roomId = intent.getStringExtra("roomId")
        Toast.makeText(this, "RoomDetails opened for ID: $roomId", Toast.LENGTH_SHORT).show()
        if (roomId == null) {
            Toast.makeText(this, "No room ID provided", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Show loading state
        binding.roomName.setText("Loading...")
        binding.roomLocation.setText("")
        binding.roomPrice.setText("")
        binding.roomAvailability.text = ""
        binding.roomDescription.setText("")
        binding.roomType.setText("")
        binding.roomRating.setText("")
        binding.roomAmenities.setText("")
        binding.checkinDate.setText("")

        val roomRepository = RoomRepository()
        GlobalScope.launch {
            val rooms = roomRepository.getRooms()
            runOnUiThread {
                Toast.makeText(this@RoomDetails, "Fetched rooms: ${rooms?.size ?: 0}", Toast.LENGTH_SHORT).show()
            }
            val room = rooms?.find { it.id == roomId }
            runOnUiThread {
                if (room == null) {
                    Toast.makeText(this@RoomDetails, "Room not found for ID: $roomId", Toast.LENGTH_SHORT).show()
                    finish()
                    return@runOnUiThread
                } else {
                    Toast.makeText(this@RoomDetails, "Room found: ${room.roomName}", Toast.LENGTH_SHORT).show()
                }
                
                currentRoom = room
                setupImageSlider(room.images)
                displayRoomDetails(room)
            }
        }

        // Handle booking
        binding.bookRoomButton.setOnClickListener {
            val currentRoom = currentRoom
            if (currentRoom == null) {
                Toast.makeText(this, "Room data not loaded", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            val picker = com.google.android.material.datepicker.MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select booking date")
                .setSelection(com.google.android.material.datepicker.MaterialDatePicker.todayInUtcMilliseconds())
                .build()
            
            // Disable booked dates
            val unavailableDates = currentRoom.unavailableDates.mapNotNull { dateStr ->
                try {
                    val date = java.time.LocalDate.parse(dateStr)
                    date.toEpochDay() * 24 * 60 * 60 * 1000 // Convert to milliseconds
                } catch (e: Exception) {
                    null
                }
            }
            
            picker.addOnPositiveButtonClickListener { selection ->
                val selectedDate = java.time.Instant.ofEpochMilli(selection).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                val formatted = selectedDate.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                val today = java.time.LocalDate.now()
                
                if (selectedDate.isBefore(today)) {
                    com.google.android.material.snackbar.Snackbar.make(binding.root, "Cannot book for a past date", com.google.android.material.snackbar.Snackbar.LENGTH_LONG).show()
                    return@addOnPositiveButtonClickListener
                }
                
                if (currentRoom.unavailableDates.contains(formatted)) {
                    com.google.android.material.snackbar.Snackbar.make(binding.root, "Room unavailable on $formatted", com.google.android.material.snackbar.Snackbar.LENGTH_LONG).show()
                } else {
                    val prefs = getSharedPreferences("auth", android.content.Context.MODE_PRIVATE)
                    val token = prefs.getString("jwt", null)
                    if (token == null) {
                        com.google.android.material.snackbar.Snackbar.make(binding.root, "You must be logged in to book", com.google.android.material.snackbar.Snackbar.LENGTH_LONG).show()
                        return@addOnPositiveButtonClickListener
                    }
                    
                    // Show loading state
                    binding.bookRoomButton.isEnabled = false
                    binding.bookRoomButton.text = "Booking..."
                    
                    GlobalScope.launch {
                        val success = roomRepository.bookRoom(currentRoom.id, formatted, token)
                        runOnUiThread {
                            binding.bookRoomButton.isEnabled = true
                            binding.bookRoomButton.text = "Book Room"
                            
                            if (success) {
                                com.google.android.material.snackbar.Snackbar.make(binding.root, "Room booked for $formatted!", com.google.android.material.snackbar.Snackbar.LENGTH_LONG).show()
                                // Refresh room details to show updated unavailable dates
                                refreshRoomDetails(roomId)
                            } else {
                                com.google.android.material.snackbar.Snackbar.make(binding.root, "Failed to book room. Try again.", com.google.android.material.snackbar.Snackbar.LENGTH_LONG).show()
                            }
                        }
                    }
                }
            }
            picker.show(supportFragmentManager, picker.toString())
        }

        // Handle back icon click
        binding.backIcon.setOnClickListener {
            finish()
        }
    }

    private fun setupImageSlider(images: List<String>) {
        println("Setting up image slider with ${images.size} images")
        images.forEachIndexed { index, image ->
            println("Image $index: $image")
        }
        
        // Filter out empty or invalid images
        val validImages = images.filter { it.isNotBlank() && it != "placeholder" }
        println("Valid images: ${validImages.size}")
        validImages.forEachIndexed { index, image ->
            println("Valid image $index: $image")
        }
        
        if (validImages.isEmpty()) {
            // If no valid images, use placeholder drawable
            println("No valid images found, using placeholder")
            imageSliderAdapter = ImageSliderAdapter(listOf("placeholder"))
        } else {
            println("Creating adapter with ${validImages.size} valid images")
            imageSliderAdapter = ImageSliderAdapter(validImages)
        }
        
        binding.imageViewPager.adapter = imageSliderAdapter
        
        // Disable user interaction - only auto-slide
        binding.imageViewPager.isUserInputEnabled = false
        
        // Setup page indicator
        if (validImages.size > 1) {
            TabLayoutMediator(binding.pageIndicator, binding.imageViewPager) { _, _ -> }.attach()
            
            // Auto-slide every 3 seconds
            val runnable = object : Runnable {
                override fun run() {
                    val currentItem = binding.imageViewPager.currentItem
                    val itemCount = imageSliderAdapter.itemCount
                    if (itemCount > 1) {
                        binding.imageViewPager.currentItem = (currentItem + 1) % itemCount
                    }
                    handler.postDelayed(this, 3000)
                }
            }
            handler.postDelayed(runnable, 3000)
        } else {
            binding.pageIndicator.visibility = android.view.View.GONE
        }
    }

    private fun displayRoomDetails(room: RoomData) {
        binding.roomName.setText(room.roomName)
        binding.roomLocation.setText(room.roomLocation)
        binding.roomPrice.setText("Ksh ${room.price.toInt()}")
        binding.roomAvailability.text = getAvailabilityText(room)
        binding.roomAvailability.background = getDrawable(
            if (room.unavailableDates.contains(java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"))))
                R.drawable.availability_tag_background_booked
            else R.drawable.availability_tag_background
        )
        binding.roomDescription.setText(room.description)
        binding.roomType.setText(room.roomType)
        binding.roomRating.setText("★ ${room.rating}")
        binding.roomAmenities.setText(room.amenities.joinToString(", "))
        binding.checkinDate.setText("Unavailable: " + room.unavailableDates.joinToString(", "))
    }

    private fun getAvailabilityText(room: com.jeff.mosbookings.models.RoomData): String {
        val today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        return if (room.unavailableDates.contains(today)) "Booked today" else "Available"
    }
    
    private fun refreshRoomDetails(roomId: String) {
        val roomRepository = RoomRepository()
        GlobalScope.launch {
            val rooms = roomRepository.getRooms()
            val room = rooms?.find { it.id == roomId }
            runOnUiThread {
                if (room != null) {
                    currentRoom = room
                    binding.roomAvailability.text = getAvailabilityText(room)
                    binding.roomAvailability.background = getDrawable(
                        if (room.unavailableDates.contains(java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"))))
                            R.drawable.availability_tag_background_booked
                        else R.drawable.availability_tag_background
                    )
                    binding.checkinDate.setText("Unavailable: " + room.unavailableDates.joinToString(", "))
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Remove callbacks to prevent memory leaks
        handler.removeCallbacksAndMessages(null)
    }
}
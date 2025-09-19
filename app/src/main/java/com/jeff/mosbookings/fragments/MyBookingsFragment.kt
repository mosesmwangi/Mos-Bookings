package com.jeff.mosbookings.fragments

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.jeff.mosbookings.R
import com.jeff.mosbookings.databinding.FragmentMyBookingsBinding
import com.jeff.mosbookings.databinding.ItemBookingCardBinding
import com.jeff.mosbookings.models.SharedData
import com.jeff.mosbookings.repository.RoomRepository
import kotlinx.coroutines.launch
import android.widget.Toast
import com.jeff.mosbookings.models.BookingData
import com.jeff.mosbookings.models.RoomData
import android.content.Intent
import com.jeff.mosbookings.screens.RoomDetails
import org.json.JSONObject
import android.util.Log

class MyBookingsFragment : Fragment() {
    private lateinit var binding: FragmentMyBookingsBinding
    private lateinit var bookingsAdapter: BookingsAdapter
    private val roomRepository = RoomRepository()
    private var allBookings: List<BookingData> = emptyList()
    private var filteredBookings: List<BookingData> = emptyList()
    private var allRoomMap: Map<String, RoomData> = emptyMap()
    private var currentFilter = "All Bookings"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMyBookingsBinding.inflate(inflater, container, false)
        loadUserProfile()
        setupRecyclerView()
        setupSearch()
        setupFilters()
        return binding.root
    }

    private fun loadUserProfile() {
        val prefs = requireContext().getSharedPreferences("auth", Context.MODE_PRIVATE)
        val userJson = prefs.getString("user", null)
        val token = prefs.getString("jwt", null)
        
        Log.d("MyBookingsFragment", "👤 Loading user profile - Token exists: ${token != null}")
        
        if (userJson != null && token != null) {
            try {
                val user = JSONObject(userJson)
                val userName = user.optString("name", "User")
                val userEmail = user.optString("email", "No email")
                val role = user.optString("role", "user").lowercase()
                
                Log.d("MyBookingsFragment", "👤 User: $userName, Role: $role")
                
                // Update the profile section with user data
                binding.userName.text = userName
                binding.userEmail.text = userEmail
                binding.userRole.text = when (role) {
                    "admin" -> "Administrator"
                    "user" -> "Regular User"
                    else -> "User ($role)"
                }
                
                // Set profile image
                binding.profileImage.setImageResource(R.drawable.ic_user)
                
                Log.d("MyBookingsFragment", "👤 Profile loaded: $userName ($userEmail)")
                
            } catch (e: Exception) {
                Log.e("MyBookingsFragment", "👤 Error parsing user data: ${e.message}")
                // Fallback to default values
                binding.userName.text = "User"
                binding.userEmail.text = "No email"
                binding.userRole.text = "User"
                binding.profileImage.setImageResource(R.drawable.ic_user)
            }
        } else {
            Log.w("MyBookingsFragment", "👤 No user data or token found")
            binding.userName.text = "Guest"
            binding.userEmail.text = "Please login"
            binding.userRole.text = "Guest"
            binding.profileImage.setImageResource(R.drawable.ic_user)
        }
    }

    private fun setupRecyclerView() {
        binding.loadingProgressBar.visibility = View.VISIBLE
        val prefs = requireContext().getSharedPreferences("auth", android.content.Context.MODE_PRIVATE)
        val token = prefs.getString("jwt", null)
        if (token == null) {
            bookingsAdapter = BookingsAdapter(emptyList(), emptyMap(), token, roomRepository)
            binding.roomsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
            binding.roomsRecyclerView.adapter = bookingsAdapter
            binding.loadingProgressBar.visibility = View.GONE
            return
        }
        
        lifecycleScope.launch {
            try {
                val myBookings = roomRepository.getUserBookings(token)
                val rooms = roomRepository.getRooms()
                val roomMap = rooms?.associateBy { it.id } ?: emptyMap()
                
                allBookings = myBookings
                filteredBookings = myBookings
                allRoomMap = roomMap
                
                if (isAdded && view != null) {
                    applyFilter(currentFilter)
                    if (filteredBookings.isNotEmpty()) {
                        bookingsAdapter = BookingsAdapter(filteredBookings, roomMap, token, roomRepository)
                        binding.roomsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
                        binding.roomsRecyclerView.adapter = bookingsAdapter
                        showContent()
                    } else {
                        showEmptyState("No Bookings Found", "You haven't made any bookings yet. Start exploring our rooms!")
                    }
                    binding.loadingProgressBar.visibility = View.GONE
                }
            } catch (e: Exception) {
                if (isAdded && view != null) {
                    Toast.makeText(requireContext(), "Error loading bookings: ${e.message}", Toast.LENGTH_SHORT).show()
                    binding.loadingProgressBar.visibility = View.GONE
                }
            }
        }
    }

    private fun setupSearch() {
        binding.searchEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) = Unit
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString().lowercase()
                val searchFiltered = filteredBookings.filter {
                    it.roomName.lowercase().contains(query) ||
                    it.date.lowercase().contains(query)
                }
                bookingsAdapter.updateBookings(searchFiltered, allRoomMap)
            }
        })
    }

    class BookingsAdapter(
        private var bookings: List<BookingData>,
        private var roomMap: Map<String, RoomData>,
        private val token: String?,
        private val roomRepository: RoomRepository
    ) : RecyclerView.Adapter<BookingsAdapter.BookingViewHolder>() {
        
        fun updateBookings(newBookings: List<BookingData>, newRoomMap: Map<String, RoomData>) {
            bookings = newBookings
            roomMap = newRoomMap
            notifyDataSetChanged()
        }
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookingViewHolder {
            val binding = ItemBookingCardBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            return BookingViewHolder(binding)
        }
        
        override fun onBindViewHolder(holder: BookingViewHolder, position: Int) {
            val booking = bookings[position]
            val room = roomMap[booking.roomId]
            
            holder.binding.apply {
                // Set booking date
                bookingDate.text = "Booked for: ${booking.date}"
                
                if (room != null) {
                    // Set room details
                    roomName.text = room.roomName
                    roomPrice.text = "Ksh ${room.price.toInt()}"
                    roomType.text = room.roomType
                    roomRating.text = "★ ${room.rating}"
                    roomLocation.text = room.roomLocation
                    roomAmenities.text = room.amenities.joinToString(", ")
                    
                    // Load room image with optimizations
                    Glide.with(roomImage.context)
                        .load(room.images.firstOrNull())
                        .placeholder(R.drawable.placeholder_room)
                        .error(R.drawable.placeholder_room)
                        .centerCrop()
                        .override(400, 300) // Resize for better performance
                        .thumbnail(0.1f) // Show thumbnail while loading
                        .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.ALL)
                        .into(roomImage)
                } else {
                    // Fallback if room not found
                    roomName.text = booking.roomName
                    roomPrice.text = "Price not available"
                    roomType.text = "Type not available"
                    roomRating.text = "Rating not available"
                    roomLocation.text = "Location not available"
                    roomAmenities.text = "Amenities not available"
                    
                    // Set default image
                    roomImage.setImageResource(R.drawable.placeholder_room)
                }

                // Add click listener for room details
                root.setOnClickListener {
                    if (room != null) {
                        val intent = Intent(holder.itemView.context, RoomDetails::class.java).apply {
                            putExtra("roomId", room.id)
                        }
                        holder.itemView.context.startActivity(intent)
                    }
                }

                // Add cancel booking functionality
                cancelButton.setOnClickListener {
                    token?.let { authToken ->
                        // Use the fragment's lifecycle scope for the coroutine
                        (holder.itemView.context as? androidx.fragment.app.FragmentActivity)?.let { activity ->
                            val fragment = activity.supportFragmentManager.fragments.find { it is MyBookingsFragment } as? MyBookingsFragment
                            fragment?.lifecycleScope?.launch {
                                try {
                                    val success = roomRepository.cancelBooking(booking.roomId, booking.date, authToken)
                                    if (fragment.isAdded && fragment.view != null) {
                                        if (success) {
                                            Toast.makeText(holder.itemView.context, "Booking cancelled successfully", Toast.LENGTH_SHORT).show()
                                            // Remove the booking from the list
                                            val updatedBookings = bookings.toMutableList()
                                            updatedBookings.removeAt(position)
                                            updateBookings(updatedBookings, roomMap)
                                        } else {
                                            Toast.makeText(holder.itemView.context, "Failed to cancel booking", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                } catch (e: Exception) {
                                    if (fragment.isAdded && fragment.view != null) {
                                        Toast.makeText(holder.itemView.context, "Error cancelling booking: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        
        override fun getItemCount() = bookings.size
        
        class BookingViewHolder(val binding: ItemBookingCardBinding) : RecyclerView.ViewHolder(binding.root)
    }

    private fun setupFilters() {
        binding.chipAllBookings.setOnClickListener {
            selectFilter("All Bookings", binding.chipAllBookings)
            applyFilter("All Bookings")
        }
        binding.chipSuites.setOnClickListener {
            selectFilter("Suites", binding.chipSuites)
            applyFilter("Suites")
        }
        binding.chipEnSuite.setOnClickListener {
            selectFilter("En Suite", binding.chipEnSuite)
            applyFilter("En Suite")
        }
        binding.chipOneBedroom.setOnClickListener {
            selectFilter("One Bedroom", binding.chipOneBedroom)
            applyFilter("One Bedroom")
        }
        binding.chipTwoBedrooms.setOnClickListener {
            selectFilter("Two Bedrooms", binding.chipTwoBedrooms)
            applyFilter("Two Bedrooms")
        }
        binding.chipThreeBedrooms.setOnClickListener {
            selectFilter("Three Bedrooms", binding.chipThreeBedrooms)
            applyFilter("Three Bedrooms")
        }
        binding.chipConference.setOnClickListener {
            selectFilter("Conference", binding.chipConference)
            applyFilter("Conference")
        }
        // Initially select "All Bookings"
        selectFilter("All Bookings", binding.chipAllBookings)
    }

    private fun selectFilter(filterName: String, selectedChip: com.google.android.material.chip.Chip) {
        resetAllChips()
        selectedChip.setTextColor(resources.getColor(android.R.color.white, null))
        selectedChip.setChipBackgroundColorResource(com.jeff.mosbookings.R.color.primary_blue)
        selectedChip.setChipStrokeWidth(0f)
        currentFilter = filterName
    }

    private fun resetAllChips() {
        val chips = listOf(
            binding.chipAllBookings, binding.chipSuites, binding.chipEnSuite,
            binding.chipOneBedroom, binding.chipTwoBedrooms, binding.chipThreeBedrooms, binding.chipConference
        )
        chips.forEach { chip ->
            chip.setTextColor(resources.getColor(com.jeff.mosbookings.R.color.text_primary, null))
            chip.setChipBackgroundColorResource(android.R.color.white)
            chip.setChipStrokeWidth(1f)
            chip.setChipStrokeColorResource(com.jeff.mosbookings.R.color.primary_blue_light)
        }
    }

    private fun applyFilter(filterType: String) {
        filteredBookings = when (filterType) {
            "All Bookings" -> allBookings
            "Suites" -> allBookings.filter { booking ->
                val room = allRoomMap[booking.roomId]
                room?.roomType?.lowercase()?.contains("suite") == true
            }
            "En Suite" -> allBookings.filter { booking ->
                val room = allRoomMap[booking.roomId]
                room?.roomType?.lowercase()?.contains("en suite") == true
            }
            "One Bedroom" -> allBookings.filter { booking ->
                val room = allRoomMap[booking.roomId]
                room?.roomType?.lowercase()?.let { roomType ->
                    roomType.contains("one bedroom") || roomType.contains("1 bedroom") || roomType.contains("single bedroom")
                } == true
            }
            "Two Bedrooms" -> allBookings.filter { booking ->
                val room = allRoomMap[booking.roomId]
                room?.roomType?.lowercase()?.let { roomType ->
                    roomType.contains("two bedroom") || roomType.contains("2 bedroom") || roomType.contains("double bedroom")
                } == true
            }
            "Three Bedrooms" -> allBookings.filter { booking ->
                val room = allRoomMap[booking.roomId]
                room?.roomType?.lowercase()?.let { roomType ->
                    roomType.contains("three bedroom") || roomType.contains("3 bedroom") || roomType.contains("triple bedroom")
                } == true
            }
            "Conference" -> allBookings.filter { booking ->
                val room = allRoomMap[booking.roomId]
                room?.roomType?.lowercase()?.contains("conference") == true ||
                room?.amenities?.any { amenity -> amenity.lowercase().contains("conference") } == true
            }
            else -> allBookings
        }
        
        // Update adapter with filtered bookings
        if (::bookingsAdapter.isInitialized) {
            bookingsAdapter.updateBookings(filteredBookings, allRoomMap)
        }
    }

    private fun showContent() {
        binding.roomsRecyclerView.visibility = View.VISIBLE
        val emptyState = binding.root.findViewById<View>(R.id.emptyState)
        emptyState.visibility = View.GONE
    }

    private fun showEmptyState(title: String, message: String) {
        binding.roomsRecyclerView.visibility = View.GONE
        val emptyState = binding.root.findViewById<View>(R.id.emptyState)
        emptyState.visibility = View.VISIBLE
        
        // Get references to empty state views
        val emptyStateTitle = binding.root.findViewById<android.widget.TextView>(R.id.emptyStateTitle)
        val emptyStateMessage = binding.root.findViewById<android.widget.TextView>(R.id.emptyStateMessage)
        val emptyStateAction = binding.root.findViewById<android.widget.Button>(R.id.emptyStateAction)
        
        // Update empty state text
        emptyStateTitle.text = title
        emptyStateMessage.text = message
        emptyStateAction.text = "Refresh"
        emptyStateAction.visibility = View.VISIBLE
        
        // Set refresh action
        emptyStateAction.setOnClickListener {
            setupRecyclerView()
        }
    }
}
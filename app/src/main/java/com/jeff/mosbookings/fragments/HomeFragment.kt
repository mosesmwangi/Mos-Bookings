package com.jeff.mosbookings.fragments

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.jeff.mosbookings.adapters.RoomsAdapter
import com.jeff.mosbookings.databinding.FragmentHomeBinding
import com.jeff.mosbookings.models.RoomData
import com.jeff.mosbookings.repository.RoomRepository
import com.jeff.mosbookings.screens.RoomDetails
import kotlinx.coroutines.launch
import org.json.JSONObject

class HomeFragment : Fragment() {

    private lateinit var binding: FragmentHomeBinding
    private lateinit var roomsAdapter: RoomsAdapter
    private lateinit var roomList: ArrayList<RoomData>
    private lateinit var filteredRoomList: ArrayList<RoomData>
    private val roomRepository = RoomRepository()
    private var currentFilter = "All Rooms"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        loadUserProfile()
        setupRecyclerView()
        setupFilters()
        fetchRooms()
        return binding.root
    }

    private fun fetchRooms() {
        binding.loadingProgressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            val rooms = roomRepository.getRooms()
            if (rooms != null) {
                roomList.clear()
                roomList.addAll(rooms)
                // Apply current filter when rooms are loaded
                applyFilter(currentFilter)
            } else {
                Toast.makeText(requireContext(), "Failed to fetch rooms", Toast.LENGTH_SHORT).show()
            }
            binding.loadingProgressBar.visibility = View.GONE
        }
    }

    private fun setupRecyclerView() {
        roomList = ArrayList()
        filteredRoomList = ArrayList()
        roomsAdapter = RoomsAdapter(
            filteredRoomList,
            onRoomClick = { room ->
                Toast.makeText(requireContext(), "Clicked: ${room.roomName}", Toast.LENGTH_SHORT).show()
                Toast.makeText(requireContext(), "Sending roomId: ${room.id}", Toast.LENGTH_SHORT).show()
                val intent = android.content.Intent(requireContext(), RoomDetails::class.java).apply {
                    putExtra("roomId", room.id)
                }
                startActivity(intent)
            }
        )
        binding.roomsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = roomsAdapter
        }
    }

    private fun loadUserProfile() {
        val prefs = requireContext().getSharedPreferences("auth", Context.MODE_PRIVATE)
        val userJson = prefs.getString("user", null)
        val token = prefs.getString("jwt", null)
        
        Log.d("HomeFragment", "🏠 Loading user profile for home screen - Token exists: ${token != null}")
        
        if (userJson != null && token != null) {
            try {
                val user = JSONObject(userJson)
                val userName = user.optString("name", "User")
                val userRole = user.optString("role", "user").lowercase()
                
                Log.d("HomeFragment", "🏠 User: $userName, Role: $userRole")
                
                // Update the username in the home screen
                binding.userName.text = "Welcome, $userName!"
                
                // Add a welcome message based on role
                when (userRole) {
                    "admin" -> {
                        binding.userName.text = "Welcome Admin, $userName!"
                        Log.d("HomeFragment", "🏠 Admin user detected in home screen")
                    }
                    "user" -> {
                        binding.userName.text = "Welcome, $userName!"
                        Log.d("HomeFragment", "🏠 Regular user detected in home screen")
                    }
                    else -> {
                        binding.userName.text = "Welcome, $userName!"
                    }
                }
                
            } catch (e: Exception) {
                Log.e("HomeFragment", "🏠 Error parsing user data: ${e.message}")
                binding.userName.text = "Welcome, User!"
            }
        } else {
            Log.w("HomeFragment", "🏠 No user data or token found")
            binding.userName.text = "Welcome, Guest!"
        }
    }
    
    private fun setupFilters() {
        // Set up filter chip click listeners
        binding.chipAllRooms.setOnClickListener { 
            selectFilter("All Rooms", binding.chipAllRooms)
            applyFilter("All Rooms")
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
        
        binding.chipConference.setOnClickListener { 
            selectFilter("Conference", binding.chipConference)
            applyFilter("Conference")
        }
        
        binding.chipTwoBedrooms.setOnClickListener { 
            selectFilter("Two Bedrooms", binding.chipTwoBedrooms)
            applyFilter("Two Bedrooms")
        }
        
        binding.chipThreeBedrooms.setOnClickListener { 
            selectFilter("Three Bedrooms", binding.chipThreeBedrooms)
            applyFilter("Three Bedrooms")
        }
    }
    
    private fun selectFilter(filterName: String, selectedChip: com.google.android.material.chip.Chip) {
        // Reset all chips to unselected state
        resetAllChips()
        
        // Set selected chip to active state
        selectedChip.setTextColor(resources.getColor(android.R.color.white, null))
        selectedChip.setChipBackgroundColorResource(com.jeff.mosbookings.R.color.primary_blue)
        selectedChip.setChipStrokeWidth(0f)
        
        currentFilter = filterName
        Log.d("HomeFragment", "🔍 Filter selected: $filterName")
    }
    
    private fun resetAllChips() {
        val chips = listOf(
            binding.chipAllRooms,
            binding.chipSuites,
            binding.chipEnSuite,
            binding.chipOneBedroom,
            binding.chipConference,
            binding.chipTwoBedrooms,
            binding.chipThreeBedrooms
        )
        
        chips.forEach { chip ->
            chip.setTextColor(resources.getColor(com.jeff.mosbookings.R.color.text_primary, null))
            chip.setChipBackgroundColorResource(android.R.color.white)
            chip.setChipStrokeWidth(1f)
            chip.setChipStrokeColorResource(com.jeff.mosbookings.R.color.primary_blue_light)
        }
    }
    
    private fun applyFilter(filterType: String) {
        filteredRoomList.clear()
        
        when (filterType) {
            "All Rooms" -> {
                filteredRoomList.addAll(roomList)
            }
            "Suites" -> {
                filteredRoomList.addAll(roomList.filter { 
                    it.roomType.lowercase().contains("suite") 
                })
            }
            "En Suite" -> {
                filteredRoomList.addAll(roomList.filter { 
                    it.roomType.lowercase().contains("en suite") 
                })
            }
            "One Bedroom" -> {
                filteredRoomList.addAll(roomList.filter { 
                    it.roomType.lowercase().contains("one bedroom") || 
                    it.roomType.lowercase().contains("1 bedroom") ||
                    it.roomType.lowercase().contains("single bedroom")
                })
            }
            "Conference" -> {
                filteredRoomList.addAll(roomList.filter { 
                    it.roomType.lowercase().contains("conference") ||
                    it.amenities.any { amenity -> 
                        amenity.lowercase().contains("conference") 
                    }
                })
            }
            "Two Bedrooms" -> {
                filteredRoomList.addAll(roomList.filter { 
                    it.roomType.lowercase().contains("two bedroom") || 
                    it.roomType.lowercase().contains("2 bedroom") ||
                    it.roomType.lowercase().contains("double bedroom")
                })
            }
            "Three Bedrooms" -> {
                filteredRoomList.addAll(roomList.filter { 
                    it.roomType.lowercase().contains("three bedroom") || 
                    it.roomType.lowercase().contains("3 bedroom") ||
                    it.roomType.lowercase().contains("triple bedroom")
                })
            }
        }
        
        roomsAdapter.notifyDataSetChanged()
        Log.d("HomeFragment", "🔍 Filter applied: $filterType - ${filteredRoomList.size} rooms found")
        
        // Show message if no rooms found
        if (filteredRoomList.isEmpty()) {
            Toast.makeText(requireContext(), "No rooms found for $filterType", Toast.LENGTH_SHORT).show()
        }
    }
}
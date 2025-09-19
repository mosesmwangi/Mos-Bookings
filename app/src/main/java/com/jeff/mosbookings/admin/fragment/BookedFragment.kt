package com.jeff.mosbookings.admin.fragment

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.jeff.mosbookings.databinding.FragmentBookedBinding
import com.jeff.mosbookings.models.RoomData
import com.jeff.mosbookings.repository.RoomRepository
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import com.jeff.mosbookings.models.BookingData
import com.google.android.material.datepicker.MaterialDatePicker
import java.text.SimpleDateFormat
import androidx.recyclerview.widget.RecyclerView
import com.jeff.mosbookings.databinding.ItemAdminBookingBinding

class BookedFragment : Fragment() {

    private lateinit var binding: FragmentBookedBinding
    private lateinit var bookingsAdapter: BookingsAdapter
    private var bookingsList: List<BookingData> = emptyList()
    private val roomRepository = RoomRepository()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentBookedBinding.inflate(inflater, container, false)

        setupRecyclerView()
        setupSearch()
        setupDateFilter()

        // Start loading immediately
        fetchBookings()

        return binding.root
    }

    private fun fetchBookings(startDate: String? = null, endDate: String? = null) {
        val prefs = requireContext().getSharedPreferences("admin_auth", android.content.Context.MODE_PRIVATE)
        val token = prefs.getString("admin_jwt", null)
        if (token == null) {
            Toast.makeText(requireContext(), "Not authenticated as admin", Toast.LENGTH_SHORT).show()
            return
        }
        
        println("Starting to fetch bookings...")
        Toast.makeText(requireContext(), "Fetching bookings...", Toast.LENGTH_SHORT).show()
        
        // Show loading, hide RecyclerView
        binding.loadingProgressBar.visibility = View.VISIBLE
        binding.roomsRecyclerView.visibility = View.GONE
        
        lifecycleScope.launch {
            try {
                println("Calling getAllBookings API...")
                Toast.makeText(requireContext(), "Calling getAllBookings API...", Toast.LENGTH_SHORT).show()
                val bookings = roomRepository.getAllBookings(token, startDate, endDate)
                println("Got ${bookings.size} bookings from API")
                Toast.makeText(requireContext(), "Got ${bookings.size} bookings from API", Toast.LENGTH_SHORT).show()
                
                println("Fetching rooms...")
                Toast.makeText(requireContext(), "Fetching rooms...", Toast.LENGTH_SHORT).show()
                val rooms = roomRepository.getRooms()
                println("Got ${rooms?.size ?: 0} rooms from API")
                Toast.makeText(requireContext(), "Got ${rooms?.size ?: 0} rooms from API", Toast.LENGTH_SHORT).show()
                
                val roomMap = rooms?.associateBy { it.id } ?: emptyMap()
                
                bookingsList = bookings
                bookingsAdapter.updateBookings(bookingsList, roomMap)
                
                // Debug: Print booking details
                bookings.forEach { booking ->
                    println("Booking: ${booking.roomName} on ${booking.date} by ${booking.user}")
                }
                
                println("Found ${bookings.size} bookings")
                Toast.makeText(requireContext(), "Found ${bookings.size} bookings", Toast.LENGTH_SHORT).show()
                
                // Hide loading, show RecyclerView
                binding.loadingProgressBar.visibility = View.GONE
                binding.roomsRecyclerView.visibility = View.VISIBLE
                
            } catch (e: Exception) {
                println("Error loading bookings: ${e.message}")
                e.printStackTrace()
                Toast.makeText(requireContext(), "Error loading bookings: ${e.message}", Toast.LENGTH_LONG).show()
                
                // Hide loading, show RecyclerView (even if empty)
                binding.loadingProgressBar.visibility = View.GONE
                binding.roomsRecyclerView.visibility = View.VISIBLE
            }
        }
    }

    private fun setupRecyclerView() {
        println("Setting up RecyclerView...")
        bookingsAdapter = BookingsAdapter(emptyList(), emptyMap())
        binding.roomsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.roomsRecyclerView.adapter = bookingsAdapter
    }

    private fun setupDateFilter() {
        binding.startDateEditText.setOnClickListener {
            val picker = MaterialDatePicker.Builder.datePicker().setTitleText("Select start date").build()
            picker.addOnPositiveButtonClickListener { selection ->
                val date = dateFormat.format(java.util.Date(selection))
                binding.startDateEditText.setText(date)
            }
            picker.show(parentFragmentManager, picker.toString())
        }
        binding.endDateEditText.setOnClickListener {
            val picker = MaterialDatePicker.Builder.datePicker().setTitleText("Select end date").build()
            picker.addOnPositiveButtonClickListener { selection ->
                val date = dateFormat.format(java.util.Date(selection))
                binding.endDateEditText.setText(date)
            }
            picker.show(parentFragmentManager, picker.toString())
        }
        binding.filterButton.setOnClickListener {
            val startDate = binding.startDateEditText.text?.toString()?.takeIf { it.isNotBlank() }
            val endDate = binding.endDateEditText.text?.toString()?.takeIf { it.isNotBlank() }
            fetchBookings(startDate, endDate)
        }
    }

    private fun setupSearch() {
        binding.searchEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) = Unit
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString().lowercase()
                val filtered = bookingsList.filter {
                    it.roomName.lowercase().contains(query) ||
                    it.user.lowercase().contains(query)
                }
                // Get the current roomMap from the adapter
                val currentRoomMap = bookingsAdapter.getRoomMap()
                bookingsAdapter.updateBookings(filtered, currentRoomMap)
            }
        })
    }

    class BookingsAdapter(
        private var bookings: List<BookingData>,
        private var roomMap: Map<String, RoomData>
    ) : RecyclerView.Adapter<BookingsAdapter.BookingViewHolder>() {
        
        fun updateBookings(newBookings: List<BookingData>, newRoomMap: Map<String, RoomData>) {
            println("Updating adapter with ${newBookings.size} bookings")
            bookings = newBookings
            roomMap = newRoomMap
            notifyDataSetChanged()
        }
        
        // Add a getter method to access roomMap
        fun getRoomMap(): Map<String, RoomData> = roomMap
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookingViewHolder {
            println("Creating ViewHolder...")
            val binding = ItemAdminBookingBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            return BookingViewHolder(binding)
        }
        
        override fun onBindViewHolder(holder: BookingViewHolder, position: Int) {
            val booking = bookings[position]
            val room = roomMap[booking.roomId]
            
            println("Binding booking at position $position: ${booking.roomName}")
            println("Booking user: ${booking.user}")
            
            holder.binding.apply {
                // Set booking information
                roomName.text = booking.roomName
                bookingDate.text = booking.date
                userEmail.text = booking.user
                println("Set userEmail text to: ${booking.user}")
                
                if (room != null) {
                    // Set room details
                    roomType.text = room.roomType
                    roomPrice.text = "Ksh ${room.price.toInt()}"
                    roomLocation.text = room.roomLocation
                    println("Room details set for: ${room.roomName}")
                } else {
                    // Fallback if room not found
                    roomType.text = "Type not available"
                    roomPrice.text = "Price not available"
                    roomLocation.text = "Location not available"
                    println("Room not found for booking: ${booking.roomName}")
                }
            }
        }

        override fun getItemCount() = bookings.size
        
        class BookingViewHolder(val binding: ItemAdminBookingBinding) : RecyclerView.ViewHolder(binding.root)
    }
}
package com.jeff.mosbookings.fragments

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.jeff.mosbookings.R
import com.jeff.mosbookings.databinding.FragmentMyBookingsBinding
import com.jeff.mosbookings.databinding.ItemBookingCardBinding
import com.jeff.mosbookings.models.SharedData
import com.jeff.mosbookings.repository.RoomRepository
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import android.widget.Toast
import com.jeff.mosbookings.models.BookingData
import com.jeff.mosbookings.models.RoomData
import android.content.Intent
import com.jeff.mosbookings.screens.RoomDetails

class MyBookingsFragment : Fragment() {
    private lateinit var binding: FragmentMyBookingsBinding
    private lateinit var bookingsAdapter: BookingsAdapter
    private val roomRepository = RoomRepository()
    private var allBookings: List<BookingData> = emptyList()
    private var allRoomMap: Map<String, RoomData> = emptyMap()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMyBookingsBinding.inflate(inflater, container, false)
        setupRecyclerView()
        setupSearch()
        return binding.root
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
        
        GlobalScope.launch {
            try {
                val myBookings = roomRepository.getUserBookings(token)
                val rooms = roomRepository.getRooms()
                val roomMap = rooms?.associateBy { it.id } ?: emptyMap()
                
                allBookings = myBookings
                allRoomMap = roomMap
                
                requireActivity().runOnUiThread {
                    bookingsAdapter = BookingsAdapter(myBookings, roomMap, token, roomRepository)
                    binding.roomsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
                    binding.roomsRecyclerView.adapter = bookingsAdapter
                    binding.loadingProgressBar.visibility = View.GONE
                }
            } catch (e: Exception) {
                requireActivity().runOnUiThread {
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
                val filtered = allBookings.filter {
                    it.roomName.lowercase().contains(query) ||
                    it.date.lowercase().contains(query)
                }
                bookingsAdapter.updateBookings(filtered, allRoomMap)
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
                    
                    // Load room image
                    Glide.with(roomImage.context)
                        .load(room.images.firstOrNull())
                        .placeholder(R.drawable.placeholder_room)
                        .error(R.drawable.placeholder_room)
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
                        GlobalScope.launch {
                            try {
                                val success = roomRepository.cancelBooking(booking.roomId, booking.date, authToken)
                                (holder.itemView.context as? android.app.Activity)?.runOnUiThread {
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
                                (holder.itemView.context as? android.app.Activity)?.runOnUiThread {
                                    Toast.makeText(holder.itemView.context, "Error cancelling booking: ${e.message}", Toast.LENGTH_SHORT).show()
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
}
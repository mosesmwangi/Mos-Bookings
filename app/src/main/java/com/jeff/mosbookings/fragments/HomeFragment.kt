package com.jeff.mosbookings.fragments

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.jeff.mosbookings.adapters.RoomsAdapter
import com.jeff.mosbookings.databinding.FragmentHomeBinding
import com.jeff.mosbookings.models.RoomData
import com.jeff.mosbookings.screens.RoomDetails


class HomeFragment : Fragment() {

    private lateinit var binding: FragmentHomeBinding
    private lateinit var roomsAdapter: RoomsAdapter
    private lateinit var roomsList: ArrayList<RoomData>
    private lateinit var filteredList: ArrayList<RoomData>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHomeBinding.inflate(inflater, container, false)

        setupRoomData()
        setupRecyclerView()
        setupSearch()

        return binding.root
    }

    private fun setupRoomData() {
        roomsList = arrayListOf(
            RoomData(
                roomImage = "https://images.unsplash.com/photo-1505691723518-36a5ac3be353",
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
                roomImage = "https://images.unsplash.com/photo-1570129477492-45c003edd2be",
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

        filteredList = ArrayList(roomsList)
    }

    private fun setupRecyclerView() {
        roomsAdapter = RoomsAdapter(filteredList) { room ->
            val intent = Intent(requireContext(), RoomDetails::class.java).apply {
                putExtra("roomImage", room.roomImage)
                putExtra("roomName", room.roomName)
                putExtra("roomLocation", room.roomLocation)
                putExtra("roomPrice", room.roomPrice)
                putExtra("roomAvailability", room.roomAvailability)
                putExtra("roomDescription", room.roomDescription)
                putExtra("checkinTime", room.checkinTime)
                putExtra("checkOutTime", room.checkOutTime)
                putExtra("checkinDate", room.checkinDate)
                putExtra("checkoutDate", room.checkoutDate)
            }
            startActivity(intent)
        }

        binding.roomsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = roomsAdapter
        }
    }

    private fun setupSearch() {
        binding.searchEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) = Unit
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString().lowercase()
                filteredList.clear()
                filteredList.addAll(roomsList.filter {
                    it.roomName.lowercase().contains(query) ||
                            it.roomLocation.lowercase().contains(query)
                })
                roomsAdapter.notifyDataSetChanged()
            }
        })
    }
}

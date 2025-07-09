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
import com.jeff.mosbookings.databinding.FragmentBookingsBinding
import com.jeff.mosbookings.models.RoomData
import com.jeff.mosbookings.models.SharedData
import com.jeff.mosbookings.screens.RoomDetails

class BookingsFragment : Fragment() {

    private lateinit var binding: FragmentBookingsBinding
    private lateinit var roomsAdapter: RoomsAdapter
    private lateinit var filteredList: ArrayList<RoomData>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentBookingsBinding.inflate(inflater, container, false)

        setupRoomData()
        setupRecyclerView()
        setupSearch()

        return binding.root
    }

    private fun setupRoomData() {
        filteredList = ArrayList(SharedData.roomsList.filter { !it.roomAvailability })
    }

    private fun setupRecyclerView() {
        roomsAdapter = RoomsAdapter(
            filteredList,
            onRoomClick = { room ->
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
            },
            onBookRoom = { room, newCheckinDate, newCheckinTime, newCheckoutDate, newCheckoutTime ->
                val index = SharedData.roomsList.indexOfFirst { it.roomName == room.roomName }
                if (index != -1) {
                    SharedData.roomsList[index] = room.copy(
                        roomAvailability = false,
                        checkinDate = newCheckinDate,
                        checkinTime = newCheckinTime,
                        checkoutDate = newCheckoutDate,
                        checkOutTime = newCheckoutTime
                    )
                    filteredList.clear()
                    filteredList.addAll(SharedData.roomsList.filter { !it.roomAvailability })
                    roomsAdapter.notifyDataSetChanged()
                }
            },
            onCancelBooking = { room ->
                val index = SharedData.roomsList.indexOfFirst { it.roomName == room.roomName }
                if (index != -1) {
                    SharedData.roomsList[index] = room.copy(roomAvailability = true)
                    filteredList.clear()
                    filteredList.addAll(SharedData.roomsList.filter { !it.roomAvailability })
                    roomsAdapter.notifyDataSetChanged()
                }
            }
        )

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
                filteredList.addAll(SharedData.roomsList.filter {
                    !it.roomAvailability && (
                            it.roomName.lowercase().contains(query) ||
                                    it.roomLocation.lowercase().contains(query)
                            )
                })
                roomsAdapter.notifyDataSetChanged()
            }
        })
    }
}
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
import java.time.LocalDate
import java.time.format.DateTimeFormatter

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
        val today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        filteredList = ArrayList(SharedData.roomsList.filter { it.unavailableDates.contains(today) })
    }

    private fun setupRecyclerView() {
        roomsAdapter = RoomsAdapter(
            filteredList,
            onRoomClick = { room ->
                val intent = Intent(requireContext(), RoomDetails::class.java).apply {
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

    private fun setupSearch() {
        binding.searchEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) = Unit
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString().lowercase()
                val today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                filteredList.clear()
                filteredList.addAll(SharedData.roomsList.filter {
                    it.unavailableDates.contains(today) && (
                        it.roomName.lowercase().contains(query) ||
                        it.roomLocation.lowercase().contains(query)
                    )
                })
                roomsAdapter.notifyDataSetChanged()
            }
        })
    }
}
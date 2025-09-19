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
import com.jeff.mosbookings.adapters.RoomsAdapter
import com.jeff.mosbookings.databinding.FragmentAvailableBinding
import com.jeff.mosbookings.models.RoomData
import com.jeff.mosbookings.repository.RoomRepository
import com.jeff.mosbookings.screens.RoomDetails
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class AvailableFragment : Fragment() {

    private lateinit var binding: FragmentAvailableBinding
    private lateinit var roomsAdapter: RoomsAdapter
    private lateinit var roomList: ArrayList<RoomData>
    private val roomRepository = RoomRepository()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAvailableBinding.inflate(inflater, container, false)

        setupRecyclerView()
        fetchRooms()
        setupSearch()

        return binding.root
    }

    private fun fetchRooms() {
        binding.loadingProgressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            val rooms = roomRepository.getRooms()
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            if (rooms != null) {
                roomList.clear()
                roomList.addAll(rooms.filter { !it.unavailableDates.contains(today) })
                roomsAdapter.notifyDataSetChanged()
            } else {
                Toast.makeText(requireContext(), "Failed to fetch rooms", Toast.LENGTH_SHORT).show()
            }
            binding.loadingProgressBar.visibility = View.GONE
        }
    }

    private fun setupRecyclerView() {
        roomList = ArrayList()
        roomsAdapter = RoomsAdapter(
            roomList,
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
                val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                val filtered = roomList.filter {
                    !it.unavailableDates.contains(today) && (
                        it.roomName.lowercase().contains(query) ||
                        it.roomLocation.lowercase().contains(query)
                    )
                }
                roomsAdapter.apply {
                    this@AvailableFragment.roomList.clear()
                    this@AvailableFragment.roomList.addAll(filtered)
                    notifyDataSetChanged()
                }
            }
        })
    }
}
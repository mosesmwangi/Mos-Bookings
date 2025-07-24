package com.jeff.mosbookings.fragments

import android.os.Bundle
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

class HomeFragment : Fragment() {

    private lateinit var binding: FragmentHomeBinding
    private lateinit var roomsAdapter: RoomsAdapter
    private lateinit var roomList: ArrayList<RoomData>
    private val roomRepository = RoomRepository()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        setupRecyclerView()
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
}
package com.jeff.mosbookings.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import com.bumptech.glide.Glide
import androidx.recyclerview.widget.RecyclerView
import com.jeff.mosbookings.databinding.RoomItemBinding
import com.jeff.mosbookings.models.RoomData

class RoomsAdapter(
    private val roomsList: ArrayList<RoomData>,
    private val onRoomClick: (RoomData) -> Unit
) : RecyclerView.Adapter<RoomsAdapter.RoomViewHolder>() {

    inner class RoomViewHolder(val binding: RoomItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(room: RoomData) {
            binding.apply {
                // Load Image
                Glide.with(roomImage.context)
                    .load(room.roomImage)
                    .into(roomImage)

                roomTitle.text = room.roomName
                roomLocation.text = room.roomLocation
                roomPrice.text = "Ksh ${room.roomPrice}"
                roomStatus.text = if (room.roomAvailability) "Available" else "Booked"

                val isAvailable = room.roomAvailability
                roomStatus.setTextColor(if (isAvailable) android.graphics.Color.BLUE else android.graphics.Color.RED)
                bookButton.text = if (isAvailable) "Book Now" else "Booked"
                bookButton.setBackgroundColor(if (isAvailable) android.graphics.Color.BLUE else android.graphics.Color.RED)
                bookButton.isEnabled = isAvailable

                root.setOnClickListener { onRoomClick(room) }
                bookButton.setOnClickListener { onRoomClick(room) }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RoomViewHolder {
        val binding = RoomItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RoomViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RoomViewHolder, position: Int) {
        holder.bind(roomsList[position])
    }

    override fun getItemCount(): Int = roomsList.size
}

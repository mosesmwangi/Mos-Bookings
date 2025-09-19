package com.jeff.mosbookings.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.jeff.mosbookings.R
import com.jeff.mosbookings.databinding.RoomItemBinding
import com.jeff.mosbookings.models.RoomData
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class RoomsAdapter(
    private val roomsList: ArrayList<RoomData>,
    private val onRoomClick: (RoomData) -> Unit
) : RecyclerView.Adapter<RoomsAdapter.RoomViewHolder>() {

    inner class RoomViewHolder(val binding: RoomItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(room: RoomData) {
            binding.apply {
                // Load first image with optimizations
                val imageUrl = room.images.firstOrNull()?.takeIf { it.isNotBlank() }
                Glide.with(roomImage.context)
                    .load(imageUrl)
                    .placeholder(R.drawable.placeholder_room)
                    .error(R.drawable.placeholder_room)
                    .centerCrop()
                    .override(400, 300) // Resize for better performance
                    .thumbnail(0.1f) // Show thumbnail while loading
                    .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.ALL)
                    .into(roomImage)

                roomTitle.text = room.roomName
                roomLocation.text = room.roomLocation
                roomPrice.text = "Ksh ${room.price.toInt()}"
                roomType.text = room.roomType
                roomRating.text = "★ ${room.rating}"

                // Amenities as comma-separated (for now)
                roomAmenities.text = room.amenities.joinToString(", ")

                // Availability by date
                val today = LocalDate.now()
                val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                val nextAvailable = (0..30).map { today.plusDays(it.toLong()).format(formatter) }
                    .firstOrNull { !room.unavailableDates.contains(it) }
                if (nextAvailable != null) {
                    roomStatus.text = "Next available: $nextAvailable"
                    roomStatus.setTextColor(android.graphics.Color.BLUE)
                } else {
                    roomStatus.text = "Fully booked"
                    roomStatus.setTextColor(android.graphics.Color.RED)
                }

                root.setOnClickListener { onRoomClick(room) }
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
package com.jeff.mosbookings.adapters

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Build
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import com.bumptech.glide.Glide
import androidx.recyclerview.widget.RecyclerView
import com.jeff.mosbookings.R
import com.jeff.mosbookings.databinding.BookingDialogBoxBinding
import com.jeff.mosbookings.databinding.RoomItemBinding
import com.jeff.mosbookings.models.RoomData
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class RoomsAdapter(
    private val roomsList: ArrayList<RoomData>,
    private val onRoomClick: (RoomData) -> Unit,
    private val onBookRoom: (RoomData, String, String, String, String) -> Unit,
    private val onCancelBooking: (RoomData) -> Unit
) : RecyclerView.Adapter<RoomsAdapter.RoomViewHolder>() {

    inner class RoomViewHolder(val binding: RoomItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        @RequiresApi(Build.VERSION_CODES.O)
        fun bind(room: RoomData) {
            binding.apply {
                // Load Image
                Glide.with(roomImage.context)
                    .load(room.roomImage?.let { android.net.Uri.parse(it) })
                    .placeholder(R.drawable.placeholder_room)
                    .into(roomImage)

                roomTitle.text = room.roomName
                roomLocation.text = room.roomLocation
                roomPrice.text = "Ksh ${room.roomPrice}"
                roomStatus.text = if (room.roomAvailability) "Available" else "Booked"

                val isAvailable = room.roomAvailability
                roomStatus.setTextColor(if (isAvailable) android.graphics.Color.BLUE else android.graphics.Color.RED)
                bookButton.text = if (isAvailable) "Book Now" else "Cancel Booking"
                bookButton.setBackgroundColor(if (isAvailable) android.graphics.Color.BLUE else android.graphics.Color.RED)
                bookButton.isEnabled = true

                root.setOnClickListener { onRoomClick(room) }
                bookButton.setOnClickListener {
                    if (isAvailable) {
                        showBookingDialog(room)
                    } else {
                        onCancelBooking(room)
                    }
                }
            }
        }

        @RequiresApi(Build.VERSION_CODES.O)
        private fun showBookingDialog(room: RoomData) {
            val dialogBinding = BookingDialogBoxBinding.inflate(LayoutInflater.from(binding.root.context))
            val dialog = AlertDialog.Builder(binding.root.context)
                .setTitle("Book ${room.roomName}")
                .setView(dialogBinding.root)
                .setNegativeButton("Cancel", null)
                .create()

            dialogBinding.checkinDateInput.setText(room.checkinDate)
            dialogBinding.checkinTimeInput.setText(room.checkinTime)
            dialogBinding.checkoutDateInput.setText(room.checkoutDate)
            dialogBinding.checkoutTimeInput.setText(room.checkOutTime)

            updateDuration(dialogBinding, room.checkinDate, room.checkoutDate)

            dialogBinding.checkinDateLayout.setEndIconOnClickListener {
                showDatePicker(dialogBinding.checkinDateInput) { date ->
                    updateDuration(dialogBinding, date, dialogBinding.checkoutDateInput.text.toString())
                }
            }
            dialogBinding.checkoutDateLayout.setEndIconOnClickListener {
                showDatePicker(dialogBinding.checkoutDateInput) { date ->
                    updateDuration(dialogBinding, dialogBinding.checkinDateInput.text.toString(), date)
                }
            }
            dialogBinding.checkinTimeLayout.setEndIconOnClickListener {
                showTimePicker(dialogBinding.checkinTimeInput)
            }
            dialogBinding.checkoutTimeLayout.setEndIconOnClickListener {
                showTimePicker(dialogBinding.checkoutTimeInput)
            }

            dialogBinding.bookRoomButton.setOnClickListener {
                val newCheckinDate = dialogBinding.checkinDateInput.text.toString()
                val newCheckinTime = dialogBinding.checkinTimeInput.text.toString()
                val newCheckoutDate = dialogBinding.checkoutDateInput.text.toString()
                val newCheckoutTime = dialogBinding.checkoutTimeInput.text.toString()
                onBookRoom(room, newCheckinDate, newCheckinTime, newCheckoutDate, newCheckoutTime)
                dialog.dismiss()
            }

            dialog.show()
        }

        private fun showDatePicker(editText: com.google.android.material.textfield.TextInputEditText, onDateSet: (String) -> Unit) {
            val context = binding.root.context
            val today = java.util.Calendar.getInstance()
            DatePickerDialog(
                context,
                { _, year, month, day ->
                    val date = String.format("%04d-%02d-%02d", year, month + 1, day)
                    editText.setText(date)
                    onDateSet(date)
                },
                today.get(java.util.Calendar.YEAR),
                today.get(java.util.Calendar.MONTH),
                today.get(java.util.Calendar.DAY_OF_MONTH)
            ).show()
        }

        private fun showTimePicker(editText: com.google.android.material.textfield.TextInputEditText) {
            val context = binding.root.context
            val now = java.util.Calendar.getInstance()
            TimePickerDialog(
                context,
                { _, hour, minute ->
                    val time = String.format("%02d:%02d %s",
                        if (hour % 12 == 0) 12 else hour % 12,
                        minute,
                        if (hour >= 12) "PM" else "AM")
                    editText.setText(time)
                },
                now.get(java.util.Calendar.HOUR_OF_DAY),
                now.get(java.util.Calendar.MINUTE),
                false
            ).show()
        }

        @RequiresApi(Build.VERSION_CODES.O)
        private fun updateDuration(binding: BookingDialogBoxBinding, checkinDate: String?, checkoutDate: String?) {
            if (checkinDate.isNullOrEmpty() || checkoutDate.isNullOrEmpty()) {
                binding.durationText.text = "Duration: N/A"
                return
            }
            try {
                val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                val checkin = LocalDate.parse(checkinDate, formatter)
                val checkout = LocalDate.parse(checkoutDate, formatter)
                val days = ChronoUnit.DAYS.between(checkin, checkout)
                binding.durationText.text = when {
                    days <= 0 -> "Duration: Same day"
                    days == 1L -> "Duration: 1 day"
                    else -> "Duration: $days days"
                }
            } catch (e: Exception) {
                binding.durationText.text = "Duration: Invalid dates"
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RoomViewHolder {
        val binding = RoomItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RoomViewHolder(binding)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onBindViewHolder(holder: RoomViewHolder, position: Int) {
        holder.bind(roomsList[position])
    }

    override fun getItemCount(): Int = roomsList.size
}
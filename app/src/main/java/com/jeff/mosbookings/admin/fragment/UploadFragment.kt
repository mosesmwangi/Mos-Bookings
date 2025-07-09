package com.jeff.mosbookings.admin.fragment

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.jeff.mosbookings.R
import com.jeff.mosbookings.databinding.FragmentUploadBinding
import com.jeff.mosbookings.models.RoomData
import com.jeff.mosbookings.models.SharedData
import java.util.Calendar

class UploadFragment : Fragment() {

    private var _binding: FragmentUploadBinding? = null
    private val binding get() = _binding!!
    private var selectedImageUri: android.net.Uri? = null

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                selectedImageUri = uri
                Glide.with(this)
                    .load(uri)
                    .into(binding.roomImagePreview)
            }
        }
    }

    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            openGallery()
        } else {
            Toast.makeText(requireContext(), "Gallery permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUploadBinding.inflate(inflater, container, false)
        setupForm()
        return binding.root
    }

    private fun setupForm() {
        // Set up image picker
        binding.pickImageButton.setOnClickListener {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                openGallery()
            } else {
                permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }

        // Set up date pickers
        binding.checkinDateLayout.setEndIconOnClickListener {
            showDatePicker(binding.checkinDateInput)
        }
        binding.checkoutDateLayout.setEndIconOnClickListener {
            showDatePicker(binding.checkoutDateInput)
        }

        // Set up time pickers
        binding.checkinTimeLayout.setEndIconOnClickListener {
            showTimePicker(binding.checkinTimeInput)
        }
        binding.checkoutTimeLayout.setEndIconOnClickListener {
            showTimePicker(binding.checkoutTimeInput)
        }

        // Handle upload button
        binding.uploadRoomButton.setOnClickListener {
            if (validateInputs()) {
                val room = RoomData(
                    roomImage = selectedImageUri?.toString(),
                    roomName = binding.roomNameInput.text.toString(),
                    roomLocation = binding.roomLocationInput.text.toString(),
                    roomPrice = binding.roomPriceInput.text.toString(),
                    roomAvailability = binding.roomAvailabilityInput.isChecked,
                    roomDescription = binding.roomDescriptionInput.text.toString(),
                    checkinTime = binding.checkinTimeInput.text.toString(),
                    checkOutTime = binding.checkoutTimeInput.text.toString(),
                    checkinDate = binding.checkinDateInput.text.toString(),
                    checkoutDate = binding.checkoutDateInput.text.toString()
                )
                SharedData.roomsList.add(room)
                Toast.makeText(requireContext(), "Room uploaded successfully", Toast.LENGTH_SHORT).show()
                clearForm()
            }
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickImageLauncher.launch(intent)
    }

    private fun showDatePicker(editText: com.google.android.material.textfield.TextInputEditText) {
        val today = Calendar.getInstance()
        DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                val date = String.format("%04d-%02d-%02d", year, month + 1, day)
                editText.setText(date)
            },
            today.get(Calendar.YEAR),
            today.get(Calendar.MONTH),
            today.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    @SuppressLint("DefaultLocale")
    private fun showTimePicker(editText: com.google.android.material.textfield.TextInputEditText) {
        val now = Calendar.getInstance()
        TimePickerDialog(
            requireContext(),
            { _, hour, minute ->
                val time = String.format("%02d:%02d %s",
                    if (hour % 12 == 0) 12 else hour % 12,
                    minute,
                    if (hour >= 12) "PM" else "AM")
                editText.setText(time)
            },
            now.get(Calendar.HOUR_OF_DAY),
            now.get(Calendar.MINUTE),
            false
        ).show()
    }

    private fun validateInputs(): Boolean {
        var isValid = true
        with(binding) {
            if (selectedImageUri == null) {
                Toast.makeText(requireContext(), "Please select an image", Toast.LENGTH_SHORT).show()
                isValid = false
            }

            if (roomNameInput.text.isNullOrBlank()) {
                roomNameLayout.error = "Room name is required"
                isValid = false
            } else {
                roomNameLayout.error = null
            }

            if (roomLocationInput.text.isNullOrBlank()) {
                roomLocationLayout.error = "Location is required"
                isValid = false
            } else {
                roomLocationLayout.error = null
            }

            if (roomPriceInput.text.isNullOrBlank()) {
                roomPriceLayout.error = "Price is required"
                isValid = false
            } else {
                roomPriceLayout.error = null
            }

            if (roomDescriptionInput.text.isNullOrBlank()) {
                roomDescriptionLayout.error = "Description is required"
                isValid = false
            } else {
                roomDescriptionLayout.error = null
            }

            if (checkinDateInput.text.isNullOrBlank()) {
                checkinDateLayout.error = "Check-in date is required"
                isValid = false
            } else {
                checkinDateLayout.error = null
            }

            if (checkinTimeInput.text.isNullOrBlank()) {
                checkinTimeLayout.error = "Check-in time is required"
                isValid = false
            } else {
                checkinTimeLayout.error = null
            }

            if (checkoutDateInput.text.isNullOrBlank()) {
                checkoutDateLayout.error = "Check-out date is required"
                isValid = false
            } else {
                checkoutDateLayout.error = null
            }

            if (checkoutTimeInput.text.isNullOrBlank()) {
                checkoutTimeLayout.error = "Check-out time is required"
                isValid = false
            } else {
                checkoutTimeLayout.error = null
            }
        }
        if (!isValid) {
            Toast.makeText(requireContext(), "Please fill all required fields", Toast.LENGTH_SHORT).show()
        }
        return isValid
    }

    private fun clearForm() {
        with(binding) {
            selectedImageUri = null
            roomImagePreview.setImageResource(R.drawable.placeholder_room)
            roomNameInput.text?.clear()
            roomLocationInput.text?.clear()
            roomPriceInput.text?.clear()
            roomDescriptionInput.text?.clear()
            roomAvailabilityInput.isChecked = true
            checkinDateInput.text?.clear()
            checkinTimeInput.text?.clear()
            checkoutDateInput.text?.clear()
            checkoutTimeInput.text?.clear()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
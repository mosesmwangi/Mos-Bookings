package com.jeff.mosbookings.admin.fragment

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.jeff.mosbookings.databinding.FragmentUploadBinding
import com.jeff.mosbookings.models.RoomData
import com.jeff.mosbookings.models.SharedData
import java.util.UUID
import kotlinx.coroutines.GlobalScope
import com.jeff.mosbookings.repository.RoomRepository
import kotlinx.coroutines.launch

class UploadFragment : Fragment() {
    private var _binding: FragmentUploadBinding? = null
    private val binding get() = _binding!!
    private val selectedImageUris = mutableListOf<Uri>()
    private lateinit var imagesAdapter: ImagesAdapter

    private val pickImagesLauncher = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris != null) {
            if (selectedImageUris.size + uris.size > 6) {
                Toast.makeText(requireContext(), "You can select up to 6 images only", Toast.LENGTH_SHORT).show()
                return@registerForActivityResult
            }
            selectedImageUris.addAll(uris.take(6 - selectedImageUris.size))
            imagesAdapter.notifyDataSetChanged()
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
        // Set up images RecyclerView
        imagesAdapter = ImagesAdapter(selectedImageUris)
        binding.imagesRecyclerView.layoutManager = LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false)
        binding.imagesRecyclerView.adapter = imagesAdapter

        // Set up image picker
        binding.pickImageButton.setOnClickListener {
            val permission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                Manifest.permission.READ_MEDIA_IMAGES
            } else {
                Manifest.permission.READ_EXTERNAL_STORAGE
            }
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    permission
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                openGallery()
            } else {
                permissionLauncher.launch(permission)
            }
        }

        // Handle upload button
        binding.uploadRoomButton.setOnClickListener {
            Toast.makeText(requireContext(), "Upload button clicked", Toast.LENGTH_SHORT).show()
            if (validateInputs()) {
                binding.loadingProgressBar.visibility = View.VISIBLE
                val name = binding.roomNameInput.text.toString()
                val type = binding.roomTypeInput.text.toString()
                val location = binding.roomLocationInput.text.toString()
                val price = binding.roomPriceInput.text.toString().toDoubleOrNull() ?: 0.0
                val rating = binding.ratingInput.text.toString().toFloatOrNull() ?: 0f
                val amenities = binding.amenitiesInput.text.toString().split(",").map { it.trim() }.filter { it.isNotEmpty() }
                val description = binding.roomDescriptionInput.text.toString()
                val unavailableDates = binding.unavailableDatesInput.text.toString().split(",").map { it.trim() }.filter { it.isNotEmpty() }

                val prefs = requireContext().getSharedPreferences("auth", android.content.Context.MODE_PRIVATE)
                val token = prefs.getString("jwt", null)
                if (token == null) {
                    Toast.makeText(requireContext(), "Not authenticated as admin", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                val roomRepository = com.jeff.mosbookings.repository.RoomRepository()
                GlobalScope.launch {
                    activity?.runOnUiThread {
                        if (!isAdded) return@runOnUiThread
                        Toast.makeText(context, "Starting upload...", Toast.LENGTH_SHORT).show()
                    }
                    try {
                        val uploadedRoom = roomRepository.uploadRoom(
                            requireContext(),
                            selectedImageUris,
                            name,
                            type,
                            location,
                            price,
                            amenities,
                            rating,
                            description,
                            unavailableDates,
                            token
                        )
                        activity?.runOnUiThread {
                            if (!isAdded) return@runOnUiThread
                            binding.loadingProgressBar.visibility = View.GONE
                            if (uploadedRoom != null) {
                                SharedData.roomsList.add(uploadedRoom)
                                context?.let { Toast.makeText(it, "Room uploaded successfully", Toast.LENGTH_SHORT).show() }
                clearForm()
                            } else {
                                context?.let { Toast.makeText(it, "Failed to upload room to server", Toast.LENGTH_SHORT).show() }
                            }
                        }
                    } catch (e: Exception) {
                        activity?.runOnUiThread {
                            if (!isAdded) return@runOnUiThread
                            binding.loadingProgressBar.visibility = View.GONE
                            context?.let { Toast.makeText(it, "Upload error: ${e.message}", Toast.LENGTH_LONG).show() }
                        }
                    }
                }
            }
        }
    }

    private fun openGallery() {
        pickImagesLauncher.launch("image/*")
    }

    private fun validateInputs(): Boolean {
        var isValid = true
        with(binding) {
            if (selectedImageUris.isEmpty()) {
                Toast.makeText(requireContext(), "Please select at least 1 image", Toast.LENGTH_SHORT).show()
                isValid = false
            }
            if (roomNameInput.text.isNullOrBlank()) {
                roomNameLayout.error = "Room name is required"
                isValid = false
            } else {
                roomNameLayout.error = null
            }
            if (roomTypeInput.text.isNullOrBlank()) {
                roomTypeLayout.error = "Room type is required"
                isValid = false
            } else {
                roomTypeLayout.error = null
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
            if (ratingInput.text.isNullOrBlank()) {
                ratingLayout.error = "Rating is required"
                isValid = false
            } else {
                ratingLayout.error = null
            }
            if (amenitiesInput.text.isNullOrBlank()) {
                amenitiesLayout.error = "Amenities are required"
                isValid = false
            } else {
                amenitiesLayout.error = null
            }
            if (roomDescriptionInput.text.isNullOrBlank()) {
                roomDescriptionLayout.error = "Description is required"
                isValid = false
            } else {
                roomDescriptionLayout.error = null
            }
        }
        if (!isValid) {
            Toast.makeText(requireContext(), "Please fill all required fields", Toast.LENGTH_SHORT).show()
        }
        return isValid
    }

    private fun clearForm() {
        selectedImageUris.clear()
        imagesAdapter.notifyDataSetChanged()
        with(binding) {
            roomNameInput.text?.clear()
            roomTypeInput.text?.clear()
            roomLocationInput.text?.clear()
            roomPriceInput.text?.clear()
            ratingInput.text?.clear()
            amenitiesInput.text?.clear()
            roomDescriptionInput.text?.clear()
            unavailableDatesInput.text?.clear()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // Simple adapter for image previews
    inner class ImagesAdapter(private val uris: List<Uri>) : RecyclerView.Adapter<ImagesAdapter.ImageViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
            val imageView = android.widget.ImageView(parent.context).apply {
                layoutParams = ViewGroup.LayoutParams(100, 100)
                scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
                setPadding(8, 8, 8, 8)
            }
            return ImageViewHolder(imageView)
        }
        override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
            Glide.with(holder.imageView.context).load(uris[position]).into(holder.imageView)
        }
        override fun getItemCount() = uris.size
        inner class ImageViewHolder(val imageView: android.widget.ImageView) : RecyclerView.ViewHolder(imageView)
    }
}
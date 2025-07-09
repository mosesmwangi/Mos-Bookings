package com.jeff.mosbookings.fragments

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.graphics.BitmapFactory
import android.widget.Toast
import com.jeff.mosbookings.R
import com.jeff.mosbookings.admin.AdminHome
import com.jeff.mosbookings.admin.auth.AdminLogin
import com.jeff.mosbookings.auth.Login
import com.jeff.mosbookings.databinding.FragmentProfileBinding

class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                openImagePicker()
            } else {
                binding.errorText.visibility = View.VISIBLE
                binding.errorText.text = "Storage permission denied"
            }
        }
    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            result.data?.data?.let { uri ->
                loadProfilePicture(uri)
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadDummyProfile()

        binding.editProfileImage.setOnClickListener {
            requestStoragePermission()
        }

        binding.adminButton.setOnClickListener {
            val intent = Intent(requireActivity(), AdminLogin::class.java)
            startActivity(intent)
            requireActivity().finish()
        }

        binding.logoutButton.setOnClickListener {
            val intent = Intent(requireActivity(), Login::class.java)
            startActivity(intent)
        }
    }

    private fun loadDummyProfile() {
        // Dummy user data
        binding.userName.text = "John Doe"
        binding.userEmail.text = "john.doe@example.com"
        binding.userPhone.text = "+1234567890"
        binding.userLocation.text = "New York, NY"
        binding.profileImage.setImageResource(R.drawable.ic_user) // Default profile picture
    }

    private fun requestStoragePermission() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                permission
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            openImagePicker()
        } else {
            requestPermissionLauncher.launch(permission)
        }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickImageLauncher.launch(intent)
    }

    private fun loadProfilePicture(uri: Uri) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val inputStream = requireContext().contentResolver.openInputStream(uri)
                inputStream?.use { stream ->
                    val imageBytes = stream.readBytes()
                    withContext(Dispatchers.Main) {
                        binding.profileImage.setImageBitmap(
                            BitmapFactory.decodeByteArray(
                                imageBytes,
                                0,
                                imageBytes.size
                            )
                        )
                        binding.errorText.visibility = View.GONE
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.errorText.visibility = View.VISIBLE
                    binding.errorText.text = "Failed to load image: ${e.message}"
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

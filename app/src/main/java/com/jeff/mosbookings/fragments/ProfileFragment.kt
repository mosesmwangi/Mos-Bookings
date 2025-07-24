package com.jeff.mosbookings.fragments

import android.content.Context
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
import org.json.JSONObject
import android.app.AlertDialog
import android.provider.Settings
import android.util.Log

class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            Log.d("ProfileFragment", "Permission result: $isGranted")
            if (isGranted) {
                // Permission granted, show a message or update UI, but do NOT open the gallery automatically
                Toast.makeText(requireContext(), "Gallery permission granted. Tap the image to select a profile picture.", Toast.LENGTH_SHORT).show()
            } else {
                binding.errorText.visibility = View.VISIBLE
                binding.errorText.text = "Storage permission denied"
                handlePermissionDenied()
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
        loadUserProfile()

        binding.editProfileImage.setOnClickListener {
            val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                android.Manifest.permission.READ_MEDIA_IMAGES
            } else {
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            }
            if (ContextCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_GRANTED) {
                openImagePicker()
            } else {
                requestStoragePermission()
            }
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

    private fun loadUserProfile() {
        val prefs = requireContext().getSharedPreferences("auth", Context.MODE_PRIVATE)
        val userJson = prefs.getString("user", null)
        if (userJson != null) {
            val user = JSONObject(userJson)
            binding.userName.text = user.optString("name", "-")
            binding.userEmail.text = user.optString("email", "-")
            binding.userPhone.text = user.optString("phone", "-")
            binding.userLocation.text = user.optString("role", "-")
        } else {
            binding.userName.text = "-"
            binding.userEmail.text = "-"
            binding.userPhone.text = "-"
            binding.userLocation.text = "-"
        }
        binding.profileImage.setImageResource(R.drawable.ic_user)
    }

    private fun requestStoragePermission() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            android.Manifest.permission.READ_MEDIA_IMAGES
        } else {
            android.Manifest.permission.READ_EXTERNAL_STORAGE
        }
        Log.d("ProfileFragment", "Requesting permission: $permission (SDK: ${Build.VERSION.SDK_INT})")
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                permission
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            Log.d("ProfileFragment", "Permission already granted: $permission")
            openImagePicker()
        } else if (shouldShowRequestPermissionRationale(permission)) {
            Log.d("ProfileFragment", "Should show rationale for: $permission")
            // Show rationale dialog
            AlertDialog.Builder(requireContext())
                .setTitle("Permission Needed")
                .setMessage("This app needs access to your gallery to select a profile picture.")
                .setPositiveButton("OK") { _, _ ->
                    Log.d("ProfileFragment", "Launching permission request after rationale: $permission")
                    requestPermissionLauncher.launch(permission)
                }
                .setNegativeButton("Cancel", null)
                .show()
        } else {
            Log.d("ProfileFragment", "Launching permission request: $permission")
            // User checked "Don't ask again" or first time
            requestPermissionLauncher.launch(permission)
        }
    }

    private fun handlePermissionDenied() {
        AlertDialog.Builder(requireContext())
            .setTitle("Permission Denied")
            .setMessage("Please enable storage permission in settings to use this feature.")
            .setPositiveButton("Open Settings") { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri = Uri.fromParts("package", requireContext().packageName, null)
                intent.data = uri
                startActivity(intent)
            }
            .setNegativeButton("Cancel", null)
            .show()
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

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
import androidx.lifecycle.lifecycleScope
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
import com.jeff.mosbookings.repository.RoomRepository
import org.json.JSONObject
import android.app.AlertDialog
import android.provider.Settings
import android.util.Log

class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val roomRepository = RoomRepository()
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
        loadUserStats()
        setupPreferences()

        binding.adminButton.setOnClickListener {
            val intent = Intent(requireActivity(), AdminLogin::class.java)
            startActivity(intent)
            requireActivity().finish()
        }

        binding.logoutButton.setOnClickListener {
            val intent = Intent(requireActivity(), Login::class.java)
            startActivity(intent)
        }

        binding.editProfileButton.setOnClickListener {
            Toast.makeText(requireContext(), "Edit profile feature coming soon!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadUserProfile() {
        val prefs = requireContext().getSharedPreferences("auth", Context.MODE_PRIVATE)
        val userJson = prefs.getString("user", null)
        val token = prefs.getString("jwt", null)
        
        Log.d("ProfileFragment", "👤 Loading user profile - Token exists: ${token != null}")
        
        if (userJson != null && token != null) {
            try {
                val user = JSONObject(userJson)
                val role = user.optString("role", "user").lowercase()
                
                Log.d("ProfileFragment", "👤 User role: $role")
                
                // Show user details based on role
                val userName = user.optString("name", "Unknown User")
                binding.userName.text = "Welcome, $userName!"
                binding.userEmail.text = "Email: ${user.optString("email", "No email")}"
                binding.userPhone.text = "Phone: ${user.optString("phone", "No phone")}"
                binding.bookingInfo.text = "Bookings by $userName"
                
                // Display role appropriately
                when (role) {
                    "admin" -> {
                        binding.userLocation.text = "Role: Administrator"
                        binding.userLocation.setTextColor(requireContext().getColor(android.R.color.holo_red_dark))
                        Log.d("ProfileFragment", "👤 Admin user detected - showing admin details")
                    }
                    "user" -> {
                        binding.userLocation.text = "Role: Regular User"
                        binding.userLocation.setTextColor(requireContext().getColor(android.R.color.holo_blue_dark))
                        Log.d("ProfileFragment", "👤 Regular user detected - showing user details")
                    }
                    else -> {
                        binding.userLocation.text = "Role: User ($role)"
                        binding.userLocation.setTextColor(requireContext().getColor(android.R.color.darker_gray))
                    }
                }
                
                Log.d("ProfileFragment", "👤 Profile loaded: ${user.optString("name")} (${user.optString("email")})")
                
            } catch (e: Exception) {
                Log.e("ProfileFragment", "👤 Error parsing user data: ${e.message}")
                // If JSON parsing fails, show default values
                binding.userName.text = "Welcome, User!"
                binding.userEmail.text = "Email: No email"
                binding.userPhone.text = "Phone: No phone"
                binding.userLocation.text = "Role: User"
                binding.bookingInfo.text = "Bookings by User"
            }
        } else {
            Log.w("ProfileFragment", "👤 No user data or token found")
            binding.userName.text = "Welcome, Guest!"
            binding.userEmail.text = "Email: Please login"
            binding.userPhone.text = "Phone: No phone"
            binding.userLocation.text = "Role: Guest"
            binding.bookingInfo.text = "Bookings by Guest"
        }
        binding.profileImage.setImageResource(R.drawable.ic_user)
    }

    private fun loadUserStats() {
        val prefs = requireContext().getSharedPreferences("auth", Context.MODE_PRIVATE)
        val token = prefs.getString("jwt", null)
        
        if (token != null) {
            lifecycleScope.launch {
                try {
                    val userBookings = roomRepository.getUserBookings(token)
                    val rooms = roomRepository.getRooms()
                    val roomMap = rooms?.associateBy { it.id } ?: emptyMap()
                    
                    var totalSpent = 0.0
                    userBookings.forEach { booking ->
                        val room = roomMap[booking.roomId]
                        if (room != null) {
                            totalSpent += room.price
                        }
                    }
                    
                    if (isAdded && view != null) {
                        binding.totalBookings.text = userBookings.size.toString()
                        binding.totalSpent.text = "KSh ${String.format("%.0f", totalSpent)}"
                    }
                } catch (e: Exception) {
                    Log.e("ProfileFragment", "Error loading user stats: ${e.message}")
                    if (isAdded && view != null) {
                        binding.totalBookings.text = "0"
                        binding.totalSpent.text = "KSh 0"
                    }
                }
            }
        } else {
            binding.totalBookings.text = "0"
            binding.totalSpent.text = "KSh 0"
        }
    }

    private fun setupPreferences() {
        val prefs = requireContext().getSharedPreferences("user_preferences", Context.MODE_PRIVATE)
        
        // Load notification preference
        val notificationsEnabled = prefs.getBoolean("notifications_enabled", true)
        binding.notificationSwitch.isChecked = notificationsEnabled
        
        // Load dark mode preference
        val darkModeEnabled = prefs.getBoolean("dark_mode_enabled", false)
        binding.darkModeSwitch.isChecked = darkModeEnabled
        
        // Setup listeners
        binding.notificationSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("notifications_enabled", isChecked).apply()
            Toast.makeText(requireContext(), 
                if (isChecked) "Notifications enabled" else "Notifications disabled", 
                Toast.LENGTH_SHORT).show()
        }
        
        binding.darkModeSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("dark_mode_enabled", isChecked).apply()
            Toast.makeText(requireContext(), 
                if (isChecked) "Dark mode enabled - Restart app to apply" else "Dark mode disabled - Restart app to apply", 
                Toast.LENGTH_LONG).show()
        }
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

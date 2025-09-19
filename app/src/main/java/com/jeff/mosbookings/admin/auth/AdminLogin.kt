package com.jeff.mosbookings.admin.auth

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.jeff.mosbookings.R
import com.jeff.mosbookings.admin.AdminHome
import com.jeff.mosbookings.databinding.ActivityAdminLoginBinding
import com.jeff.mosbookings.network.RetrofitInstance
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AdminLogin : AppCompatActivity() {
    private lateinit var binding: ActivityAdminLoginBinding
    
    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivityAdminLoginBinding.inflate(layoutInflater)
        supportActionBar?.hide()
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.btnLogin.setOnClickListener {
            val email = binding.emailEt.text.toString().trim()
            val password = binding.passEt.text.toString().trim()
            
            Log.d("AdminLogin", "🔐 ADMIN LOGIN - Button clicked for email: $email")
            
            if (email.isEmpty() || password.isEmpty()) {
                Log.w("AdminLogin", "🔐 ADMIN LOGIN - Validation failed: Empty fields")
                Toast.makeText(this, "Please enter both email and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            binding.progressBar.visibility = View.VISIBLE
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    Log.d("AdminLogin", "🔐 POST /api/auth/login - Sending admin login request")
                    val startTime = System.currentTimeMillis()
                    
                    val response = RetrofitInstance.api.login(mapOf("email" to email, "password" to password))
                    val duration = System.currentTimeMillis() - startTime
                    
                    withContext(Dispatchers.Main) {
                        binding.progressBar.visibility = View.GONE
                        Log.d("AdminLogin", "🔐 POST /api/auth/login - Response received in ${duration}ms")
                        Log.d("AdminLogin", "🔐 POST /api/auth/login - Status: ${response.code()}")
                        
                        if (response.isSuccessful && response.body() != null) {
                            val authResponse = response.body()!!
                            val token = authResponse.token
                            val user = authResponse.user
                            
                            Log.d("AdminLogin", "🔐 POST /api/auth/login - Success: Token received")
                            Log.d("AdminLogin", "🔐 User details: ${user?.email}, Role: ${user?.role}")
                            
                            // Check if user is admin - only allow admin login here
                            if (user?.role?.lowercase() != "admin") {
                                Log.w("AdminLogin", "🔐 POST /api/auth/login - Non-admin login blocked through admin login")
                                Toast.makeText(this@AdminLogin, "Only admin users can login here. Please use regular login.", Toast.LENGTH_LONG).show()
                                return@withContext
                            }
                            
                            if (token != null) {
                                // Save admin token and user info
                                val prefs = getSharedPreferences("admin_auth", Context.MODE_PRIVATE)
                                prefs.edit().putString("admin_jwt", token).apply()
                                Log.d("AdminLogin", "🔐 Admin token saved to SharedPreferences")
                                
                                if (user != null) {
                                    val userJson = "{" +
                                        "\"id\":\"${user.id}\"," +
                                        "\"email\":\"${user.email}\"," +
                                        "\"role\":\"${user.role}\"," +
                                        "\"name\":\"${user.name}\"," +
                                        "\"phone\":\"\"}"
                                    prefs.edit().putString("admin_user", userJson).apply()
                                    Log.d("AdminLogin", "🔐 Admin user data saved to SharedPreferences")
                                }
                                
                                Log.d("AdminLogin", "🔐 Navigating to AdminHome activity")
                                startActivity(Intent(this@AdminLogin, AdminHome::class.java))
                                finish()
                            } else {
                                Log.e("AdminLogin", "🔐 POST /api/auth/login - Invalid response: No token")
                                Toast.makeText(this@AdminLogin, "Invalid response from server", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            val errorMsg = response.errorBody()?.string() ?: "Admin login failed"
                            Log.e("AdminLogin", "🔐 POST /api/auth/login - Failed: ${response.code()} - $errorMsg")
                            Toast.makeText(this@AdminLogin, errorMsg, Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    Log.e("AdminLogin", "🔐 POST /api/auth/login - Exception: ${e.localizedMessage}", e)
                    withContext(Dispatchers.Main) {
                        binding.progressBar.visibility = View.GONE
                        Toast.makeText(this@AdminLogin, "Network error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}
package com.jeff.mosbookings.auth

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.jeff.mosbookings.Home
import com.jeff.mosbookings.R
import com.jeff.mosbookings.databinding.ActivityLoginBinding
import com.jeff.mosbookings.network.RetrofitInstance
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.view.View

class Login : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivityLoginBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.btnLogin.setOnClickListener {
            val email = binding.emailEt.text.toString().trim()
            val password = binding.passEt.text.toString().trim()
            Log.d("LoginActivity", "🔐 LOGIN - Button clicked for email: $email")
            
            if (email.isEmpty() || password.isEmpty()) {
                Log.w("LoginActivity", "🔐 LOGIN - Validation failed: Empty fields")
                Toast.makeText(this, "Please enter both email and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            binding.progressBar.visibility = View.VISIBLE
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    Log.d("LoginActivity", "🔐 POST /api/auth/login - Sending login request")
                    val startTime = System.currentTimeMillis()
                    
                    val response = RetrofitInstance.api.login(mapOf("email" to email, "password" to password))
                    val duration = System.currentTimeMillis() - startTime
                    
                    withContext(Dispatchers.Main) {
                        binding.progressBar.visibility = View.GONE
                        Log.d("LoginActivity", "🔐 POST /api/auth/login - Response received in ${duration}ms")
                        Log.d("LoginActivity", "🔐 POST /api/auth/login - Status: ${response.code()}")
                        
                        if (response.isSuccessful && response.body() != null) {
                            val authResponse = response.body()!!
                            val token = authResponse.token
                            val user = authResponse.user
                            
                            Log.d("LoginActivity", "🔐 POST /api/auth/login - Success: Token received")
                            Log.d("LoginActivity", "🔐 User details: ${user?.email}, Role: ${user?.role}")
                            
                            // Check if user is admin - restrict admin login through normal login
                            if (user?.role?.lowercase() == "admin") {
                                Log.w("LoginActivity", "🔐 POST /api/auth/login - Admin login blocked through normal login")
                                Toast.makeText(this@Login, "Admin login not allowed here. Please use Admin Login.", Toast.LENGTH_LONG).show()
                                return@withContext
                            }
                            
                            if (token != null) {
                                // Save token and user info
                                val prefs = getSharedPreferences("auth", Context.MODE_PRIVATE)
                                prefs.edit().putString("jwt", token).apply()
                                Log.d("LoginActivity", "🔐 Token saved to SharedPreferences")
                                
                                if (user != null) {
                                    val userJson = "{" +
                                        "\"id\":\"${user.id}\"," +
                                        "\"email\":\"${user.email}\"," +
                                        "\"role\":\"${user.role}\"," +
                                        "\"name\":\"${user.name}\"," +
                                        "\"phone\":\"\"}"
                                    prefs.edit().putString("user", userJson).apply()
                                    Log.d("LoginActivity", "🔐 User data saved to SharedPreferences")
                                }
                                
                                Log.d("LoginActivity", "🔐 Navigating to Home activity")
                                startActivity(Intent(this@Login, Home::class.java))
                                finish()
                            } else {
                                Log.e("LoginActivity", "🔐 POST /api/auth/login - Invalid response: No token")
                                Toast.makeText(this@Login, "Invalid response from server", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            val errorMsg = response.errorBody()?.string() ?: "Login failed"
                            Log.e("LoginActivity", "🔐 POST /api/auth/login - Failed: ${response.code()} - $errorMsg")
                            Toast.makeText(this@Login, errorMsg, Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    Log.e("LoginActivity", "🔐 POST /api/auth/login - Exception: ${e.localizedMessage}", e)
                    withContext(Dispatchers.Main) {
                        binding.progressBar.visibility = View.GONE
                        Toast.makeText(this@Login, "Network error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        binding.tvRegister.setOnClickListener {
            startActivity(Intent(this, Register::class.java))
            finish()
        }
    }
}
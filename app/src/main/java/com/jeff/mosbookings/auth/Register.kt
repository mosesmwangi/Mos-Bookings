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
import com.jeff.mosbookings.databinding.ActivityRegisterBinding
import com.jeff.mosbookings.network.RetrofitInstance
import com.jeff.mosbookings.auth.Login
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.view.View

class Register : AppCompatActivity() {
    private lateinit var binding : ActivityRegisterBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        supportActionBar?.hide()
        super.onCreate(savedInstanceState)

        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.btnRegister.setOnClickListener {
            val name = binding.nameEt.text.toString().trim()
            val email = binding.emailEt.text.toString().trim()
            val phone = binding.phoneEt.text.toString().trim()
            val password = binding.passEt.text.toString().trim()
            val confirmPassword = binding.confirmPassEt.text.toString().trim()
            
            Log.d("RegisterActivity", "📝 REGISTER - Button clicked for email: $email")
            
            if (name.isEmpty() || email.isEmpty() || phone.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Log.w("RegisterActivity", "📝 REGISTER - Validation failed: Empty fields")
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (password != confirmPassword) {
                Log.w("RegisterActivity", "📝 REGISTER - Validation failed: Passwords don't match")
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            binding.progressBar.visibility = View.VISIBLE
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    Log.d("RegisterActivity", "📝 POST /api/auth/register - Sending registration request")
                    Log.d("RegisterActivity", "📝 User details: name=$name, email=$email, phone=$phone")
                    val startTime = System.currentTimeMillis()
                    
                    val response = RetrofitInstance.api.register(mapOf(
                        "name" to name,
                        "email" to email,
                        "phone" to phone,
                        "password" to password
                    ))
                    val duration = System.currentTimeMillis() - startTime
                    
                    withContext(Dispatchers.Main) {
                        binding.progressBar.visibility = View.GONE
                        Log.d("RegisterActivity", "📝 POST /api/auth/register - Response received in ${duration}ms")
                        Log.d("RegisterActivity", "📝 POST /api/auth/register - Status: ${response.code()}")
                        
                        if (response.isSuccessful && response.body() != null) {
                            val authResponse = response.body()!!
                            val token = authResponse.token
                            val user = authResponse.user
                            
                            Log.d("RegisterActivity", "📝 POST /api/auth/register - Success: Registration completed")
                            Log.d("RegisterActivity", "📝 User details: ${user?.email}, Role: ${user?.role}")
                            
                            // Check if user is admin - restrict admin registration through normal register
                            if (user?.role?.lowercase() == "admin") {
                                Log.w("RegisterActivity", "📝 POST /api/auth/register - Admin registration blocked through normal register")
                                Toast.makeText(this@Register, "Admin registration not allowed here. Please contact administrator.", Toast.LENGTH_LONG).show()
                                return@withContext
                            }
                            
                            if (token != null) {
                                Log.d("RegisterActivity", "📝 Token received, saving and going to Home")
                                val prefs = getSharedPreferences("auth", Context.MODE_PRIVATE)
                                prefs.edit().putString("jwt", token).apply()
                                
                                if (user != null) {
                                    val userJson = "{" +
                                        "\"id\":\"${user.id}\"," +
                                        "\"email\":\"${user.email}\"," +
                                        "\"role\":\"${user.role}\"," +
                                        "\"name\":\"${user.name}\"," +
                                        "\"phone\":\"$phone\"}"
                                    prefs.edit().putString("user", userJson).apply()
                                    Log.d("RegisterActivity", "📝 User data saved to SharedPreferences")
                                }
                                
                                Log.d("RegisterActivity", "📝 Navigating to Home activity")
                                startActivity(Intent(this@Register, Home::class.java))
                                finish()
                            } else {
                                Log.d("RegisterActivity", "📝 No token received, going to Login")
                                Toast.makeText(this@Register, "Registration successful. Please login.", Toast.LENGTH_SHORT).show()
                                startActivity(Intent(this@Register, Login::class.java))
                                finish()
                            }
                        } else {
                            val errorMsg = response.errorBody()?.string() ?: "Registration failed"
                            Log.e("RegisterActivity", "📝 POST /api/auth/register - Failed: ${response.code()} - $errorMsg")
                            Toast.makeText(this@Register, errorMsg, Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    Log.e("RegisterActivity", "📝 POST /api/auth/register - Exception: ${e.localizedMessage}", e)
                    withContext(Dispatchers.Main) {
                        binding.progressBar.visibility = View.GONE
                        Toast.makeText(this@Register, "Network error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        binding.tvLogin.setOnClickListener {
            startActivity(Intent(this, Login::class.java))
            finish()
        }
    }
}
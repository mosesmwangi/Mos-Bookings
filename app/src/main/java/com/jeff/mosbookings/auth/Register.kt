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
            if (name.isEmpty() || email.isEmpty() || phone.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (password != confirmPassword) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            binding.progressBar.visibility = View.VISIBLE
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    Log.d("RegisterActivity", "Sending registration request: $email")
                    val response = RetrofitInstance.api.register(mapOf(
                        "name" to name,
                        "email" to email,
                        "phone" to phone,
                        "password" to password
                    ))
                    withContext(Dispatchers.Main) {
                        binding.progressBar.visibility = View.GONE
                        Log.d("RegisterActivity", "Received response: ${response.code()} ${response.body()}")
                        if (response.isSuccessful && response.body() != null) {
                            val token = response.body()!!.token
                            if (token != null) {
                                Log.d("RegisterActivity", "Token received, saving and going to Home")
                                getSharedPreferences("auth", Context.MODE_PRIVATE).edit().putString("jwt", token).apply()
                                startActivity(Intent(this@Register, Home::class.java))
                                finish()
                            } else {
                                Log.d("RegisterActivity", "No token received, going to Login")
                                Toast.makeText(this@Register, "Registration successful. Please login.", Toast.LENGTH_SHORT).show()
                                startActivity(Intent(this@Register, Login::class.java))
                                finish()
                            }
                        } else {
                            val errorMsg = response.errorBody()?.string() ?: "Registration failed"
                            Log.e("RegisterActivity", "Registration failed: $errorMsg")
                            Toast.makeText(this@Register, errorMsg, Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    Log.e("RegisterActivity", "Network error: ${e.localizedMessage}", e)
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
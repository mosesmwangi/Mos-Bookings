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
            Log.d("LoginActivity", "Login button clicked. Email: $email")
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter both email and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            binding.progressBar.visibility = View.VISIBLE
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    Log.d("LoginActivity", "Sending login request to backend")
                    val response = RetrofitInstance.api.login(mapOf("email" to email, "password" to password))
                    withContext(Dispatchers.Main) {
                        binding.progressBar.visibility = View.GONE
                        Log.d("LoginActivity", "Received response: ${response.code()} ${response.body()}")
                        if (response.isSuccessful && response.body() != null) {
                            val token = response.body()!!.token
                            val user = response.body()!!.user
                            if (token != null) {
                                // Save token and user info
                                Log.d("LoginActivity", "Token received, saving and going to Home")
                                val prefs = getSharedPreferences("auth", Context.MODE_PRIVATE)
                                prefs.edit().putString("jwt", token).apply()
                                if (user != null) {
                                    val userJson = "{" +
                                        "\"id\":\"${user.id}\"," +
                                        "\"email\":\"${user.email}\"," +
                                        "\"role\":\"${user.role}\"," +
                                        "\"name\":\"${user.name}\"}"
                                    prefs.edit().putString("user", userJson).apply()
                                }
                                startActivity(Intent(this@Login, Home::class.java))
                                finish()
                            } else {
                                Toast.makeText(this@Login, "Invalid response from server", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            val errorMsg = response.errorBody()?.string() ?: "Login failed"
                            Log.e("LoginActivity", "Login failed: $errorMsg")
                            Toast.makeText(this@Login, errorMsg, Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    Log.e("LoginActivity", "Network error: ${e.localizedMessage}", e)
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
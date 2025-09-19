package com.jeff.mosbookings.admin

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.jeff.mosbookings.R
import com.jeff.mosbookings.admin.fragment.AvailableFragment
import com.jeff.mosbookings.admin.fragment.UploadFragment
import com.jeff.mosbookings.admin.fragment.BookedFragment
import com.jeff.mosbookings.databinding.ActivityAdminHomeBinding

class AdminHome : AppCompatActivity() {
    private lateinit var binding: ActivityAdminHomeBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        supportActionBar?.hide()
        
        // Set up bottom navigation
        setupBottomNavigation()
        
        // Load default fragment
        replaceFragment(AvailableFragment())
    }
    
    private fun setupBottomNavigation() {
        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.icHome -> {
                    replaceFragment(AvailableFragment())
                    true
                }
                R.id.icBookings -> {
                    replaceFragment(BookedFragment())
                    true
                }
                R.id.myBookings -> {
                    replaceFragment(UploadFragment())
                    true
                }
                else -> false
            }
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        val fragmentManager = supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.frameLayout, fragment)
        fragmentTransaction.commit()
    }

}
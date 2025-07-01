package com.jeff.mosbookings

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.jeff.mosbookings.databinding.ActivityHomeBinding
import com.jeff.mosbookings.fragments.BookingsFragment
import com.jeff.mosbookings.fragments.HomeFragment
import com.jeff.mosbookings.fragments.MyBookingsFragment
import com.jeff.mosbookings.fragments.ProfileFragment

class Home : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        supportActionBar?.hide()
        setContentView(binding.root)
        replaceFragment(HomeFragment())

        binding.bottomNavigationView.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.icHome -> replaceFragment(HomeFragment())
                R.id.icBookings -> replaceFragment(BookingsFragment())
                R.id.myBookings -> replaceFragment(MyBookingsFragment())
                R.id.profile -> replaceFragment(ProfileFragment())

                else -> {
                }
            }
            true
        }

    }

    private fun replaceFragment(fragment: Fragment) {
        val fragmentManager = supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.frameLayout, fragment)
        fragmentTransaction.commit()
    }
}
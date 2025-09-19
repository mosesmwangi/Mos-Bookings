package com.jeff.mosbookings

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.jeff.mosbookings.databinding.ActivityHomeBinding
import com.jeff.mosbookings.fragments.HomeFragment
import com.jeff.mosbookings.fragments.MyBookingsFragment
import com.jeff.mosbookings.fragments.ProfileFragment
import com.jeff.mosbookings.fragments.ReportsFragment
import com.jeff.mosbookings.dialogs.AIAssistantDialog

class Home : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        supportActionBar?.hide()
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(0, systemBars.top, 0, systemBars.bottom)
            insets
        }

        replaceFragment(HomeFragment())
        setupFloatingActionButton()

        binding.bottomNavigationView.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.icHome -> replaceFragment(HomeFragment())
                R.id.myBookings -> replaceFragment(MyBookingsFragment())
                R.id.profile -> replaceFragment(ProfileFragment())
                R.id.reports -> replaceFragment(ReportsFragment())
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
    
    private fun setupFloatingActionButton() {
        binding.fabAIAssistant.setOnClickListener {
            val dialog = AIAssistantDialog()
            dialog.show(supportFragmentManager, "AIAssistantDialog")
        }
    }
    
}
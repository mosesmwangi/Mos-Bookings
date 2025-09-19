package com.jeff.mosbookings.dialogs

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.fragment.app.DialogFragment
import com.jeff.mosbookings.R
import com.jeff.mosbookings.databinding.DialogAiAssistantBinding

class AIAssistantDialog : DialogFragment() {
    
    private lateinit var binding: DialogAiAssistantBinding
    private val TAG = "AIAssistantDialog"
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DialogAiAssistantBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClickListeners()
    }
    
    private fun setupClickListeners() {
        // Close button
        binding.btnClose.setOnClickListener {
            dismiss()
        }
        
        // Quick help topics
        binding.btnBookingGuide.setOnClickListener {
            showResponse(getBookingGuide())
        }
        
        binding.btnCancelBooking.setOnClickListener {
            showResponse(getCancelBookingGuide())
        }
        
        binding.btnPaymentInfo.setOnClickListener {
            showResponse(getPaymentInfo())
        }
        
        binding.btnContactSupport.setOnClickListener {
            showResponse(getContactSupportInfo())
            binding.btnCallSupport.visibility = View.VISIBLE
        }
        
        // Call support button
        binding.btnCallSupport.setOnClickListener {
            callSupport()
        }
    }
    
    private fun showResponse(response: String) {
        Log.d(TAG, "🤖 AI Assistant - Showing response")
        binding.responseScrollView.visibility = View.VISIBLE
        binding.tvResponse.text = response
        binding.btnCallSupport.visibility = View.GONE
    }
    
    private fun getBookingGuide(): String {
        return """
            📖 HOW TO BOOK A ROOM
            
            1. Browse Available Rooms
               • Open the app and go to the Home tab
               • Scroll through available rooms
               • Tap on a room to view details
            
            2. Select Your Dates
               • Check room availability
               • Choose your check-in and check-out dates
               • Ensure the room is available for your dates
            
            3. Complete Booking
               • Tap "Book Now" button
               • Confirm your booking details
               • Your booking will be processed instantly
            
            4. Confirmation
               • You'll receive a booking confirmation
               • Check "My Bookings" tab to view your reservations
               • You can cancel or modify bookings from there
            
            💡 Tips:
            • Book in advance for better availability
            • Check room amenities before booking
            • Keep your booking confirmation safe
        """.trimIndent()
    }
    
    private fun getCancelBookingGuide(): String {
        return """
            ❌ HOW TO CANCEL A BOOKING
            
            1. Access Your Bookings
               • Go to "My Bookings" tab
               • Find the booking you want to cancel
            
            2. Cancel Process
               • Tap on the booking you want to cancel
               • Look for "Cancel Booking" button
               • Confirm the cancellation
            
            3. Refund Information
               • Cancellations are processed immediately
               • Refunds are processed within 3-5 business days
               • Check your payment method for refund
            
            4. Confirmation
               • You'll receive a cancellation confirmation
               • The room will be available for others to book
            
            ⚠️ Important Notes:
            • Cancellation policies may vary by room type
            • Some bookings may have cancellation fees
            • Contact support if you need assistance
        """.trimIndent()
    }
    
    private fun getPaymentInfo(): String {
        return """
            💳 PAYMENT INFORMATION
            
            1. Accepted Payment Methods
               • Credit Cards (Visa, MasterCard, American Express)
               • Debit Cards
               • Mobile Money (M-Pesa, Airtel Money)
               • Bank Transfers
            
            2. Payment Security
               • All payments are encrypted and secure
               • We don't store your payment details
               • PCI DSS compliant payment processing
            
            3. Pricing
               • Prices are displayed in Kenyan Shillings (KSh)
               • All prices include applicable taxes
               • No hidden fees or charges
            
            4. Refunds
               • Refunds processed within 3-5 business days
               • Refund method same as payment method
               • Contact support for refund inquiries
            
            💡 Payment Tips:
            • Ensure sufficient funds before booking
            • Keep payment confirmation receipts
            • Contact support for payment issues
        """.trimIndent()
    }
    
    private fun getContactSupportInfo(): String {
        return """
            📞 CONTACT SUPPORT
            
            Thank you for contacting MosBookings! 
            
            For further assistance, please call our support team:
            
            📱 Phone: +254702851367
            
            Our support team is available:
            • Monday - Friday: 8:00 AM - 6:00 PM
            • Saturday: 9:00 AM - 4:00 PM
            • Sunday: 10:00 AM - 2:00 PM
            
            We're here to help with:
            • Booking assistance
            • Cancellation support
            • Payment issues
            • Technical problems
            • General inquiries
            
            Tap the call button below to contact us directly!
        """.trimIndent()
    }
    
    private fun callSupport() {
        Log.d(TAG, "📞 AI Assistant - Calling support")
        try {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:+254702851367")
            }
            startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "📞 Error calling support: ${e.message}")
        }
    }
    
    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        
        // Add modern fade animation
        dialog?.window?.setWindowAnimations(R.style.DialogAnimation)
    }
}

package com.jeff.mosbookings.models

// Data class for authentication response

data class AuthResponse(
    val token: String?,
    val user: UserData?
) 
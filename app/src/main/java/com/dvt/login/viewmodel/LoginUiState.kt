package com.dvt.login.viewmodel

data class LoginUiState(
    val username: String = "",
    val password: String = "",
    val rememberMe: Boolean = false,
    val isLoading: Boolean = false,
    val failureCount: Int = 0,
    val errorMessage: String? = null,
    val lockoutExpiresAt: Long? = null // Timestamp for lockout expiration
) {

    // Check if the account is locked by comparing the current time with lockoutExpiresAt
    val isLockedOut: Boolean = lockoutExpiresAt?.let {
        it > System.currentTimeMillis()  // Account is locked if lockout expiration time is in the future
    } ?: false

    // Enables the login button only when the fields are valid
    val isLoginEnabled: Boolean =
        username.isValidUsername() &&
                password.length >= 8 &&
                !isLoading


    // Username validation logic
    val usernameError: String? = when {
        username.isNotEmpty() && username.length < 3 -> "Username must be at least 3 characters"
        username.contains(" ") -> "Username cannot contain spaces"
        else -> null
    }

    // Password validation logic
    val passwordError: String? = when {
        password.isNotEmpty() && password.length < 8 -> "Password must be at least 8 characters"
        else -> null
    }

    // Helper function to validate username
    private fun String.isValidUsername(): Boolean {
        return this.length >= 3 && !this.contains(" ")
    }
}

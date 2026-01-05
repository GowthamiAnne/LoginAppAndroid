package com.dvt.login.viewmodel

data class LoginUiState(
    val username: String = "",
    val password: String = "",
    val rememberMe: Boolean = false,
    val isLoading: Boolean = false,
    val failureCount: Int = 0,
    val isLockedOut: Boolean = false,
    val errorMessage: String? = null
) {
    val isLoginEnabled: Boolean =
        username.isNotBlank() &&
                password.length >= 4 &&
                !isLoading &&
                !isLockedOut
}

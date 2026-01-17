package com.dvt.login.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dvt.login.network.NetworkMonitor
import com.dvt.login.repository.AuthRepository
import com.dvt.login.util.LoginErrors
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val LOCKOUT_DURATION_MS = 1 * 60 * 1000L // 15 minutes

/**
 * ViewModel responsible for handling login-related business logic and UI state,
 * including time-based lockout after consecutive failed login attempts.
 */
class LoginViewModel(
    private val repository: AuthRepository,
    private val networkMonitor: NetworkMonitor
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState = _uiState.asStateFlow()

    private val navigationChannel = Channel<Unit>(Channel.BUFFERED)
    val navigationEvents = navigationChannel.receiveAsFlow()

    init {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            // Load remembered token if available
            val token = repository.getRememberedToken()
            if (!token.isNullOrEmpty()) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        rememberMe = true
                    )
                }
                navigationChannel.trySend(Unit)
            } else {
                _uiState.update { it.copy(isLoading = false) }
            }

            // Load lockout timestamp from repository (SharedPreferences)
            val lockoutTime = repository.getLockoutTimestamp()
            if (lockoutTime != null && lockoutTime > System.currentTimeMillis()) {
                _uiState.update { it.copy(lockoutExpiresAt = lockoutTime) }
            } else {
                repository.clearLockoutTimestamp()
            }
        }
    }

    fun onUsernameChange(value: String) {
        _uiState.update { it.copy(username = value, errorMessage = null) }
    }

    fun onPasswordChange(value: String) {
        _uiState.update { it.copy(password = value, errorMessage = null) }
    }

    fun onRememberMeChange(value: Boolean) {
        _uiState.update { it.copy(rememberMe = value) }
    }

    fun login() {
        viewModelScope.launch {
            val currentState = _uiState.value

            // Prevent login if account is locked
            if (currentState.isLockedOut) {
                val remainingMinutes =
                    ((currentState.lockoutExpiresAt!! - System.currentTimeMillis()) / 60000) + 1
                _uiState.update {
                    it.copy(
                        errorMessage = "Account locked. Try again in $remainingMinutes minutes."
                    )
                }
                return@launch
            }

            // Check network availability
            if (!networkMonitor.isOnline()) {
                _uiState.update { it.copy(errorMessage = LoginErrors.NO_INTERNET) }
                return@launch
            }

            // Show loading
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val result = repository.login(currentState.username, currentState.password)

            result.fold(
                onSuccess = { token ->
                    if (currentState.rememberMe) repository.rememberToken(token)

                    // Reset failure count & lockout on success
                    repository.clearLockoutTimestamp()
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            failureCount = 0,
                            lockoutExpiresAt = null,
                            errorMessage = null
                        )
                    }

                    navigationChannel.trySend(Unit)
                },
                onFailure = {
                    val failures = _uiState.value.failureCount + 1

                    // If 3 failures, set lockout timestamp
                    val lockoutTimestamp =
                        if (failures >= 3) System.currentTimeMillis() + LOCKOUT_DURATION_MS else null

                    if (lockoutTimestamp != null) repository.setLockoutTimestamp(lockoutTimestamp)

                    _uiState.update {
                        it.copy(
                            failureCount = failures,
                            lockoutExpiresAt = lockoutTimestamp,
                            errorMessage = LoginErrors.INVALID_CREDENTIALS,
                            isLoading = false
                        )
                    }
                }
            )
        }
    }

    fun reset() {
        viewModelScope.launch {
            repository.clearRememberedToken()
            repository.clearLockoutTimestamp()
            _uiState.value = LoginUiState()
        }
    }
}

class LoginViewModelFactory(
    private val repository: AuthRepository,
    private val networkMonitor: NetworkMonitor
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
            return LoginViewModel(repository, networkMonitor) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

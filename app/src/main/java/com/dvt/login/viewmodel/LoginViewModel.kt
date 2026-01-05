package com.dvt.login.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dvt.login.network.NetworkMonitor
import com.dvt.login.repository.AuthRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow

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

class LoginViewModel(
    private val repository: AuthRepository,
    private val isOnline: StateFlow<Boolean>
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState = _uiState.asStateFlow()

    private val navigationChannel = Channel<Unit>(Channel.BUFFERED)
    val navigationEvents = navigationChannel.receiveAsFlow()


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

            if (currentState.isLockedOut) return@launch

            if (!isOnline.value) {
                _uiState.update { it.copy(errorMessage = "No internet connection") }
                return@launch
            }

            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val result = repository.login(currentState.username, currentState.password)
            result.fold(
                onSuccess = { token ->
                    if (currentState.rememberMe) repository.rememberToken(token)
                    _uiState.update { it.copy(isLoading = false, failureCount = 0) }
                    navigationChannel.trySend(Unit)

                },
                onFailure = {
                    val failures = _uiState.value.failureCount + 1
                    _uiState.update {
                        it.copy(
                            failureCount = failures,
                            isLockedOut = failures >= 3,
                            errorMessage = "Invalid Credentials",
                            isLoading = false
                        )
                    }
                }
            )
        }
    }

    fun reset() {
        _uiState.value = LoginUiState() // reset to default state
    }

    fun getRememberedToken(): String? {
        return repository.getRememberedToken()
    }


}

class LoginViewModelFactory(
    private val application: Application,
    private val repository: AuthRepository,
    private val networkMonitor: NetworkMonitor
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
            return LoginViewModel(
                repository = repository,
                isOnline = networkMonitor.isOnline
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}


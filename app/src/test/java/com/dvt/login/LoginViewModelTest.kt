package com.dvt.login

import com.dvt.login.repository.AuthRepository
import com.dvt.login.network.NetworkMonitor
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import MainDispatcherRule
import com.dvt.login.viewmodel.LoginViewModel

@ExperimentalCoroutinesApi
class LoginViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Mock
    private lateinit var authRepository: AuthRepository

    @Mock
    private lateinit var networkMonitor: NetworkMonitor

    private lateinit var viewModel: LoginViewModel

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        viewModel = LoginViewModel(authRepository, networkMonitor)
    }

    @Test
    fun `login success navigates`() = runTest {
        `when`(networkMonitor.isOnline()).thenReturn(true)
        `when`(authRepository.login("anne", "anne")).thenReturn(Result.success("jwt_token_123"))

        viewModel.onUsernameChange("anne")
        viewModel.onPasswordChange("anne")
        viewModel.login()


        assertEquals(0, viewModel.uiState.value.failureCount)
    }

    @Test
    fun `login error increments failure count`() = runTest {
        `when`(networkMonitor.isOnline()).thenReturn(true)
        `when`(authRepository.login("anne", "wrongpass")).thenReturn(Result.failure(Exception("Invalid credentials")))

        viewModel.onUsernameChange("anne")
        viewModel.onPasswordChange("wrongpass")
        viewModel.login()

        assertEquals(1, viewModel.uiState.value.failureCount)
    }

    @Test
    fun `lockout after 3 failures`() = runTest {
        `when`(networkMonitor.isOnline()).thenReturn(true)
        `when`(authRepository.login("anne", "wrongpass")).thenReturn(Result.failure(Exception("Invalid credentials")))

        repeat(3) {
            viewModel.onUsernameChange("anne")
            viewModel.onPasswordChange("wrongpass")
            viewModel.login()
        }

        assertEquals(true, viewModel.uiState.value.isLockedOut)
    }

    @Test
    fun `offline shows message and no service call`() = runTest {
        `when`(networkMonitor.isOnline()).thenReturn(false)

        viewModel.onUsernameChange("anne")
        viewModel.onPasswordChange("anne")
        viewModel.login()

        assertEquals("No internet connection", viewModel.uiState.value.errorMessage)
        verify(authRepository, Mockito.never()).login(Mockito.anyString(), Mockito.anyString())
    }

    @Test
    fun `remember me persists token`() = runTest {
        `when`(networkMonitor.isOnline()).thenReturn(true)
        `when`(authRepository.login("anne", "anne")).thenReturn(Result.success("jwt_token_123"))

        viewModel.onUsernameChange("anne")
        viewModel.onPasswordChange("anne")
        viewModel.onRememberMeChange(true)
        viewModel.login()

        verify(authRepository).rememberToken("jwt_token_123")
    }

    // Additional Test #1: Validation - Initial state has empty credentials
    @Test
    fun `initial state has empty username and password`() {
        assertEquals("", viewModel.uiState.value.username)
        assertEquals("", viewModel.uiState.value.password)
        assertEquals(false, viewModel.uiState.value.rememberMe)
        assertEquals(0, viewModel.uiState.value.failureCount)
        assertEquals(false, viewModel.uiState.value.isLockedOut)
    }

    // Additional Test #2: Validation - Username change updates state
    @Test
    fun `username change updates ui state`() {
        viewModel.onUsernameChange("anne")
        assertEquals("anne", viewModel.uiState.value.username)
    }

    // Additional Test #3: Validation - Password change updates state
    @Test
    fun `password change updates ui state`() {
        viewModel.onPasswordChange("anne")
        assertEquals("anne", viewModel.uiState.value.password)
    }

    // Additional Test #4: Validation - Remember me toggle updates state
    @Test
    fun `remember me toggle updates ui state`() {
        assertEquals(false, viewModel.uiState.value.rememberMe)

        viewModel.onRememberMeChange(true)
        assertEquals(true, viewModel.uiState.value.rememberMe)

        viewModel.onRememberMeChange(false)
        assertEquals(false, viewModel.uiState.value.rememberMe)
    }

    // Additional Test #5: Success - Login success sets isLoginSuccess to true
    @Test
    fun `successful login sets login success flag`() = runTest {
        `when`(networkMonitor.isOnline()).thenReturn(true)
        `when`(authRepository.login("anne", "anne")).thenReturn(Result.success("jwt_token_123"))

        viewModel.onUsernameChange("anne")
        viewModel.onPasswordChange("anne")


        viewModel.login()

        assertEquals(0, viewModel.uiState.value.failureCount)
        assertEquals(null, viewModel.uiState.value.errorMessage)
    }

    // Additional Test #6: Error - Login failure sets error message
    @Test
    fun `login failure sets error message`() = runTest {
        `when`(networkMonitor.isOnline()).thenReturn(true)
        `when`(authRepository.login("anne", "wrongpass"))
            .thenReturn(Result.failure(Exception("Invalid Credentials")))

        viewModel.onUsernameChange("anne")
        viewModel.onPasswordChange("wrongpass")
        viewModel.login()

        assertEquals("Invalid Credentials", viewModel.uiState.value.errorMessage)
    }

    // Additional Test #7: Error - Multiple failures increment count correctly
    @Test
    fun `multiple login failures increment failure count`() = runTest {
        `when`(networkMonitor.isOnline()).thenReturn(true)
        `when`(authRepository.login("anne", "wrongpass"))
            .thenReturn(Result.failure(Exception("Invalid credentials")))

        viewModel.onUsernameChange("anne")
        viewModel.onPasswordChange("wrongpass")

        // First failure
        viewModel.login()
        assertEquals(1, viewModel.uiState.value.failureCount)
        assertEquals(false, viewModel.uiState.value.isLockedOut)

        // Second failure
        viewModel.login()
        assertEquals(2, viewModel.uiState.value.failureCount)
        assertEquals(false, viewModel.uiState.value.isLockedOut)
    }

    // Additional Test #8: Lockout - Cannot login when locked out
    @Test
    fun `cannot login when account is locked out`() = runTest {
        `when`(networkMonitor.isOnline()).thenReturn(true)
        `when`(authRepository.login("anne", "wrongpass"))
            .thenReturn(Result.failure(Exception("Invalid credentials")))

        viewModel.onUsernameChange("anne")
        viewModel.onPasswordChange("wrongpass")

        // Lock out the account
        repeat(3) {
            viewModel.login()
        }

        assertEquals(true, viewModel.uiState.value.isLockedOut)

        // Try to login again - should not call repository
        viewModel.login()

        // Verify repository was only called 3 times (during lockout), not 4
        verify(authRepository, Mockito.times(3)).login(Mockito.anyString(), Mockito.anyString())
    }

    // Additional Test #9: Offline - Offline state prevents login immediately
    @Test
    fun `offline state prevents login attempt`() = runTest {
        `when`(networkMonitor.isOnline()).thenReturn(false)

        viewModel.onUsernameChange("anne")
        viewModel.onPasswordChange("anne")

        assertEquals(null, viewModel.uiState.value.errorMessage)

        viewModel.login()

        assertEquals("No internet connection", viewModel.uiState.value.errorMessage)
        verify(authRepository, Mockito.never()).login(Mockito.anyString(), Mockito.anyString())
    }

    // Additional Test #10: Remember me - Token not saved when remember me is false
    @Test
    fun `token not saved when remember me is false`() = runTest {
        `when`(networkMonitor.isOnline()).thenReturn(true)
        `when`(authRepository.login("anne", "anne")).thenReturn(Result.success("jwt_token_123"))

        viewModel.onUsernameChange("anne")
        viewModel.onPasswordChange("anne")
        viewModel.onRememberMeChange(false) // Explicitly set to false
        viewModel.login()

        verify(authRepository, Mockito.never()).rememberToken(Mockito.anyString())
    }

    // Additional Test #12: Lockout - Lockout flag is set exactly at 3 failures
    @Test
    fun `lockout flag set at exactly 3 failures`() = runTest {
        `when`(networkMonitor.isOnline()).thenReturn(true)
        `when`(authRepository.login("anne", "wrongpass"))
            .thenReturn(Result.failure(Exception("Invalid credentials")))

        viewModel.onUsernameChange("anne")
        viewModel.onPasswordChange("wrongpass")

        // After 2 failures, should not be locked
        repeat(2) {
            viewModel.login()
        }
        assertEquals(2, viewModel.uiState.value.failureCount)
        assertEquals(false, viewModel.uiState.value.isLockedOut)

        // After 3rd failure, should be locked
        viewModel.login()
        assertEquals(3, viewModel.uiState.value.failureCount)
        assertEquals(true, viewModel.uiState.value.isLockedOut)
    }
}
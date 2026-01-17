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
import com.dvt.login.util.LoginErrors
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

    //Login Success
    @Test
    fun `login success navigates`() = runTest {
        `when`(networkMonitor.isOnline()).thenReturn(true)
        `when`(authRepository.login("anne", "gowthami")).thenReturn(Result.success("jwt_token_123"))

        viewModel.onUsernameChange("anne")
        viewModel.onPasswordChange("gowthami")
        viewModel.login()


        assertEquals(0, viewModel.uiState.value.failureCount)
    }

    //Login failure count
    @Test
    fun `login error increments failure count`() = runTest {
        `when`(networkMonitor.isOnline()).thenReturn(true)
        `when`(authRepository.login("anne", "wrongpass")).thenReturn(Result.failure(Exception("Invalid credentials")))

        viewModel.onUsernameChange("anne")
        viewModel.onPasswordChange("wrongpass")
        viewModel.login()

        assertEquals(1, viewModel.uiState.value.failureCount)
    }

    //Lockout
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

    //Offline
    @Test
    fun `offline shows message and no service call`() = runTest {
        `when`(networkMonitor.isOnline()).thenReturn(false)

        viewModel.onUsernameChange("anne")
        viewModel.onPasswordChange("gowthami")
        viewModel.login()

        assertEquals("No internet connection", viewModel.uiState.value.errorMessage)
        verify(authRepository, Mockito.never()).login(Mockito.anyString(), Mockito.anyString())
    }

    //Remember me persist token
    @Test
    fun `remember me persists token`() = runTest {
        `when`(networkMonitor.isOnline()).thenReturn(true)
        `when`(authRepository.login("anne", "gowthami")).thenReturn(Result.success("jwt_token_123"))

        viewModel.onUsernameChange("anne")
        viewModel.onPasswordChange("gowthami")
        viewModel.onRememberMeChange(true)
        viewModel.login()

        verify(authRepository).rememberToken("jwt_token_123")
    }

    // Initial state has empty credentials
    @Test
    fun `initial state has empty username and password`() {
        assertEquals("", viewModel.uiState.value.username)
        assertEquals("", viewModel.uiState.value.password)
        assertEquals(false, viewModel.uiState.value.rememberMe)
        assertEquals(0, viewModel.uiState.value.failureCount)
        assertEquals(false, viewModel.uiState.value.isLockedOut)
    }

    // Username change updates state
    @Test
    fun `username change updates ui state`() {
        viewModel.onUsernameChange("anne")
        assertEquals("anne", viewModel.uiState.value.username)
    }

    // Password change updates state
    @Test
    fun `password change updates ui state`() {
        viewModel.onPasswordChange("gowthami")
        assertEquals("gowthami", viewModel.uiState.value.password)
    }

    //Remember me toggle updates state
    @Test
    fun `remember me toggle updates ui state`() {
        assertEquals(false, viewModel.uiState.value.rememberMe)

        viewModel.onRememberMeChange(true)
        assertEquals(true, viewModel.uiState.value.rememberMe)

        viewModel.onRememberMeChange(false)
        assertEquals(false, viewModel.uiState.value.rememberMe)
    }

    //Login failure sets error message
    @Test
    fun `login failure sets error message`() = runTest {
        `when`(networkMonitor.isOnline()).thenReturn(true)
        `when`(authRepository.login("anne", "wrongpass"))
            .thenReturn(Result.failure(Exception(LoginErrors.INVALID_CREDENTIALS)))

        viewModel.onUsernameChange("anne")
        viewModel.onPasswordChange("wrongpass")
        viewModel.login()

        assertEquals(LoginErrors.INVALID_CREDENTIALS, viewModel.uiState.value.errorMessage)
    }

    // Multiple failures increment count correctly
    @Test
    fun `multiple login failures increment failure count`() = runTest {
        `when`(networkMonitor.isOnline()).thenReturn(true)
        `when`(authRepository.login("anne", "wrongpass"))
            .thenReturn(Result.failure(Exception(LoginErrors.INVALID_CREDENTIALS)))

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

    //Offline - Offline state prevents login immediately
    @Test
    fun `offline state prevents login attempt`() = runTest {
        `when`(networkMonitor.isOnline()).thenReturn(false)

        viewModel.onUsernameChange("anne")
        viewModel.onPasswordChange("gowthami")

        assertEquals(null, viewModel.uiState.value.errorMessage)

        viewModel.login()

        assertEquals(LoginErrors.NO_INTERNET, viewModel.uiState.value.errorMessage)
        verify(authRepository, Mockito.never()).login(Mockito.anyString(), Mockito.anyString())
    }

    // Lockout - Lockout flag is set exactly at 3 failures
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

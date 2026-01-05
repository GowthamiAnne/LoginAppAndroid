package com.dvt.login.viewmodel

import com.dvt.login.network.NetworkMonitor
import com.dvt.login.repository.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {


    private val testDispatcher = StandardTestDispatcher()

    private lateinit var repository: AuthRepository


    private lateinit var networkMonitor: NetworkMonitor
    private lateinit var isOnlineFlow: MutableStateFlow<Boolean>
    private lateinit var viewModel: LoginViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = AuthRepository()
        isOnlineFlow = MutableStateFlow(true)
        viewModel = LoginViewModel(repository, isOnlineFlow)

    }

    @Test
    fun `login button enabled only when username and password valid`() = runTest {
        viewModel.onUsernameChange("")
        viewModel.onPasswordChange("123")
        assertFalse(viewModel.uiState.value.isLoginEnabled)

        viewModel.onUsernameChange("anne")
        viewModel.onPasswordChange("anne")
        assertTrue(viewModel.uiState.value.isLoginEnabled)
    }

    @Test
    fun `successful login emits navigation event`() = runTest {
        viewModel.onUsernameChange("anne")
        viewModel.onPasswordChange("anne")

        viewModel.login()
        advanceUntilIdle()

        val event = viewModel.navigationEvents.first()
        assertNotNull(event)
    }

    @Test
    fun `failed login increments failure count`() = runTest {
        viewModel.onUsernameChange("wrong")
        viewModel.onPasswordChange("wrong")

        viewModel.login()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.failureCount)
        assertEquals("Invalid Credentials", state.errorMessage)
    }

    @Test
    fun `account locks after 3 failures`() = runTest {
        viewModel.onUsernameChange("wrong")
        viewModel.onPasswordChange("wrong")

        repeat(3) {
            viewModel.login()
            advanceUntilIdle()
        }

        val state = viewModel.uiState.value
        assertTrue(state.isLockedOut)
        assertEquals(3, state.failureCount)
    }

    @Test
    fun `offline login shows error and does not proceed`() = runTest {
        isOnlineFlow.value = false

        viewModel.onUsernameChange("user")
        viewModel.onPasswordChange("pass")
        viewModel.login()
        advanceUntilIdle()

        assertEquals(
            "No internet connection",
            viewModel.uiState.value.errorMessage
        )
    }

    @Test
    fun `remember me persists token`() = runTest {
        viewModel.onUsernameChange("anne")
        viewModel.onPasswordChange("anne")
        viewModel.onRememberMeChange(true)

        viewModel.login()
        advanceUntilIdle()

        assertEquals(
            "jwt_token_123",
            repository.getRememberedToken()
        )
    }

    @Test
    fun `reset clears fields loading and token`() = runTest {
        viewModel.onUsernameChange("user")
        viewModel.onPasswordChange("pass")
        viewModel.onRememberMeChange(true)

        viewModel.login()
        advanceUntilIdle()

        viewModel.reset()

        val state = viewModel.uiState.value
        assertEquals("", state.username)
        assertEquals("", state.password)
    }
}

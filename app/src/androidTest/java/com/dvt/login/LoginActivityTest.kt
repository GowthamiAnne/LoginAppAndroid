package com.dvt.login

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dvt.login.viewmodel.LoginViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import com.dvt.login.repository.AuthRepository
import com.dvt.login.userInterface.LoginScreen
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import junit.framework.TestCase.assertFalse

class FakeAuthRepositorySuccess : AuthRepository() {
    var loginCalled = false // track if login was called

    override suspend fun login(username: String, password: String): Result<String> {
        loginCalled = true
        return Result.success("fake_token_123")
    }

    private var token: String? = null
    override fun rememberToken(token: String) {
        this.token = token
        super.rememberToken(token)
    }

    override fun getRememberedToken() = token
}

class FakeAuthRepositoryFailure : AuthRepository() {
    var loginCalled = false

    override suspend fun login(username: String, password: String): Result<String> {
        loginCalled = true
        return Result.failure(Exception("Invalid Credentials"))
    }
}



// ----------------------
// Test class
// ----------------------
@RunWith(AndroidJUnit4::class)
class LoginFailureTest {

    @get:Rule
    val composeRule = createComposeRule()

    // ✅ Common helper method for setting content
    private fun setLoginContent(
        viewModel: LoginViewModel,
        onLoginSuccess: () -> Unit = {}
    ) {
        composeRule.setContent {
            LoginScreen(
                viewModel = viewModel,
                onLoginSuccess = onLoginSuccess
            )
        }
    }
    @Test
    fun login_withInvalidCredentials_showsError() {
        val fakeRepo = FakeAuthRepositoryFailure()
        val viewModel = LoginViewModel(fakeRepo, MutableStateFlow(true))

        setLoginContent(viewModel)


        composeRule.onNodeWithTag("username")
            .performTextInput("wrong")

        composeRule.onNodeWithTag("password")
            .performTextInput("wrong")

        composeRule.onNodeWithTag("loginButton")
            .performClick()

        composeRule.waitUntil(2_000) {
            viewModel.uiState.value.errorMessage != null
        }
    }

    @Test
    fun login_withEmptyCredentials_disablesLoginButton() {
        val fakeRepo = FakeAuthRepositorySuccess()
        val viewModel = LoginViewModel(fakeRepo, MutableStateFlow(true))

        composeRule.setContent {
            LoginScreen(viewModel = viewModel, onLoginSuccess = {})
        }

        composeRule.onNodeWithTag("username")
            .performTextInput("")
        composeRule.onNodeWithTag("password")
            .performTextInput("")

        composeRule.onNodeWithTag("loginButton")
            .assertIsNotEnabled()
    }

    @Test
    fun login_withValidCredentials_enablesLoginButton() {
        val fakeRepo = FakeAuthRepositorySuccess()
        val viewModel = LoginViewModel(fakeRepo, MutableStateFlow(true))

        // Use common method
        setLoginContent(viewModel)

        composeRule.onNodeWithTag("username")
            .performTextInput("anne")
        composeRule.onNodeWithTag("password")
            .performTextInput("anne")

        composeRule.onNodeWithTag("loginButton")
            .assertIsEnabled()
    }

    @Test
    fun login_withValidCredentials_triggersNavigation() {
        val fakeRepo = FakeAuthRepositorySuccess()
        val viewModel = LoginViewModel(fakeRepo, MutableStateFlow(true))

        // Flag to check if navigation lambda is called
        var navigationTriggered = false

        setLoginContent(viewModel) {
            navigationTriggered = true
        }

        // Fill in valid credentials
        composeRule.onNodeWithTag("username")
            .performTextInput("anne")
        composeRule.onNodeWithTag("password")
            .performTextInput("anne")

        // Click login
        composeRule.onNodeWithTag("loginButton")
            .performClick()

        // Wait until login completes
        composeRule.waitUntil(2_000) { viewModel.uiState.value.errorMessage == null }

        // Assert that navigation was triggered
        assert(navigationTriggered) { "Navigation should be triggered on successful login" }
    }

    // 3. Error increments failure count
    @Test
    fun login_withInvalidCredentials_incrementsFailureCount() {
        val viewModel = LoginViewModel(FakeAuthRepositoryFailure(), MutableStateFlow(true))
        setLoginContent(viewModel)

        composeRule.onNodeWithTag("username").performTextInput("wrong")
        composeRule.onNodeWithTag("password").performTextInput("wrong")
        composeRule.onNodeWithTag("loginButton").performClick()

        composeRule.waitUntil(2000) { viewModel.uiState.value.errorMessage != null }
        assertEquals(1, viewModel.uiState.value.failureCount)
    }

    @Test
    fun login_lockoutAfterThreeFailures() {
        val fakeRepo = FakeAuthRepositoryFailure() // always fails
        val viewModel = LoginViewModel(fakeRepo, MutableStateFlow(true))

        // Set up the Compose content
        composeRule.setContent {
            LoginScreen(viewModel = viewModel, onLoginSuccess = {})
        }

        // Function to perform a failed login
        fun performFailedLogin() {
            composeRule.onNodeWithTag("username").performTextInput("wrong")
            composeRule.onNodeWithTag("password").performTextInput("wrong")
            composeRule.onNodeWithTag("loginButton").performClick()
            // Wait for error message to appear
            composeRule.waitUntil(2000) { viewModel.uiState.value.errorMessage != null }
        }

        // Perform 3 failed logins
        repeat(3) { performFailedLogin() }

        // After 3 failures, the lockout should trigger
        assertTrue(viewModel.uiState.value.isLockedOut) // assuming your UIState has isLocked
        assertEquals(3, viewModel.uiState.value.failureCount)

        // Login button should be disabled during lockout
        composeRule.onNodeWithTag("loginButton").assertIsNotEnabled()
    }

    @Test
    fun login_offline_showsOfflineMessage_noServiceCall() {
        val fakeRepo = FakeAuthRepositoryFailure()
        val viewModel = LoginViewModel(fakeRepo, MutableStateFlow(false))

        setLoginContent(viewModel)

        composeRule.onNodeWithTag("username").performTextInput("anne")
        composeRule.onNodeWithTag("password").performTextInput("anne")
        composeRule.onNodeWithTag("loginButton").performClick()

        composeRule.waitUntil(2000) { viewModel.uiState.value.errorMessage != null }

        assertEquals("No internet connection", viewModel.uiState.value.errorMessage)
        assertFalse(fakeRepo.loginCalled)
    }

    @Test
    fun login_rememberMePersistsToken() {
        // Fake repository that tracks login and remembered token
        val fakeRepo = FakeAuthRepositorySuccess()
        val viewModel = LoginViewModel(fakeRepo, MutableStateFlow(true))

        // Compose content
        composeRule.setContent {
            LoginScreen(
                viewModel = viewModel,
                onLoginSuccess = {}
            )
        }

        // Input valid credentials
        composeRule.onNodeWithTag("username").performTextInput("anne")
        composeRule.onNodeWithTag("password").performTextInput("anne")

        // Enable "Remember Me" checkbox
        composeRule.onNodeWithTag("rememberMe").performClick()

        // Click login
        composeRule.onNodeWithTag("loginButton").performClick()

        // Wait until login success is reflected in UIState
        composeRule.waitUntil(2_000) {
            viewModel.uiState.value.errorMessage == null &&
                    viewModel.uiState.value.failureCount == 0
        }

        // Assert the token was remembered in the repository
        assertEquals("fake_token_123", fakeRepo.getRememberedToken())
    }



}

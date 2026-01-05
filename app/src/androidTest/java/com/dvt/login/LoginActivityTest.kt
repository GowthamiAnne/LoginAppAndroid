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
import androidx.test.platform.app.InstrumentationRegistry
import com.dvt.login.network.NetworkMonitor
import kotlinx.coroutines.flow.StateFlow


// ----------------------
// Test class
// ----------------------
@RunWith(AndroidJUnit4::class)
class LoginActivityTest {

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

    //Invalid Credentials
    @Test
    fun login_withInvalidCredentials_showsError() {

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val viewModel = LoginViewModel(AuthRepository(context), NetworkMonitor(context))

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

    //Disable Login Button
    @Test
    fun login_withEmptyCredentials_disablesLoginButton() {

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val viewModel = LoginViewModel(AuthRepository(context), NetworkMonitor(context))

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

    //Enable Login button
    @Test
    fun login_withValidCredentials_enablesLoginButton() {

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val viewModel = LoginViewModel(AuthRepository(context), NetworkMonitor(context))

        // Use common method
        setLoginContent(viewModel)

        composeRule.onNodeWithTag("username")
            .performTextInput("anne")
        composeRule.onNodeWithTag("password")
            .performTextInput("anne")

        composeRule.onNodeWithTag("loginButton")
            .assertIsEnabled()
    }

    //Navigation triggered
    @Test
    fun login_withValidCredentials_triggersNavigation() {

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val viewModel = LoginViewModel(AuthRepository(context), NetworkMonitor(context))

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

    // Error increments failure count
    @Test
    fun login_withInvalidCredentials_incrementsFailureCount() {

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val viewModel = LoginViewModel(AuthRepository(context), NetworkMonitor(context))
        setLoginContent(viewModel)

        composeRule.onNodeWithTag("username").performTextInput("wrong")
        composeRule.onNodeWithTag("password").performTextInput("wrong")
        composeRule.onNodeWithTag("loginButton").performClick()

        composeRule.waitUntil(2000) { viewModel.uiState.value.errorMessage != null }
        assertEquals(1, viewModel.uiState.value.failureCount)
    }

    //Lockout
    @Test
    fun login_lockoutAfterThreeFailures() {

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val viewModel = LoginViewModel(AuthRepository(context), NetworkMonitor(context))

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


    //rememeber me
    @Test
    fun login_rememberMePersistsToken() {

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val repository = AuthRepository(context)
        val viewModel = LoginViewModel(repository, NetworkMonitor(context))

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

        val rememberedToken = runBlocking {
            repository.getRememberedToken()
        }

        // Assert the token was persisted correctly
        assertEquals("jwt_token_123", rememberedToken)
    }



}

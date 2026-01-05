package com.dvt.login.userInterface

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import com.dvt.login.viewmodel.LoginViewModel

@Composable
fun LoginScreen(viewModel: LoginViewModel, onLoginSuccess: () -> Unit) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.navigationEvents.collect { onLoginSuccess() }
    }

    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        OutlinedTextField(
            value = state.username,
            onValueChange = viewModel::onUsernameChange,
            label = { Text("Username") },
            modifier = Modifier.testTag("username")
        )

        OutlinedTextField(
            value = state.password,
            onValueChange = viewModel::onPasswordChange,
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.testTag("password")
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = state.rememberMe,
                onCheckedChange = viewModel::onRememberMeChange,
                modifier = Modifier.testTag("rememberMe")
            )
            Text("Remember me")
        }

        val errorText = when {
            state.isLockedOut -> "Account locked after 3 failures"
            state.errorMessage != null -> state.errorMessage
            else -> null
        }

        errorText?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Button(
            enabled = state.isLoginEnabled,
            onClick = viewModel::login,
            modifier = Modifier.fillMaxWidth().testTag("loginButton")
        ) {
            if (state.isLoading) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            else Text("Login")
        }

    }
}

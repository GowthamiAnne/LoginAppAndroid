package com.dvt.login.userInterface

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.dvt.login.viewmodel.LoginViewModel

@Composable
fun WelcomeScreen(navController: NavController, viewModel: LoginViewModel) {
    // Use Box with contentAlignment to center everything
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Welcome, ${viewModel.uiState.value.username}!",
                style = MaterialTheme.typography.titleLarge
            )

            Button(
                onClick = {
                    viewModel.reset()
                    // Navigate back to login screen
                    navController.navigate("login") {
                        popUpTo("welcome") { inclusive = true } // Remove Welcome from backstack
                    }
                }
            ) {
                Text("Logout")
            }
        }
    }
}

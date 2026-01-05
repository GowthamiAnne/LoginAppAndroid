
package com.dvt.login

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.dvt.login.network.NetworkMonitor
import com.dvt.login.repository.AuthRepository
import com.dvt.login.userInterface.LoginScreen
import com.dvt.login.userInterface.WelcomeScreen
import com.dvt.login.viewmodel.LoginViewModel
import com.dvt.login.viewmodel.LoginViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val factory = LoginViewModelFactory(application, AuthRepository(this), NetworkMonitor(this))

        setContent {
            val navController = rememberNavController()
            val viewModel: LoginViewModel = viewModel(factory = factory)

            NavHost(navController = navController, startDestination = "login") {

                composable("login") {
                    LoginScreen(viewModel) {
                        // Navigate to Welcome screen on successful login
                        navController.navigate("welcome") {
                            popUpTo("login") { inclusive = true }
                        }
                    }
                }

                composable("welcome") {
                    WelcomeScreen(navController = navController, viewModel = viewModel) // pass navController

                }
            }
        }
    }


}

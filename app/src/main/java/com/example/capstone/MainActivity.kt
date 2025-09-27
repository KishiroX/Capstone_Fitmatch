package com.example.capstone

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.capstone.ui.screens.*
import com.example.capstone.ui.theme.AssistanceScreen
import com.example.capstone.ui.screens.TryOnScreen
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Firebase instances
        val auth = FirebaseAuth.getInstance()
        val db = FirebaseFirestore.getInstance()

        setContent {
            AppNavHost()
        }
    }
}

@Composable
fun AppNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = "onboarding") {

        // Onboarding
        composable("onboarding") {
            OnboardingScreen(
                onGetStartedClick = {
                    navController.navigate("login") {
                        popUpTo("onboarding") { inclusive = true }
                    }
                },
                onRegisterClick = {
                    navController.navigate("signup")
                }
            )
        }

        // Login
        composable("login") {
            LoginScreen(
                onLoginClick = { email, password ->
                    if (email.isNotEmpty() && password.isNotEmpty()) {
                        navController.navigate("home") {
                            popUpTo("login") { inclusive = true }
                        }
                    }
                },
                onRegisterClick = {
                    navController.navigate("signup")
                },
                onForgotPasswordClick = {
                    // TODO: Add forgot password functionality
                }
            )
        }

        composable("signup") {
            SignUpScreen(
                onSignUpClick = { name, email, password ->
                    // handled in SignUpScreen directly
                },
                onAlreadyHaveAccountClick = {
                    // navigate to login if you have one
                },
                navController = navController //  pass navController down
            )
        }
        composable("scan") {
            ScanScreen(navController) // your scan screen
        }

        // Result Screen
        composable("result") {
            ResultScreen(navController = navController)
        }

        // Home Screen
        composable("home") {
            HomeScreen(navController = navController)
        }

        // Assistant Screen
        composable("assistant") {
            AssistanceScreen(
                onNavigate = { destination ->
                    when (destination) {
                        "home" -> navController.navigate("home") {
                            popUpTo("assistant") { inclusive = true }
                        }
                        "tryon" -> navController.navigate("tryon")
                    }
                }
            )
        }

        // Try-On Screen
        composable("tryon") {
            TryOnScreen(
                onNavigate = { destination ->
                    when (destination) {
                        "assistant" -> navController.navigate("assistant") {
                            popUpTo("tryon") { inclusive = true }
                        }
                        "home" -> navController.navigate("home") {
                            popUpTo("tryon") { inclusive = true }
                        }
                    }
                }
            )
        }

        // Profile Screen
//        composable("profile") {
//            ProfileScreen(
//                onNavigate = { destination ->
//                    when (destination) {
//                        "home" -> navController.navigate("home") {
//                            popUpTo("profile") { inclusive = true }
//                        }
//                        else -> navController.navigate(destination)
//                    }
//                }
//            )
//        }
    }
}

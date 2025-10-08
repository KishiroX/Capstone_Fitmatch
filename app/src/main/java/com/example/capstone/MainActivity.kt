package com.example.capstone

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
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

        setContent {
            AppNavHost()
        }
    }
}

@Composable
fun AppNavHost(navController: NavHostController = rememberNavController()) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    NavHost(navController = navController, startDestination = "assistant") {

        // -------------------- ONBOARDING --------------------
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

        // -------------------- LOGIN --------------------
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
                onForgotPasswordClick = { /* Optional */ }
            )
        }

        // -------------------- SIGNUP --------------------
        composable("signup") {
            SignUpScreen(
                onSignUpClick = { name, email, password -> /* handled in SignUpScreen */ },
                onAlreadyHaveAccountClick = { navController.navigate("login") },
                navController = navController
            )
        }

        // -------------------- SCAN --------------------
        composable("scan") { ScanScreen(navController) }

        // -------------------- RESULT --------------------
        composable("result") { ResultScreen(navController = navController) }

        // -------------------- HOME --------------------
        composable("home") { HomeScreen(navController = navController) }

        // -------------------- ASSISTANT --------------------
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

        // -------------------- TRY ON --------------------
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

        // -------------------- PROFILE --------------------
        composable("profile") {
            val firebaseUser = auth.currentUser
            var user by remember { mutableStateOf(User()) }
            var bodyMeasurements by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
            var bodyRatios by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

            LaunchedEffect(firebaseUser?.uid) {
                firebaseUser?.uid?.let { uid ->

                    // --- Load user info ---
                    db.collection("users").document(uid).get()
                        .addOnSuccessListener { document ->
                            if (document.exists()) {
                                user = User(
                                    name = document.getString("name")
                                        ?: firebaseUser.displayName
                                        ?: "User",
                                    email = firebaseUser.email ?: "user@example.com",
                                    height = document.getString("height") ?: "170",
                                    weight = document.getString("weight") ?: "65",
                                    age = document.getString("age") ?: "25",
                                    bodyType = document.getString("bodyType") ?: "Rectangle"
                                )
                            }
                        }

                    // --- Load body measurements & ratios ---
                    db.collection("users")
                        .document(uid)
                        .collection("bodyComposition")
                        .document("latest")
                        .get()
                        .addOnSuccessListener { doc ->
                            if (doc.exists()) {
                                val measurements = doc.get("measurements") as? Map<String, String>
                                val ratios = doc.get("ratios") as? Map<String, String>

                                bodyMeasurements = measurements ?: emptyMap()
                                bodyRatios = ratios ?: emptyMap()
                            }
                        }
                }
            }

            // --- Profile Screen ---
            ProfileScreen(
                onNavigate = { destination ->
                    when (destination) {
                        "home" -> navController.navigate("home") {
                            popUpTo("profile") { inclusive = true }
                        }
                        else -> navController.navigate(destination)
                    }
                },
                user = user,
                bodyMeasurements = bodyMeasurements,
                bodyRatios = bodyRatios
            )
        }
    }
}

package com.example.capstone

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.compose.*
import com.example.capstone.ui.screens.LoginScreen
import com.example.capstone.ui.screens.ResultScreen
import com.example.capstone.ui.screens.SignUpScreen
import com.example.capstone.ui.screens.ScanScreen

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request permissions before anything else
        if (!hasRequiredPermissions()) {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, 0)
            return // Prevent setContent from loading before permissions are granted
        }

        setContent {
            val navController = rememberNavController()

            NavHost(navController = navController, startDestination = "login") {

                composable("login") {
                    LoginScreen(
                        onLoginClick = { _, _ ->
                            // Navigate to scan after login (for testing)
                            navController.navigate("scan")
                        },
                        onRegisterClick = {
                            navController.navigate("signup")
                        }
                    )
                }

                composable("signup") {
                    SignUpScreen(
                        onSignUpClick = { _, _, _, _, _, _ ->
                            // Navigate to scan after successful signup
                            navController.navigate("scan")
                        },
                        onAlreadyHaveAccountClick = {
                            navController.popBackStack()
                        }
                    )
                }

                composable("scan") {
                    ScanScreen(navController)
                }
                composable("result") {
                    ResultScreen(navController)
                }

            }

        }
    }

    private fun hasRequiredPermissions(): Boolean {
        return REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    companion object {
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
    }
}

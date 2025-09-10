package com.example.capstone

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.capstone.ui.screens.LoginScreen
import com.example.capstone.ui.screens.ResultScreen
import com.example.capstone.ui.screens.SignUpScreen
import com.example.capstone.ui.screens.ScanScreen

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request permissions if not already granted
        if (!hasRequiredPermissions()) {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, 0)
        }

        setContent {
            val navController = rememberNavController()

            NavHost(navController = navController, startDestination = "login") {

                composable("login") {
                    LoginScreen(
                        onLoginClick = { _, _ ->
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
                            navController.navigate("scan")
                        },
                        onAlreadyHaveAccountClick = {
                            navController.popBackStack()
                        }
                    )
                }

                // ✅ This matches ScanScreen(navController: NavController)
                composable("scan") {
                    ScanScreen(navController = navController)
                }

                // ✅ This matches ResultScreen(navController: NavController)
                composable("result") {
                    ResultScreen(navController = navController)
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
            Manifest.permission.CAMERA
        )
    }
}

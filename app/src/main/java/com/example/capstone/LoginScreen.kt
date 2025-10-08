
package com.example.capstone.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LoginScreen(
    onLoginClick: (String, String) -> Unit = { _, _ -> },
    onRegisterClick: () -> Unit = {},
    onForgotPasswordClick: () -> Unit = {}
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {


        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        listOf(Color(0xFF00C39A), Color(0xFF009C7A))
                    )
                )
                .padding(vertical = 32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Welcome Back",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Sign in to continue your style journey",
                    fontSize = 14.sp,
                    color = Color(0xFFE0F2F1)
                )
            }
        }


        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .offset(y = (-24).dp)
                .shadow(4.dp, RoundedCornerShape(20.dp))
                .background(Color.White, RoundedCornerShape(20.dp))
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {

                Column {
                    Text("Email Address", fontSize = 14.sp, color = Color(0xFF37474F))
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        placeholder = { Text("Enter your email") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Email,
                                contentDescription = "Email",
                                tint = Color.Gray
                            )
                        }
                    )
                }


                Column {
                    Text("Password", fontSize = 14.sp, color = Color(0xFF37474F))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        placeholder = { Text("Enter your password") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Lock,
                                contentDescription = "Password",
                                tint = Color.Gray
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(
                                    imageVector = if (showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                    contentDescription = "Toggle Password",
                                    tint = Color.Gray
                                )
                            }
                        }
                    )
                }


                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = "Forgot Password?",
                        color = Color(0xFF00C39A),
                        modifier = Modifier.clickable { onForgotPasswordClick() },
                        fontSize = 14.sp
                    )
                }


                Button(
                    onClick = {
                        loading = true
                        val auth = FirebaseAuth.getInstance()
                        auth.signInWithEmailAndPassword(email, password)
                            .addOnCompleteListener { task ->
                                loading = false
                                if (task.isSuccessful) {

                                    onLoginClick(email, password)
                                } else {

                                    println("Login failed: ${task.exception?.message}")
                                }
                            }
                    },
                    enabled = email.isNotEmpty() && password.isNotEmpty() && !loading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C39A))
                ) {
                    if (loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Sign In", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }

            }
        }

        Spacer(modifier = Modifier.height(24.dp))


        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Text("Don't have an account? ", color = Color.Gray, fontSize = 14.sp)
            Text(
                text = "Create Account",
                color = Color(0xFF00C39A),
                modifier = Modifier.clickable { onRegisterClick() },
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }


        Spacer(modifier = Modifier.height(24.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .background(Color(0xFFF9FAFB), RoundedCornerShape(16.dp))
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Demo Login", color = Color.Gray, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = {
                    email = "demo@fitmatch.com"
                    password = "demo123"
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF00C39A)),
                border = ButtonDefaults.outlinedButtonBorder
            ) {
                Text("Use Demo Credentials")
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun LoginScreenPreview() {
    LoginScreen()
}

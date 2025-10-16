package com.example.capstone.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.TextFieldDefaults
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController

@Composable
fun SignUpScreen(
    onSignUpClick: (String, String, String) -> Unit = { _, _, _ -> },
    onAlreadyHaveAccountClick: () -> Unit = {},
    navController: NavHostController
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var showConfirmPassword by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }

    // 🔹 New fields
    var age by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    val genderOptions = listOf("Male", "Female", "Other")
    var weight by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }

    val isFormValid = name.isNotEmpty() && email.isNotEmpty() &&
            password.isNotEmpty() && confirmPassword.isNotEmpty()

    fun handleSignUp() {
        if (password != confirmPassword) {
            println("Passwords do not match")
            return
        }
        loading = true

        val auth = FirebaseAuth.getInstance()
        val db = FirebaseFirestore.getInstance()

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                loading = false
                if (task.isSuccessful) {
                    val uid = auth.currentUser?.uid ?: ""

                    val firstName = name.split(" ").firstOrNull() ?: name

                    // 🔹 Include new data
                    val userData = hashMapOf(
                        "uid" to uid,
                        "name" to name,
                        "firstName" to firstName,
                        "email" to email,
                        "age" to age,
                        "gender" to gender,
                        "weight" to weight,
                        "height" to height
                    )

                    db.collection("users").document(uid)
                        .set(userData)
                        .addOnSuccessListener {
                            println("User saved in Firestore with gender, age, weight, and height")
                        }
                        .addOnFailureListener { e ->
                            println("Firestore save failed: ${e.message}")
                        }

                    navController.navigate("scan") {
                        popUpTo("signup") { inclusive = true }
                    }

                } else {
                    println("Auth Error: ${task.exception?.message}")
                }
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(Color(0xFF10B981), Color(0xFF059669))
                    )
                )
                .padding(vertical = 32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "Create Account",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    "Join the style community today",
                    fontSize = 14.sp,
                    color = Color(0xFFC6F6D5)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .offset(y = (-24).dp)
                .background(Color.White, shape = RoundedCornerShape(24.dp))
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = { Text("Enter your full name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    textColor = Color.Black,
                    focusedBorderColor = Color(0xFF10B981),
                    unfocusedBorderColor = Color(0xFFD1D5DB),
                    cursorColor = Color(0xFF10B981),
                    placeholderColor = Color.Gray
                )
            )

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                placeholder = { Text("Enter your email") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    textColor = Color.Black,
                    focusedBorderColor = Color(0xFF10B981),
                    unfocusedBorderColor = Color(0xFFD1D5DB),
                    cursorColor = Color(0xFF10B981),
                    placeholderColor = Color.Gray
                )
            )

            // 🔹 Age
            OutlinedTextField(
                value = age,
                onValueChange = { age = it },
                placeholder = { Text("Enter your age") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    textColor = Color.Black,
                    focusedBorderColor = Color(0xFF10B981),
                    unfocusedBorderColor = Color(0xFFD1D5DB),
                    cursorColor = Color(0xFF10B981),
                    placeholderColor = Color.Gray
                )
            )

            // 🔹 Gender Spinner
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = gender,
                    onValueChange = {},
                    placeholder = { Text("Select Gender") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expanded = true },
                    enabled = false,
                    readOnly = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        disabledTextColor = Color.Black,
                        disabledBorderColor = Color(0xFFD1D5DB),
                        disabledPlaceholderColor = Color.Gray,
                        cursorColor = Color(0xFF10B981)
                    )
                )
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                ) {
                    genderOptions.forEach { option ->
                        DropdownMenuItem(onClick = {
                            gender = option
                            expanded = false
                        }) {
                            Text(option)
                        }
                    }
                }
            }

            // 🔹 Weight
            OutlinedTextField(
                value = weight,
                onValueChange = { weight = it },
                placeholder = { Text("Enter your weight (kg)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    textColor = Color.Black,
                    focusedBorderColor = Color(0xFF10B981),
                    unfocusedBorderColor = Color(0xFFD1D5DB),
                    cursorColor = Color(0xFF10B981),
                    placeholderColor = Color.Gray
                )
            )

            // 🔹 Height
            OutlinedTextField(
                value = height,
                onValueChange = { height = it },
                placeholder = { Text("Enter your height (inches)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    textColor = Color.Black,
                    focusedBorderColor = Color(0xFF10B981),
                    unfocusedBorderColor = Color(0xFFD1D5DB),
                    cursorColor = Color(0xFF10B981),
                    placeholderColor = Color.Gray
                )
            )

            // 🔹 Password Field
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    placeholder = { Text("Create a password") },
                    singleLine = true,
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        textColor = Color.Black,
                        focusedBorderColor = Color(0xFF10B981),
                        unfocusedBorderColor = Color(0xFFD1D5DB),
                        cursorColor = Color(0xFF10B981),
                        placeholderColor = Color.Gray
                    )
                )
                Text(
                    if (showPassword) "Hide" else "Show",
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 16.dp)
                        .clickable { showPassword = !showPassword },
                    color = Color.Gray
                )
            }

            // 🔹 Confirm Password Field
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    placeholder = { Text("Confirm your password") },
                    singleLine = true,
                    visualTransformation = if (showConfirmPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        textColor = Color.Black,
                        focusedBorderColor = Color(0xFF10B981),
                        unfocusedBorderColor = Color(0xFFD1D5DB),
                        cursorColor = Color(0xFF10B981),
                        placeholderColor = Color.Gray
                    )
                )
                Text(
                    if (showConfirmPassword) "Hide" else "Show",
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 16.dp)
                        .clickable { showConfirmPassword = !showConfirmPassword },
                    color = Color.Gray
                )
            }

            // 🔹 Sign Up Button
            Button(
                onClick = { handleSignUp() },
                enabled = isFormValid && !loading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF10B981))
            ) {
                if (loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Create Account", color = Color.White)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text("Already have an account? ", color = Color.Gray)
                Text(
                    "Sign In",
                    color = Color(0xFF10B981),
                    modifier = Modifier.clickable { onAlreadyHaveAccountClick() },
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .background(Color(0xFFF0FDF4), RoundedCornerShape(12.dp))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "By creating an account, you agree to our Terms of Service and Privacy Policy",
                color = Color.Gray,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

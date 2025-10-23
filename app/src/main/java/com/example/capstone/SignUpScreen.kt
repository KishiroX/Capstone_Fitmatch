package com.example.capstone.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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

    var age by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    val genderOptions = listOf("Male", "Female", "Other")
    var weight by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }

    // Terms checkbox
    var agreedToTerms by remember { mutableStateOf(false) }

    // Validation error states
    var nameError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var ageError by remember { mutableStateOf<String?>(null) }
    var genderError by remember { mutableStateOf<String?>(null) }
    var weightError by remember { mutableStateOf<String?>(null) }
    var heightError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var confirmPasswordError by remember { mutableStateOf<String?>(null) }
    var signUpError by remember { mutableStateOf<String?>(null) }

    // Validation functions
    fun validateName(name: String): String? {
        return when {
            name.isEmpty() -> "Name is required"
            name.length < 2 -> "Name must be at least 2 characters"
            !name.matches(Regex("^[a-zA-Z\\s]+$")) -> "Name can only contain letters"
            else -> null
        }
    }

    fun validateEmail(email: String): String? {
        return when {
            email.isEmpty() -> "Email is required"
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> "Invalid email format"
            else -> null
        }
    }

    fun validateAge(age: String): String? {
        return when {
            age.isEmpty() -> "Age is required"
            age.toIntOrNull() == null -> "Age must be a number"
            age.toInt() < 13 -> "You must be at least 13 years old"
            age.toInt() > 120 -> "Please enter a valid age"
            else -> null
        }
    }

    fun validateWeight(weight: String): String? {
        return when {
            weight.isEmpty() -> "Weight is required"
            weight.toFloatOrNull() == null -> "Weight must be a number"
            weight.toFloat() < 20 -> "Please enter a valid weight"
            weight.toFloat() > 500 -> "Please enter a valid weight"
            else -> null
        }
    }

    fun validateHeight(height: String): String? {
        return when {
            height.isEmpty() -> "Height is required"
            height.toFloatOrNull() == null -> "Height must be a number"
            height.toFloat() < 24 -> "Please enter a valid height (inches)"
            height.toFloat() > 96 -> "Please enter a valid height (inches)"
            else -> null
        }
    }

    fun validatePassword(password: String): String? {
        return when {
            password.isEmpty() -> "Password is required"
            password.length < 6 -> "Password must be at least 6 characters"
            !password.any { it.isDigit() } -> "Password must contain at least one number"
            else -> null
        }
    }

    fun validateConfirmPassword(password: String, confirmPassword: String): String? {
        return when {
            confirmPassword.isEmpty() -> "Please confirm your password"
            password != confirmPassword -> "Passwords do not match"
            else -> null
        }
    }

    // Show error dialog
    signUpError?.let { error ->
        AlertDialog(
            onDismissRequest = { signUpError = null },
            title = { Text("Sign Up Failed") },
            text = { Text(error) },
            confirmButton = {
                TextButton(onClick = { signUpError = null }) {
                    Text("OK", color = Color(0xFF10B981))
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }

    fun handleSignUp() {
        // Validate all fields
        val nameValidation = validateName(name)
        val emailValidation = validateEmail(email)
        val ageValidation = validateAge(age)
        val weightValidation = validateWeight(weight)
        val heightValidation = validateHeight(height)
        val passwordValidation = validatePassword(password)
        val confirmPasswordValidation = validateConfirmPassword(password, confirmPassword)

        nameError = nameValidation
        emailError = emailValidation
        ageError = ageValidation
        genderError = if (gender.isEmpty()) "Please select a gender" else null
        weightError = weightValidation
        heightError = heightValidation
        passwordError = passwordValidation
        confirmPasswordError = confirmPasswordValidation

        // Check if any validation failed
        if (nameValidation != null || emailValidation != null || ageValidation != null ||
            gender.isEmpty() || weightValidation != null || heightValidation != null ||
            passwordValidation != null || confirmPasswordValidation != null) {
            return
        }

        // Check if terms are agreed
        if (!agreedToTerms) {
            signUpError = "Please agree to the Terms of Service and Privacy Policy to continue"
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

                    val userData = hashMapOf(
                        "uid" to uid,
                        "name" to name,
                        "firstName" to firstName,
                        "email" to email,
                        "age" to age,
                        "gender" to gender,
                        "weight" to weight,
                        "height" to height,
                        "level" to 1,
                        "currentXP" to 0,
                        "streak" to 0,
                        "totalOutfits" to 0,
                        "maxStorage" to 20,
                        "currentStorage" to 0,
                        "createdAt" to com.google.firebase.Timestamp.now()
                    )

                    db.collection("users").document(uid)
                        .set(userData)
                        .addOnSuccessListener {
                            android.util.Log.d("SignUpScreen", "✅ User saved to Firestore")
                            navController.navigate("scan") {
                                popUpTo("signup") { inclusive = true }
                            }
                        }
                        .addOnFailureListener { e ->
                            android.util.Log.e("SignUpScreen", "❌ Firestore error: ${e.message}")
                            signUpError = "Account created but failed to save profile: ${e.message}"
                        }
                } else {
                    val errorMessage = when {
                        task.exception?.message?.contains("email address is already in use") == true ->
                            "This email is already registered"
                        task.exception?.message?.contains("password is invalid") == true ->
                            "Password is too weak"
                        task.exception?.message?.contains("network error") == true ->
                            "Network error. Please check your connection"
                        else -> task.exception?.message ?: "Sign up failed. Please try again"
                    }
                    android.util.Log.e("SignUpScreen", "❌ Auth error: $errorMessage")
                    signUpError = errorMessage
                }
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(rememberScrollState())
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
            // Name Field
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        nameError = null
                    },
                    placeholder = { Text("Enter your full name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    isError = nameError != null,
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        textColor = Color.Black,
                        focusedBorderColor = Color(0xFF10B981),
                        unfocusedBorderColor = Color(0xFFD1D5DB),
                        errorBorderColor = Color(0xFFEF4444),
                        cursorColor = Color(0xFF10B981),
                        placeholderColor = Color.Gray
                    )
                )
                if (nameError != null) {
                    Text(
                        text = nameError!!,
                        color = Color(0xFFEF4444),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                    )
                }
            }

            // Email Field
            Column {
                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it.trim()
                        emailError = null
                    },
                    placeholder = { Text("Enter your email") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    isError = emailError != null,
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        textColor = Color.Black,
                        focusedBorderColor = Color(0xFF10B981),
                        unfocusedBorderColor = Color(0xFFD1D5DB),
                        errorBorderColor = Color(0xFFEF4444),
                        cursorColor = Color(0xFF10B981),
                        placeholderColor = Color.Gray
                    )
                )
                if (emailError != null) {
                    Text(
                        text = emailError!!,
                        color = Color(0xFFEF4444),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                    )
                }
            }

            // Age Field
            Column {
                OutlinedTextField(
                    value = age,
                    onValueChange = {
                        if (it.isEmpty() || it.all { char -> char.isDigit() }) {
                            age = it
                            ageError = null
                        }
                    },
                    placeholder = { Text("Enter your age") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    isError = ageError != null,
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        textColor = Color.Black,
                        focusedBorderColor = Color(0xFF10B981),
                        unfocusedBorderColor = Color(0xFFD1D5DB),
                        errorBorderColor = Color(0xFFEF4444),
                        cursorColor = Color(0xFF10B981),
                        placeholderColor = Color.Gray
                    )
                )
                if (ageError != null) {
                    Text(
                        text = ageError!!,
                        color = Color(0xFFEF4444),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                    )
                }
            }

            // Gender Dropdown
            Column {
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = gender,
                        onValueChange = {},
                        placeholder = { Text("Select Gender") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                expanded = true
                                genderError = null
                            },
                        enabled = false,
                        readOnly = true,
                        shape = RoundedCornerShape(12.dp),
                        isError = genderError != null,
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            disabledTextColor = Color.Black,
                            disabledBorderColor = if (genderError != null) Color(0xFFEF4444) else Color(0xFFD1D5DB),
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
                                genderError = null
                                expanded = false
                            }) {
                                Text(option)
                            }
                        }
                    }
                }
                if (genderError != null) {
                    Text(
                        text = genderError!!,
                        color = Color(0xFFEF4444),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                    )
                }
            }

            // Weight Field
            Column {
                OutlinedTextField(
                    value = weight,
                    onValueChange = {
                        if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) {
                            weight = it
                            weightError = null
                        }
                    },
                    placeholder = { Text("Enter your weight (kg)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    isError = weightError != null,
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        textColor = Color.Black,
                        focusedBorderColor = Color(0xFF10B981),
                        unfocusedBorderColor = Color(0xFFD1D5DB),
                        errorBorderColor = Color(0xFFEF4444),
                        cursorColor = Color(0xFF10B981),
                        placeholderColor = Color.Gray
                    )
                )
                if (weightError != null) {
                    Text(
                        text = weightError!!,
                        color = Color(0xFFEF4444),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                    )
                }
            }

            // Height Field
            Column {
                OutlinedTextField(
                    value = height,
                    onValueChange = {
                        if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) {
                            height = it
                            heightError = null
                        }
                    },
                    placeholder = { Text("Enter your height (inches)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    isError = heightError != null,
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        textColor = Color.Black,
                        focusedBorderColor = Color(0xFF10B981),
                        unfocusedBorderColor = Color(0xFFD1D5DB),
                        errorBorderColor = Color(0xFFEF4444),
                        cursorColor = Color(0xFF10B981),
                        placeholderColor = Color.Gray
                    )
                )
                if (heightError != null) {
                    Text(
                        text = heightError!!,
                        color = Color(0xFFEF4444),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                    )
                }
            }

            // Password Field
            Column {
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            passwordError = null
                        },
                        placeholder = { Text("Create a password") },
                        singleLine = true,
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        isError = passwordError != null,
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            textColor = Color.Black,
                            focusedBorderColor = Color(0xFF10B981),
                            unfocusedBorderColor = Color(0xFFD1D5DB),
                            errorBorderColor = Color(0xFFEF4444),
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
                if (passwordError != null) {
                    Text(
                        text = passwordError!!,
                        color = Color(0xFFEF4444),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                    )
                }
            }

            // Confirm Password Field
            Column {
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = {
                            confirmPassword = it
                            confirmPasswordError = null
                        },
                        placeholder = { Text("Confirm your password") },
                        singleLine = true,
                        visualTransformation = if (showConfirmPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        isError = confirmPasswordError != null,
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            textColor = Color.Black,
                            focusedBorderColor = Color(0xFF10B981),
                            unfocusedBorderColor = Color(0xFFD1D5DB),
                            errorBorderColor = Color(0xFFEF4444),
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
                if (confirmPasswordError != null) {
                    Text(
                        text = confirmPasswordError!!,
                        color = Color(0xFFEF4444),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                    )
                }
            }

            // Terms and Conditions Checkbox
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = agreedToTerms,
                    onCheckedChange = { agreedToTerms = it },
                    colors = CheckboxDefaults.colors(
                        checkedColor = Color(0xFF10B981),
                        uncheckedColor = Color(0xFFD1D5DB),
                        checkmarkColor = Color.White
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "I agree to the Terms of Service and Privacy Policy",
                    fontSize = 13.sp,
                    color = if (agreedToTerms) Color(0xFF374151) else Color(0xFF6B7280),
                    modifier = Modifier.clickable { agreedToTerms = !agreedToTerms }
                )
            }

            // Sign Up Button
            Button(
                onClick = { handleSignUp() },
                enabled = !loading && agreedToTerms,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color(0xFF10B981),
                    disabledBackgroundColor = Color(0xFF10B981).copy(alpha = 0.5f)
                )
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
    }
}
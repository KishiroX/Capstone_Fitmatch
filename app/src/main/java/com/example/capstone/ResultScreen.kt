package com.example.capstone.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.capstone.analysis.BodyAnalyzer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.util.Log
import com.google.firebase.firestore.SetOptions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(navController: NavController) {
    val context = LocalContext.current
    val bodyAnalyzer = remember { BodyAnalyzer(context) }

    val savedBitmap = remember {
        navController.previousBackStackEntry
            ?.savedStateHandle
            ?.get<Bitmap>("capturedBitmap")
    }

    // Holds analyzer results (Map<String, Any>)
    val resultData = remember { mutableStateOf<Map<String, Any>>(emptyMap()) }

    // Editable text states (one for each field)
    val editableFields = remember { mutableStateMapOf<String, String>() }

    LaunchedEffect(savedBitmap) {
        savedBitmap?.let { bitmap ->
            val bodyData = withContext(Dispatchers.Default) {
                try {
                    val poseResult = bodyAnalyzer.analyze(bitmap)
                    val data = bodyAnalyzer.calculateFullBodyComposition(bitmap, poseResult)
                    if (data.isEmpty()) mapOf("Error" to "Pose not fully detected")
                    else data
                } catch (e: Exception) {
                    mapOf("Error" to "Analysis failed: ${e.localizedMessage}")
                }
            }
            resultData.value = bodyData
            // Initialize editable fields with detected values
            editableFields.clear()
            bodyData.forEach { (key, value) ->
                editableFields[key] = when (value) {
                    is Float -> String.format("%.2f", value)
                    else -> value.toString()
                }
            }
            Log.d("ResultScreen", "📊 Analysis finished: $bodyData")
        }
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {

        // Top progress bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(Color(0xFF10B981), Color(0xFF0D9488))
                    )
                )
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(horizontalArrangement = Arrangement.Center) {
                repeat(4) { index ->
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(
                                if (index < 4) Color.White else Color.White.copy(alpha = 0.3f)
                            )
                    )
                    if (index != 3) Spacer(modifier = Modifier.width(8.dp))
                }
            }
        }

        // Content area
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Text(
                        "Analysis Complete!",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF111827)
                    )
                    Text(
                        "Here’s your scan result summary",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )

                    // Measurements
                    SectionHeader("Measurements")
                    val measurements = listOf(
                        "Shoulder Width", "Hip Width", "Torso Length",
                        "Leg Length", "Arm Length", "Estimated Skin Color"
                    )
                    measurements.forEach { key ->
                        EditableResultField(
                            label = key,
                            value = editableFields[key] ?: "",
                            onValueChange = { editableFields[key] = it }
                        )
                    }

                    // Ratios
                    SectionHeader("Body Ratios")
                    val ratios = listOf(
                        "Shoulder-to-Hip Ratio",
                        "Torso-to-Leg Ratio",
                        "Arm-to-Leg Ratio",
                        "Shoulder-to-Height Ratio"
                    )
                    ratios.forEach { key ->
                        EditableResultField(
                            label = key,
                            value = editableFields[key] ?: "",
                            onValueChange = { editableFields[key] = it }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Confirm Button
                    Button(
                        onClick = {
                            val userId = FirebaseAuth.getInstance().currentUser?.uid
                            val data = editableFields

                            Log.d("ResultScreen", "Confirm clicked. UserId: $userId, Data: $data")

                            if (userId != null && data.isNotEmpty() && !data.containsKey("Error")) {
                                val db = FirebaseFirestore.getInstance()

                                val safeData = data.mapValues { (_, v) ->
                                    v.toString()
                                }

                                val dataToSave = safeData + mapOf(
                                    "timestamp" to com.google.firebase.Timestamp.now()
                                )

                                Log.d("ResultScreen", "🚀 Saving to Firestore: $dataToSave")

                                db.collection("users")
                                    .document(userId)
                                    .collection("bodyComposition")
                                    .document("latest")
                                    .set(dataToSave, SetOptions.merge())
                                    .addOnSuccessListener {
                                        Log.d("Firestore", "✅ Saved successfully for $userId")
                                        navController.navigate("home") {
                                            popUpTo("result") { inclusive = true }
                                            launchSingleTop = true
                                        }
                                    }
                                    .addOnFailureListener { e ->
                                        Log.e("Firestore", "❌ Save failed: ${e.message}", e)
                                        navController.navigate("home") {
                                            popUpTo("result") { inclusive = true }
                                            launchSingleTop = true
                                        }
                                    }
                            } else {
                                Log.w("ResultScreen", "⚠️ No valid user or data. Skipping save.")
                                navController.navigate("home") {
                                    popUpTo("result") { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                        },
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C8A0)),
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth()
                    ) {
                        Text("Confirm", color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 18.sp,
        fontWeight = FontWeight.SemiBold,
        color = Color(0xFF111827)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditableResultField(label: String, value: String, onValueChange: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = label, color = Color(0xFF374151), fontSize = 14.sp)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp)),
            textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color(0xFFF9FAFB),
                unfocusedContainerColor = Color(0xFFF9FAFB),
                focusedBorderColor = Color(0xFF00C8A0),
                unfocusedBorderColor = Color.LightGray,
                cursorColor = Color(0xFF00C8A0),
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black
            ),
            singleLine = true
        )
    }
}

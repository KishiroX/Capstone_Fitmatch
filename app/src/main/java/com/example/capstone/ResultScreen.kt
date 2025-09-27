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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.capstone.analysis.BodyAnalyzer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.util.Log

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
    val resultData = remember { mutableStateOf<Map<String, Any>>(emptyMap()) }

    // 🔹 Analyze bitmap only
    LaunchedEffect(savedBitmap) {
        savedBitmap?.let { bitmap ->
            val bodyData = withContext(Dispatchers.Default) {
                try {
                    val poseResult = bodyAnalyzer.analyze(bitmap)
                    val data = bodyAnalyzer.calculateFullBodyComposition(bitmap, poseResult)
                    if (data.isEmpty()) {
                        mapOf("Error" to "Pose not fully detected")
                    } else data
                } catch (e: Exception) {
                    mapOf("Error" to "Analysis failed: ${e.localizedMessage}")
                }
            }
            resultData.value = bodyData
            Log.d("ResultScreen", "📊 Analysis finished: $bodyData")
        }
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Gradient Header
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

        // Content card
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

                    // Measurements Section
                    SectionHeader("Measurements")
                    val measurements = listOf(
                        "Shoulder Width", "Hip Width", "Torso Length",
                        "Leg Length", "Arm Length", "Estimated Skin Color"
                    )
                    measurements.forEach { key ->
                        ResultField(key, resultData.value[key])
                    }

                    // Ratios Section
                    SectionHeader("Body Ratios")
                    val ratios = listOf(
                        "Shoulder-to-Hip Ratio",
                        "Torso-to-Leg Ratio",
                        "Arm-to-Leg Ratio",
                        "Shoulder-to-Height Ratio"
                    )
                    ratios.forEach { key ->
                        ResultField(key, resultData.value[key])
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 🔹 Confirm button saves + navigates
                    Button(
                        onClick = {
                            val userId = FirebaseAuth.getInstance().currentUser?.uid
                            val data = resultData.value

                            Log.d("ResultScreen", "Confirm clicked. UserId: $userId, Data: $data")

                            if (userId != null && data.isNotEmpty() && !data.containsKey("Error")) {
                                val db = FirebaseFirestore.getInstance()

                                // ✅ Ensure only supported Firestore data types
                                val safeData = data.mapValues { (_, v) ->
                                    when (v) {
                                        is Float, is Double, is Int, is Long, is String, is Boolean -> v
                                        else -> v.toString()
                                    }
                                }

                                val dataToSave = safeData + mapOf("timestamp" to com.google.firebase.Timestamp.now())

                                Log.d("ResultScreen", "🚀 Saving to Firestore: $dataToSave")

                                db.collection("users")
                                    .document(userId)
                                    .collection("bodyComposition")
                                    .add(dataToSave)
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

@Composable
fun ResultField(label: String, value: Any?) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = label, color = Color(0xFF374151), fontSize = 14.sp)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFFF9FAFB))
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text = when (value) {
                    is Float -> String.format("%.2f", value)
                    is String -> value
                    else -> "N/A"
                },
                color = Color.Black,
                fontSize = 14.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

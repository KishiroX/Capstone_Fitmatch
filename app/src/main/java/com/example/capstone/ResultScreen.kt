package com.example.capstone.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.capstone.analysis.BodyAnalyzer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
//asdfasdffaasdfasdf
    LaunchedEffect(savedBitmap) {
        savedBitmap?.let { bitmap ->
            val bodyData = withContext(Dispatchers.Default) {
                try {
                    val poseResult = bodyAnalyzer.analyze(bitmap)
                    val data = bodyAnalyzer.calculateFullBodyComposition(bitmap, poseResult)

                    if (data.isEmpty()) {
                        mapOf("Error" to "Pose not fully detected")
                    } else {
                        data
                    }
                } catch (e: Exception) {
                    mapOf("Error" to "Analysis failed: ${e.localizedMessage}")
                }
            }
            resultData.value = bodyData
        }
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF00C8A0)),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(50.dp))

        Text(
            "Result",
            fontSize = 24.sp,
            color = Color.Black,
            modifier = Modifier.padding(bottom = 20.dp)
        )

        // Middle scrollable area (weight occupies remaining space)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp))
                .background(Color(0xFFF6FFFA))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val labelColor = Color.Black
                val fieldColor = Color(0xFFE6FFF3)

                // Measurements
                Text("Measurements", fontSize = 18.sp, color = labelColor)
                val measurements = listOf(
                    "Shoulder Width",
                    "Hip Width",
                    "Torso Length",
                    "Leg Length",
                    "Arm Length",
                    "Estimated Skin Color"
                )
                measurements.forEach { key ->
                    ResultField(key, resultData.value[key], labelColor, fieldColor)
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Ratios
                Text("Body Ratios", fontSize = 18.sp, color = labelColor)
                val ratios = listOf(
                    "Shoulder-to-Hip Ratio",
                    "Torso-to-Leg Ratio",
                    "Arm-to-Leg Ratio",
                    "Shoulder-to-Height Ratio"
                )
                ratios.forEach { key ->
                    ResultField(key, resultData.value[key], labelColor, fieldColor)
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        // Bottom button stays fixed
        Button(
            onClick = { navController.popBackStack() },
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

@Composable
fun ResultField(label: String, value: Any?, labelColor: Color, fieldColor: Color) {
    Column(horizontalAlignment = Alignment.Start, modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            color = labelColor,
            fontSize = 16.sp
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(fieldColor)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            contentAlignment = Alignment.CenterStart
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

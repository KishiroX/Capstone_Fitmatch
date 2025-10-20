package com.example.capstone.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.capstone.missions.MissionManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AdScreen(
    navController: NavController,
    duration: Int // 30 or 60 seconds
) {
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
    val db = FirebaseFirestore.getInstance()
    val missionManager = remember { MissionManager(userId, db) }
    val scope = rememberCoroutineScope()

    var timeRemaining by remember { mutableStateOf(duration) }
    var canClose by remember { mutableStateOf(false) }

    // Block back button during ad
    BackHandler(enabled = !canClose) {
        // Do nothing - prevent user from exiting
    }

    // Countdown timer
    LaunchedEffect(Unit) {
        while (timeRemaining > 0) {
            delay(1000)
            timeRemaining--
        }
        canClose = true

        // Auto-complete mission when countdown finishes
        val missionId = if (duration == 30) "watchAd30s" else "watchAd1min"
        missionManager.completeMission(missionId)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1F2937))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Fake video player
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF4338CA), Color(0xFF6366F1))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(48.dp)
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    Text(
                        "FitMatch Premium",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        "Unlock unlimited wardrobe space!",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            // Countdown
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF374151)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        if (canClose) "Ad Complete!" else "Please wait...",
                        fontSize = 16.sp,
                        color = if (canClose) Color(0xFF10B981) else Color.White,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        if (canClose) "You can close now" else "$timeRemaining seconds",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (canClose) Color(0xFF10B981) else Color.White
                    )

                    Spacer(Modifier.height(16.dp))

                    LinearProgressIndicator(
                        progress = 1f - (timeRemaining.toFloat() / duration),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = Color(0xFF10B981),
                        trackColor = Color(0xFF4B5563)
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // Close button (only enabled after countdown)
            Button(
                onClick = {
                    navController.navigateUp()
                },
                enabled = canClose,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF10B981),
                    disabledContainerColor = Color(0xFF4B5563)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    if (canClose) "Claim Reward & Close" else "Please wait...",
                    fontSize = 16.sp,
                    color = Color.White,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            Spacer(Modifier.height(12.dp))

            Text(
                "🎁 +${if (duration == 30) 15 else 15} XP",
                fontSize = 14.sp,
                color = Color(0xFF10B981),
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
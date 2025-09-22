// OnboardingScreen.kt
package com.example.capstone.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun OnboardingScreen(
    onGetStartedClick: () -> Unit = {},   // ✅ renamed for MainActivity
    onRegisterClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Header with gradient
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF00C39A), Color(0xFF009C7A))
                    )
                )
                .padding(top = 40.dp, bottom = 32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Logo circle
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(Color.White, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = android.R.drawable.ic_menu_gallery), // TODO replace with ic_shirt
                        contentDescription = "Logo",
                        tint = Color(0xFF00C39A),
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Welcome to Fitmatch",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Your personal style companion",
                    fontSize = 14.sp,
                    color = Color(0xFFE0F2F1)
                )
            }
        }

        // Feature list
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            FeatureItem(
                bgColor = Color(0xFFE0F7EA),
                iconColor = Color(0xFF00C39A),
                title = "AI Body Analysis",
                description = "Upload your photo for personalized styling"
            )
            FeatureItem(
                bgColor = Color(0xFFE3F2FD),
                iconColor = Color(0xFF1E88E5),
                title = "Smart Recommendations",
                description = "Get outfit suggestions for any occasion"
            )
            FeatureItem(
                bgColor = Color(0xFFF3E5F5),
                iconColor = Color(0xFF8E24AA),
                title = "Boost Confidence",
                description = "Feel great in clothes that suit you"
            )
        }

        // Illustration box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .background(Color(0xFFF0FAF9), RoundedCornerShape(16.dp))
                .padding(vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(Color(0xFF00C39A), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = android.R.drawable.ic_menu_gallery), // TODO replace with ic_shirt
                        contentDescription = "Illustration",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Discover your perfect style today",
                    fontSize = 14.sp,
                    color = Color(0xFF37474F)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Buttons
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = onGetStartedClick,   // ✅ now calls correct callback
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C39A))
            ) {
                Text("Get Started", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(12.dp))

            TextButton(onClick = onRegisterClick) {
                Text(
                    "I already have an account",
                    fontSize = 14.sp,
                    color = Color(0xFF6B6B6B)
                )
            }
        }
    }
}

@Composable
fun FeatureItem(
    bgColor: Color,
    iconColor: Color,
    title: String,
    description: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(bgColor, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = android.R.drawable.ic_menu_gallery), // TODO replace with correct icons
                contentDescription = title,
                tint = iconColor,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column {
            Text(text = title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
            Text(text = description, fontSize = 13.sp, color = Color.Gray)
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun OnboardingScreenPreview() {
    OnboardingScreen()
}

// ProfileScreen.kt
package com.example.capstone.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Simple user model
data class User(
    val name: String = "User Name",
    val email: String = "user@example.com",
    val height: String = "170",
    val weight: String = "65",
    val age: String = "25",
    val bodyType: String = "Rectangle"
)

data class BodyTypeInfo(
    val description: String,
    val tips: List<String>,
    val emoji: String
)

@Composable
fun ProfileScreen(onNavigate: (String) -> Unit, user: User) {
    var isEditing by remember { mutableStateOf(false) }
    var height by remember { mutableStateOf(user.height) }
    var weight by remember { mutableStateOf(user.weight) }
    var age by remember { mutableStateOf(user.age) }

    val bodyTypeInfo = mapOf(
        "Rectangle" to BodyTypeInfo(
            description = "Well-balanced proportions with similar bust, waist, and hip measurements",
            tips = listOf(
                "Create curves with fitted tops and flare bottoms",
                "Use belts to define your waist",
                "Layer different textures for visual interest",
                "Try wrap dresses and peplum tops"
            ),
            emoji = "👤"
        )
    )
    val info = bodyTypeInfo[user.bodyType] ?: bodyTypeInfo["Rectangle"]!!

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(Color(0xFFF9FAFB))
            .padding(bottom = 16.dp)
    ) {
        // Header (gradient)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color(0xFF10B981), Color(0xFF0D9488))
                    )
                )
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { onNavigate("home") }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Text("Profile", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                }
                IconButton(onClick = { /* open settings */ }) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Profile Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(20.dp),
            elevation = 6.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFD1FAE5)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            user.name.firstOrNull()?.toString() ?: "U",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF059669)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(user.name, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                        Text(user.email, fontSize = 14.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(10.dp))

                        // Stats row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            StatBox("23", "Outfits", Color(0xFF10B981), Modifier.weight(1f))
                            StatBox("47", "Items", Color(0xFF2563EB), Modifier.weight(1f))
                            StatBox("89%", "Confidence", Color(0xFF7C3AED), Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Body Analysis Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(20.dp),
            elevation = 6.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Body Analysis", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    OutlinedButton(onClick = { onNavigate("scan") }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Rescan", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Rescan")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Body type box
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFD1FAE5), shape = RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(info.emoji, fontSize = 36.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(user.bodyType, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF065F46))
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(info.description, fontSize = 14.sp, color = Color(0xFF065F46))
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Height / Weight / Age row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatCard(height + " cm", "Height", Modifier.weight(1f))
                    StatCard(weight + " kg", "Weight", Modifier.weight(1f))
                    StatCard(age, "Age", Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = { isEditing = !isEditing },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Edit Information")
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // --- Body Measurements Section ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(20.dp),
            elevation = 6.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Body Measurements", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)

                Spacer(modifier = Modifier.height(8.dp))

                val bodyMeasurements = listOf(
                    "Shoulder Width",
                    "Hip Width",
                    "Torso Length",
                    "Leg Length",
                    "Arm Length",
                    "Estimated Skin Color"
                )

                bodyMeasurements.forEach { label ->
                    InfoRow(label, "N/A")
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // --- Body Ratios Section ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(20.dp),
            elevation = 6.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Body Ratios", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)

                Spacer(modifier = Modifier.height(8.dp))

                val bodyRatios = listOf(
                    "Shoulder-to-Hip Ratio",
                    "Torso-to-Leg Ratio",
                    "Arm-to-Leg Ratio",
                    "Shoulder-to-Height Ratio"
                )

                bodyRatios.forEach { label ->
                    InfoRow(label, "N/A")
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Style Tips
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(20.dp),
            elevation = 6.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Visibility, contentDescription = null, tint = Color(0xFF10B981))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Style Tips for You", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                }

                Spacer(modifier = Modifier.height(8.dp))

                info.tips.forEachIndexed { index, tip ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFD1FAE5), shape = RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF10B981)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text((index + 1).toString(), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(tip, color = Color(0xFF065F46))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFDBEAFE), shape = RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        "💡 These recommendations are based on your ${user.bodyType} body type. Remember, style is personal - wear what makes you feel confident!",
                        color = Color(0xFF1E3A8A)
                    )
                }
            }
        }
    }
}

// --- Helpers ---
@Composable
fun StatBox(value: String, label: String, color: Color, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.padding(4.dp)
    ) {
        Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = color)
        Text(label, fontSize = 12.sp, color = Color.Gray)
    }
}

@Composable
fun StatCard(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .background(Color(0xFFF9FAFB), shape = RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Text(value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Black)
        Text(label, fontSize = 12.sp, color = Color.Gray)
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color.Black)
        Text(value, fontSize = 14.sp, color = Color.Gray)
    }
}

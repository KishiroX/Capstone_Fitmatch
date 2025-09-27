// HomeScreen.kt
package com.example.capstone.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

// Data for preview / sample
private val sampleOutfits = listOf(
    Triple("Date Night", listOf("👔", "👖", "👞"), "Yesterday"),
    Triple("Work Meeting", listOf("👗", "👠"), "2 days ago"),
    Triple("Casual Weekend", listOf("👕", "👖", "👟"), "3 days ago")
)

@Composable
fun HomeScreen(navController: NavController, userName: String = "Style") {
    HomeScreenContent(onNavigate = { route -> navController.navigate(route) }, userName = userName)
}

/** Separated content so preview can call without a NavController. */
@Composable
fun HomeScreenContent(onNavigate: (String) -> Unit, userName: String = "Style") {
    val quickStats = listOf(
        "Outfits" to 0,
        "Items" to 0,
        "Used" to 0
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF9FAFB))
    ) {
        item {
            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFF10B981), Color(0xFF0D9488))
                        )
                    )
                    .padding(20.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Hello, ${userName.split(" ").firstOrNull() ?: "Style"}!",
                                fontSize = 24.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Ready to look amazing today?",
                                color = Color(0xFFD1FAE5),
                                fontSize = 14.sp
                            )
                        }

                        //  Profile button (navigates to Profile)
                        IconButton(
                            onClick = { onNavigate("profile") },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(Color.White.copy(alpha = 0.2f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Profile",
                                tint = Color.White
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    Row(
                        horizontalArrangement = Arrangement.SpaceAround,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        quickStats.forEach { (label, value) ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "$value",
                                    fontSize = 22.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = label,
                                    color = Color(0xFFD1FAE5),
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        //  Main Actions grid
        item {
            Column(Modifier.padding(16.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ActionCard(
                        title = "Style Assistant",
                        subtitle = "Get recommendations",
                        color1 = Color(0xFF8B5CF6),
                        color2 = Color(0xFFEC4899),
                        modifier = Modifier.weight(1f),
                        icon = { Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White) }
                    ) { onNavigate("assistant") }

                    ActionCard(
                        title = "Wardrobe",
                        subtitle = "Manage clothes",
                        color1 = Color(0xFF3B82F6),
                        color2 = Color(0xFF06B6D4),
                        modifier = Modifier.weight(1f),
                        icon = { Icon(Icons.Default.Checkroom, contentDescription = null, tint = Color.White) }
                    ) { onNavigate("wardrobe") }
                }

                Spacer(Modifier.height(12.dp))

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ActionCard(
                        title = "History",
                        subtitle = "Past outfits",
                        color1 = Color(0xFFF97316),
                        color2 = Color(0xFFEF4444),
                        modifier = Modifier.weight(1f),
                        icon = { Icon(Icons.Default.History, contentDescription = null, tint = Color.White) }
                    ) { onNavigate("history") }

                    ActionCard(
                        title = "Build a Fit",
                        subtitle = "Create outfits",
                        color1 = Color(0xFF22C55E),
                        color2 = Color(0xFF059669),
                        modifier = Modifier.weight(1f),
                        icon = { Icon(Icons.Default.Add, contentDescription = null, tint = Color.White) }
                    ) { onNavigate("buildFit") }
                }
            }
        }

        //  Today's Suggestion
        item {
            Card(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            Text("Today's Suggestion", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text("Perfect for today's weather", color = Color.Gray)
                        }
                        Box(modifier = Modifier.size(28.dp)) {} // placeholder
                    }

                    Spacer(Modifier.height(12.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🌤️", fontSize = 32.sp)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Perfect for today's weather", fontWeight = FontWeight.Medium)
                            Text("22°C, Partly cloudy", color = Color.Gray)
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                        Text("👕", fontSize = 28.sp)
                        Spacer(Modifier.width(8.dp))
                        Text("👖", fontSize = 28.sp)
                        Spacer(Modifier.width(8.dp))
                        Text("👟", fontSize = 28.sp)
                    }

                    Spacer(Modifier.height(16.dp))

                    Button(
                        onClick = { onNavigate("assistant") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                    ) {
                        Text("Get Full Recommendation", color = Color.White)
                    }
                }
            }
        }

        //  Recent Outfits
        item {
            Card(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Recent Outfits", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        TextButton(onClick = { onNavigate("history") }) {
                            Text("View All", color = Color(0xFF10B981))
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    sampleOutfits.forEach { outfit ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFF3F4F6))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Row {
                                    outfit.second.forEach {
                                        Text(it, fontSize = 20.sp, modifier = Modifier.padding(end = 4.dp))
                                    }
                                }
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(outfit.first, fontWeight = FontWeight.Medium)
                                    Text(outfit.third, fontSize = 12.sp, color = Color.Gray)
                                }
                            }
                            Icon(Icons.Default.FavoriteBorder, contentDescription = "like", tint = Color.Gray)
                        }
                    }
                }
            }
        }

        // Style Journey
        item {
            Card(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Your Style Journey", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("Confidence Level", fontWeight = FontWeight.Medium)
                            Text("Keep building your style!", color = Color.Gray, fontSize = 12.sp)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("85%", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                            Text("+5% this week", color = Color(0xFF059669), fontSize = 12.sp)
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFE5E7EB))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(0.85f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(Color(0xFF10B981), Color(0xFF0D9488))
                                    )
                                )
                        )
                    }
                }
            }
        }
    }
}

/** ActionCard accepts modifier so caller can pass weight(1f) from Row scope. */
@Composable
fun ActionCard(
    title: String,
    subtitle: String,
    color1: Color,
    color2: Color,
    modifier: Modifier = Modifier,
    icon: @Composable (() -> Unit)? = null,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(140.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(color1, color2)))
            .clickable { onClick() }
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            if (icon != null) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color.White.copy(alpha = 0.2f), shape = RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    icon()
                }
                Spacer(Modifier.height(12.dp))
            }
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(Modifier.height(4.dp))
            Text(subtitle, color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
        }
    }
}

/** Preview */
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun HomeScreenPreview() {
    HomeScreenContent(onNavigate = {}, userName = "John Doe")
}

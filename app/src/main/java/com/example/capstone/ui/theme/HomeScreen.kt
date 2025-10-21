package com.example.capstone.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

private val sampleOutfits = listOf(
    Triple("Date Night", listOf("👔", "👖", "👞"), "Yesterday"),
    Triple("Work Meeting", listOf("👗", "👠"), "2 days ago"),
    Triple("Casual Weekend", listOf("👕", "👖", "👟"), "3 days ago")
)

@Composable
fun HomeScreen(navController: NavController) {
    val userId = FirebaseAuth.getInstance().currentUser?.uid
    var firstName by remember { mutableStateOf("Style") }
    var userLevel by remember { mutableStateOf(1) }
    var userXP by remember { mutableStateOf(0) }
    var totalOutfits by remember { mutableStateOf(0) }
    var totalItems by remember { mutableStateOf(0) }
    var streak by remember { mutableStateOf(0) }
    var maxStorage by remember { mutableStateOf(20) }
    var currentStorage by remember { mutableStateOf(0) }

    // Real-time listener for user data updates
    LaunchedEffect(userId) {
        if (userId != null) {
            val db = FirebaseFirestore.getInstance()

            // Real-time listener for user document
            db.collection("users").document(userId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        android.util.Log.e("HomeScreen", "Error listening to user data", error)
                        return@addSnapshotListener
                    }

                    if (snapshot != null && snapshot.exists()) {
                        firstName = snapshot.getString("firstName")
                            ?: snapshot.getString("name")?.split(" ")?.firstOrNull()
                                    ?: "Style"
                        userLevel = (snapshot.getLong("level") ?: 1L).toInt()
                        userXP = (snapshot.getLong("currentXP") ?: 0L).toInt()
                        streak = (snapshot.getLong("streak") ?: 0L).toInt()
                        maxStorage = (snapshot.getLong("maxStorage") ?: 20L).toInt()
                        currentStorage = (snapshot.getLong("currentStorage") ?: 0L).toInt()
                        totalOutfits = (snapshot.getLong("totalOutfits") ?: 0L).toInt()
                    }
                }

            // Count wardrobe items
            try {
                val wardrobeSnapshot = db.collection("users").document(userId)
                    .collection("wardrobe").get().await()
                totalItems = wardrobeSnapshot.size()
            } catch (e: Exception) {
                android.util.Log.e("HomeScreen", "Error loading wardrobe", e)
            }
        }
    }

    HomeScreenContent(
        onNavigate = { route -> navController.navigate(route) },
        userName = firstName,
        userLevel = userLevel,
        userXP = userXP,
        totalOutfits = totalOutfits,
        totalItems = totalItems,
        streak = streak,
        maxStorage = maxStorage,
        currentStorage = currentStorage
    )
}

@Composable
fun HomeScreenContent(
    onNavigate: (String) -> Unit,
    userName: String = "Style",
    userLevel: Int = 1,
    userXP: Int = 0,
    totalOutfits: Int = 0,
    totalItems: Int = 0,
    streak: Int = 0,
    maxStorage: Int = 20,
    currentStorage: Int = 0
) {
    // Calculate XP progress using MissionManager formula
    val xpForNextLevel = when {
        userLevel <= 10 -> 100
        userLevel <= 20 -> 200
        else -> 300 + ((userLevel - 20) * 100)
    }
    val xpProgress = if (xpForNextLevel > 0) (userXP.toFloat() / xpForNextLevel).coerceIn(0f, 1f) else 0f

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF9FAFB))
    ) {
        // Header with Level Badge and XP Progress
        item {
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Hello, $userName!",
                                    fontSize = 24.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                // Level Badge
                                Surface(
                                    color = Color(0xFFFBBF24),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        "Lv $userLevel",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Text(
                                text = "Ready to look amazing today?",
                                color = Color(0xFFD1FAE5),
                                fontSize = 14.sp
                            )
                        }

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

                    Spacer(modifier = Modifier.height(12.dp))

                    // XP Progress Bar
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Level Progress",
                                color = Color.White,
                                fontSize = 12.sp
                            )
                            Text(
                                text = "$userXP / $xpForNextLevel XP",
                                color = Color(0xFFD1FAE5),
                                fontSize = 12.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = xpProgress,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = Color(0xFFFBBF24),
                            trackColor = Color.White.copy(alpha = 0.3f)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Stats Row with Streak
                    Row(
                        horizontalArrangement = Arrangement.SpaceAround,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$totalOutfits",
                                fontSize = 22.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Outfits",
                                color = Color(0xFFD1FAE5),
                                fontSize = 12.sp
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$totalItems",
                                fontSize = 22.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Items",
                                color = Color(0xFFD1FAE5),
                                fontSize = 12.sp
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "$streak",
                                    fontSize = 22.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = " 🔥",
                                    fontSize = 18.sp
                                )
                            }
                            Text(
                                text = "Streak",
                                color = Color(0xFFD1FAE5),
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        // Action Cards
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
                    ) { onNavigate("tryon") }
                }
            }
        }



        // Recent Outfits Card
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

        // Style Journey Card
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
                            Text("Level $userLevel", fontWeight = FontWeight.Medium)
                            Text("Keep building your style!", color = Color.Gray, fontSize = 12.sp)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                "${(xpProgress * 100).toInt()}%",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF10B981)
                            )
                            Text("to Level ${userLevel + 1}", color = Color(0xFF059669), fontSize = 12.sp)
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    LinearProgressIndicator(
                        progress = xpProgress,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        color = Color(0xFF10B981),
                        trackColor = Color(0xFFE5E7EB)
                    )
                }
            }
        }
    }
}

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
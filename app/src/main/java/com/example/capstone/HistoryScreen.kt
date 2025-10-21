package com.example.capstone.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp

data class OutfitHistory(
    val id: String = "",
    val name: String = "",
    val occasion: String = "",
    val theme: String = "",
    val weather: String = "",
    val timestamp: Timestamp? = null,
    val items: List<String> = emptyList(),
    val fromWardrobe: List<String> = emptyList(),
    val confidence: Int = 0,
    val worn: Boolean = false,
    val isFavorite: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(onNavigate: (String) -> Unit) {
    val db = FirebaseFirestore.getInstance()
    val userId = FirebaseAuth.getInstance().currentUser?.uid

    var outfitHistory by remember { mutableStateOf<List<OutfitHistory>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Load outfit history from Firebase
    LaunchedEffect(userId) {
        if (userId != null) {
            db.collection("users").document(userId)
                .collection("outfitHistory")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        android.util.Log.e("HistoryScreen", "Error loading history", error)
                        isLoading = false
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        outfitHistory = snapshot.documents.mapNotNull { doc ->
                            try {
                                OutfitHistory(
                                    id = doc.id,
                                    name = doc.getString("name") ?: "Untitled Outfit",
                                    occasion = doc.getString("occasion") ?: "N/A",
                                    theme = doc.getString("theme") ?: "Casual",
                                    weather = doc.getString("weather") ?: "N/A",
                                    timestamp = doc.getTimestamp("timestamp"),
                                    items = (doc.get("items") as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                                    fromWardrobe = (doc.get("fromWardrobe") as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                                    confidence = (doc.getLong("confidence") ?: 0).toInt(),
                                    worn = doc.getBoolean("worn") ?: false,
                                    isFavorite = doc.getBoolean("isFavorite") ?: false
                                )
                            } catch (e: Exception) {
                                android.util.Log.e("HistoryScreen", "Error parsing outfit", e)
                                null
                            }
                        }
                        isLoading = false
                    }
                }
        } else {
            isLoading = false
        }
    }

    val stats = remember(outfitHistory) {
        mapOf(
            "totalOutfits" to outfitHistory.size,
            "favorites" to outfitHistory.count { it.isFavorite },
            "worn" to outfitHistory.count { it.worn }
        )
    }

    fun toggleFavorite(outfitId: String) {
        if (userId != null) {
            val outfit = outfitHistory.find { it.id == outfitId }
            if (outfit != null) {
                db.collection("users").document(userId)
                    .collection("outfitHistory").document(outfitId)
                    .update("isFavorite", !outfit.isFavorite)
            }
        }
    }

    fun getConfidenceColor(confidence: Int): Color {
        return when {
            confidence >= 90 -> Color(0xFF16A34A)
            confidence >= 80 -> Color(0xFFCA8A04)
            else -> Color(0xFFDC2626)
        }
    }

    fun formatDate(timestamp: Timestamp?): String {
        if (timestamp == null) return "N/A"
        val now = System.currentTimeMillis()
        val then = timestamp.toDate().time
        val diff = now - then

        return when {
            diff < 86400000 -> "Today"
            diff < 172800000 -> "Yesterday"
            diff < 604800000 -> "${diff / 86400000} days ago"
            diff < 2592000000 -> "${diff / 604800000} weeks ago"
            else -> "${diff / 2592000000} months ago"
        }
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color(0xFF10B981))
        }
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF9FAFB))
        ) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                // Header
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(Color(0xFFF97316), Color(0xFFEF4444))
                                )
                            )
                            .padding(24.dp)
                            .padding(bottom = 8.dp)
                    ) {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(bottom = 24.dp)
                            ) {
                                IconButton(
                                    onClick = { onNavigate("home") },
                                    modifier = Modifier.padding(end = 16.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowBack,
                                        contentDescription = "Back",
                                        tint = Color.White
                                    )
                                }
                                Text(
                                    text = "Outfit History",
                                    fontSize = 24.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.History,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Your style journey and past recommendations",
                                    color = Color(0xFFFED7AA),
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }

                // Stats Card
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .offset(y = (-16).dp),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            StatsItem(
                                value = stats["totalOutfits"].toString(),
                                label = "Total Outfits",
                                color = Color(0xFF111827)
                            )
                            StatsItem(
                                value = stats["favorites"].toString(),
                                label = "Favorites",
                                color = Color(0xFFEF4444)
                            )
                            StatsItem(
                                value = stats["worn"].toString(),
                                label = "Worn",
                                color = Color(0xFF10B981)
                            )
                        }
                    }
                }

                // Section Header
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .padding(top = 24.dp, bottom = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Your Outfits",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF111827)
                        )
                        Text(
                            text = "${outfitHistory.size} outfits",
                            color = Color(0xFFF97316),
                            fontSize = 14.sp
                        )
                    }
                }

                // Empty State
                if (outfitHistory.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 48.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Checkroom,
                                contentDescription = null,
                                tint = Color(0xFF9CA3AF),
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No Outfits Yet",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF111827)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Create your first outfit to see it here",
                                fontSize = 14.sp,
                                color = Color(0xFF6B7280)
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = { onNavigate("tryon") },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF10B981)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Create Outfit")
                            }
                        }
                    }
                }

                // Outfit Cards
                items(outfitHistory) { outfit ->
                    OutfitCard(
                        outfit = outfit,
                        onToggleFavorite = { toggleFavorite(outfit.id) },
                        onNavigate = onNavigate,
                        getConfidenceColor = ::getConfidenceColor,
                        formatDate = ::formatDate
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(96.dp))
                }
            }
        }
    }
}

@Composable
fun StatsItem(value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color(0xFF6B7280)
        )
    }
}

@Composable
fun OutfitCard(
    outfit: OutfitHistory,
    onToggleFavorite: () -> Unit,
    onNavigate: (String) -> Unit,
    getConfidenceColor: (Int) -> Color,
    formatDate: (Timestamp?) -> String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 16.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        outfit.items.take(3).forEach { item ->
                            Text(text = item, fontSize = 24.sp)
                        }
                    }
                    Column {
                        Text(
                            text = outfit.name,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF111827),
                            fontSize = 16.sp
                        )
                        Text(
                            text = formatDate(outfit.timestamp),
                            color = Color(0xFF6B7280),
                            fontSize = 14.sp
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(
                                if (outfit.isFavorite) Color(0xFFFEE2E2) else Color(0xFFF3F4F6)
                            )
                    ) {
                        Icon(
                            imageVector = if (outfit.isFavorite) Icons.Filled.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (outfit.isFavorite) Color(0xFFEF4444) else Color(0xFF9CA3AF),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Info Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF9FAFB))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = Color(0xFF6B7280),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Occasion",
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF374151),
                                fontSize = 14.sp
                            )
                        }
                        Text(
                            text = outfit.occasion,
                            color = Color(0xFF6B7280),
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Theme: ${outfit.theme}",
                            color = Color(0xFF9CA3AF),
                            fontSize = 12.sp
                        )
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF9FAFB))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = Color(0xFF6B7280),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Weather",
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF374151),
                                fontSize = 14.sp
                            )
                        }
                        Text(
                            text = outfit.weather,
                            color = Color(0xFF6B7280),
                            fontSize = 14.sp
                        )
                    }
                }
            }

            if (outfit.fromWardrobe.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))

                Column {
                    Text(
                        text = "From Your Wardrobe:",
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF111827),
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        outfit.fromWardrobe.take(3).forEach { item ->
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0xFFD1FAE5)
                            ) {
                                Text(
                                    text = item,
                                    color = Color(0xFF047857),
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Confidence and Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Confidence Level:",
                        color = Color(0xFF374151),
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${outfit.confidence}%",
                        fontWeight = FontWeight.Bold,
                        color = getConfidenceColor(outfit.confidence),
                        fontSize = 14.sp
                    )
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (outfit.worn) Color(0xFFD1FAE5) else Color(0xFFF3F4F6)
                ) {
                    Text(
                        text = if (outfit.worn) "✓ Worn" else "Not worn",
                        color = if (outfit.worn) Color(0xFF047857) else Color(0xFF6B7280),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { onNavigate("buildFit") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF10B981)
                    )
                ) {
                    Text(
                        text = "Wear Again",
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                OutlinedButton(
                    onClick = { /* Share functionality */ },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF6B7280)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
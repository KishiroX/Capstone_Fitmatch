package com.example.capstone.ui.screen

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

data class OutfitHistory(
    val id: Int,
    val name: String,
    val occasion: String,
    val theme: String,
    val weather: String,
    val date: String,
    val items: List<String>,
    val fromWardrobe: List<String>,
    val confidence: Int,
    val worn: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onNavigate: (String) -> Unit
) {
    var favorites by remember { mutableStateOf(setOf(1, 3)) }

    val outfitHistory = remember {
        listOf(
            OutfitHistory(
                id = 1,
                name = "Date Night Look",
                occasion = "Date night",
                theme = "Chic",
                weather = "Sunny, 24°C",
                date = "Today",
                items = listOf("👔", "👖", "👞"),
                fromWardrobe = listOf("Blue Blazer", "Dark Jeans", "Brown Loafers"),
                confidence = 95,
                worn = true
            ),
            OutfitHistory(
                id = 2,
                name = "Work Meeting",
                occasion = "Work meeting",
                theme = "Professional",
                weather = "Cloudy, 18°C",
                date = "Yesterday",
                items = listOf("👗", "👠", "👜"),
                fromWardrobe = listOf("Black Dress", "Heels", "Leather Bag"),
                confidence = 88,
                worn = true
            ),
            OutfitHistory(
                id = 3,
                name = "Casual Weekend",
                occasion = "Casual outing",
                theme = "Casual",
                weather = "Sunny, 26°C",
                date = "2 days ago",
                items = listOf("👕", "👖", "👟"),
                fromWardrobe = listOf("White T-shirt", "Blue Jeans", "Sneakers"),
                confidence = 92,
                worn = false
            ),
            OutfitHistory(
                id = 4,
                name = "Party Ready",
                occasion = "Party",
                theme = "Trendy",
                weather = "Clear, 22°C",
                date = "3 days ago",
                items = listOf("🧥", "👖", "👠"),
                fromWardrobe = listOf("Leather Jacket", "Black Pants", "Heels"),
                confidence = 90,
                worn = true
            ),
            OutfitHistory(
                id = 5,
                name = "Beach Day",
                occasion = "Beach/Pool",
                theme = "Casual",
                weather = "Hot, 32°C",
                date = "1 week ago",
                items = listOf("👙", "🩳", "👡"),
                fromWardrobe = listOf("Swimsuit", "Shorts", "Sandals"),
                confidence = 85,
                worn = true
            )
        )
    }

    val stats = remember(outfitHistory, favorites) {
        mapOf(
            "totalOutfits" to outfitHistory.size,
            "favorites" to favorites.size,
            "worn" to outfitHistory.count { it.worn },
        )
    }

    fun toggleFavorite(outfitId: Int) {
        favorites = if (favorites.contains(outfitId)) {
            favorites - outfitId
        } else {
            favorites + outfitId
        }
    }

    fun getConfidenceColor(confidence: Int): Color {
        return when {
            confidence >= 90 -> Color(0xFF16A34A)
            confidence >= 80 -> Color(0xFFCA8A04)
            else -> Color(0xFFDC2626)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF9FAFB))
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFFF97316),
                                    Color(0xFFEF4444)
                                )
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

            // Stats
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

            // Outfit Cards
            items(outfitHistory) { outfit ->
                OutfitCard(
                    outfit = outfit,
                    isFavorite = favorites.contains(outfit.id),
                    onToggleFavorite = { toggleFavorite(outfit.id) },
                    onNavigate = onNavigate,
                    getConfidenceColor = ::getConfidenceColor
                )
            }

            item {
                Spacer(modifier = Modifier.height(96.dp))
            }
        }
    }
}

@Composable
fun StatsItem(value: String, label: String, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
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
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onNavigate: (String) -> Unit,
    getConfidenceColor: (Int) -> Color
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
        Column(
            modifier = Modifier.padding(24.dp)
        ) {
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
                        outfit.items.forEach { item ->
                            Text(
                                text = item,
                                fontSize = 24.sp
                            )
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
                            text = outfit.date,
                            color = Color(0xFF6B7280),
                            fontSize = 14.sp
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(
                                if (isFavorite) Color(0xFFFEE2E2) else Color(0xFFF3F4F6)
                            )
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (isFavorite) Color(0xFFEF4444) else Color(0xFF9CA3AF),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(
                        onClick = { },
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF3F4F6))
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More",
                            tint = Color(0xFF9CA3AF),
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
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
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
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
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

            Spacer(modifier = Modifier.height(16.dp))

            // Wardrobe Items
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
                    outfit.fromWardrobe.forEach { item ->
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

            Spacer(modifier = Modifier.height(16.dp))

            // Confidence and Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
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
                    onClick = { },
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

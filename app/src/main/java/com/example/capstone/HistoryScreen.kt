package com.example.capstone.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class OutfitHistory(
    val id: String = "",
    val name: String = "",
    val timestamp: Timestamp? = null,
    val items: List<String> = emptyList(),
    val fromWardrobe: List<String> = emptyList(),
    val fromWardrobeImages: List<String> = emptyList(), // ⭐ NEW: Store clothing item image URLs
    val worn: Boolean = false,
    val isFavorite: Boolean = false,
    val tryOnImageUrl: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(onNavigate: (String) -> Unit) {
    val db = FirebaseFirestore.getInstance()
    val userId = FirebaseAuth.getInstance().currentUser?.uid
    val scope = rememberCoroutineScope()

    var outfitHistory by remember { mutableStateOf<List<OutfitHistory>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val snackbarHostState = remember { SnackbarHostState() }

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
                                    timestamp = doc.getTimestamp("timestamp"),
                                    items = (doc.get("items") as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                                    fromWardrobe = (doc.get("fromWardrobe") as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                                    fromWardrobeImages = (doc.get("fromWardrobeImages") as? List<*>)?.mapNotNull { it as? String } ?: emptyList(), // ⭐ Load images
                                    worn = doc.getBoolean("worn") ?: false,
                                    isFavorite = doc.getBoolean("isFavorite") ?: false,
                                    tryOnImageUrl = doc.getString("tryOnImageUrl")
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

    fun deleteOutfit(outfitId: String) {
        if (userId != null) {
            scope.launch {
                try {
                    db.collection("users").document(userId)
                        .collection("outfitHistory")
                        .document(outfitId)
                        .delete()
                        .await()
                    snackbarHostState.showSnackbar("Outfit deleted")
                } catch (e: Exception) {
                    android.util.Log.e("HistoryScreen", "Error deleting outfit", e)
                    snackbarHostState.showSnackbar("Failed to delete outfit")
                }
            }
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

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFF10B981))
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFF9FAFB))
                    .padding(paddingValues)
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
                                        text = "Your saved outfits and try-on results",
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
                            onDelete = { deleteOutfit(outfit.id) },
                            onNavigate = onNavigate,
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
    onDelete: () -> Unit,
    onNavigate: (String) -> Unit,
    formatDate: (Timestamp?) -> String
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Delete Confirmation Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Outfit?") },
            text = { Text("Are you sure you want to delete this outfit? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFEF4444)
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 16.dp),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(6.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column {
            // Try-On Image Preview (if available)
            outfit.tryOnImageUrl?.let { imageUrl ->
                if (imageUrl.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                            .background(Color(0xFFF9FAFB))
                    ) {
                        Image(
                            painter = rememberAsyncImagePainter(imageUrl),
                            contentDescription = "Try-On Result",
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 300.dp, max = 500.dp),
                            contentScale = ContentScale.Fit
                        )

                        // AI Badge with glow effect
                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(16.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF10B981),
                            shadowElevation = 8.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "AI Virtual Try-On",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Content Section
            Column(modifier = Modifier.padding(20.dp)) {
                // Header with outfit name and date
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = outfit.name,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF111827),
                            fontSize = 18.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = Color(0xFF9CA3AF),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = formatDate(outfit.timestamp),
                                color = Color(0xFF6B7280),
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                // Outfit items - remove emojis section since we have images below
                // This section is now redundant with the wardrobe images

                // Wardrobe items with images
                if (outfit.fromWardrobe.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Inventory,
                                contentDescription = null,
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "From Wardrobe",
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF047857),
                                fontSize = 13.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))

                        // Display clothing item images
                        if (outfit.fromWardrobeImages.isNotEmpty()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                outfit.fromWardrobeImages.take(4).forEachIndexed { index, imageUrl ->
                                    Box(
                                        modifier = Modifier
                                            .size(70.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color(0xFFF3F4F6))
                                    ) {
                                        Image(
                                            painter = rememberAsyncImagePainter(imageUrl),
                                            contentDescription = outfit.fromWardrobe.getOrNull(index),
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )

                                        // Item name overlay
                                        Surface(
                                            modifier = Modifier
                                                .align(Alignment.BottomCenter)
                                                .fillMaxWidth(),
                                            color = Color.Black.copy(alpha = 0.6f)
                                        ) {
                                            Text(
                                                text = outfit.fromWardrobe.getOrNull(index) ?: "",
                                                color = Color.White,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Medium,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 3.dp),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }

                                // Show "+X more" if there are more items
                                if (outfit.fromWardrobe.size > 4) {
                                    Box(
                                        modifier = Modifier
                                            .size(70.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color(0xFFF3F4F6)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "+${outfit.fromWardrobe.size - 4}",
                                            color = Color(0xFF6B7280),
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        } else {
                            // Fallback to text chips if no images
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                outfit.fromWardrobe.take(3).forEach { item ->
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFFD1FAE5)
                                    ) {
                                        Text(
                                            text = item,
                                            color = Color(0xFF047857),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                                if (outfit.fromWardrobe.size > 3) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFFF3F4F6)
                                    ) {
                                        Text(
                                            text = "+${outfit.fromWardrobe.size - 3} more",
                                            color = Color(0xFF6B7280),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action buttons and status row
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Action buttons row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Favorite Button
                        OutlinedButton(
                            onClick = onToggleFavorite,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (outfit.isFavorite) Color(0xFFFEE2E2) else Color.White,
                                contentColor = if (outfit.isFavorite) Color(0xFFEF4444) else Color(0xFF6B7280)
                            ),
                            border = BorderStroke(
                                1.5.dp,
                                if (outfit.isFavorite) Color(0xFFEF4444) else Color(0xFFE5E7EB)
                            ),
                            contentPadding = PaddingValues(vertical = 14.dp)
                        ) {
                            Icon(
                                imageVector = if (outfit.isFavorite) Icons.Filled.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Favorite",
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (outfit.isFavorite) "Favorited" else "Favorite",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        // Delete Button
                        OutlinedButton(
                            onClick = { showDeleteDialog = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = Color.White,
                                contentColor = Color(0xFFEF4444)
                            ),
                            border = BorderStroke(1.5.dp, Color(0xFFEF4444)),
                            contentPadding = PaddingValues(vertical = 14.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Delete",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Try Again Button - Full Width
                    Button(
                        onClick = { onNavigate("tryon") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF10B981)
                        ),
                        contentPadding = PaddingValues(vertical = 16.dp),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 4.dp,
                            pressedElevation = 8.dp
                        )
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Try This Outfit Again",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Show worn badge if worn
                    if (outfit.worn) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFD1FAE5),
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF047857),
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Worn on ${formatDate(outfit.timestamp)}",
                                    color = Color(0xFF047857),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
package com.example.capstone.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class ClothingItem(
    val id: Int,
    val name: String,
    val icon: String,
    val color: String,
    val category: String
)

data class SavedOutfit(
    val id: Long,
    val name: String,
    val items: List<String>
)

data class SelectedOutfit(
    val top: ClothingItem? = null,
    val bottom: ClothingItem? = null,
    val shoes: ClothingItem? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuildAFitScreen(
    onNavigate: (String) -> Unit
) {
    var selectedOutfit by remember { mutableStateOf(SelectedOutfit()) }
    var savedOutfits by remember {
        mutableStateOf(
            listOf(
                SavedOutfit(1, "Morning Run", listOf("👕", "🩳", "👟")),
                SavedOutfit(2, "Yoga Session", listOf("👙", "👖", "🧘‍♀️")),
                SavedOutfit(3, "Gym Workout", listOf("🧥", "👖", "👟"))
            )
        )
    }

    val clothingCategories = remember {
        mapOf(
            "tops" to listOf(
                ClothingItem(1, "Athletic Tank", "👕", "Blue", "tops"),
                ClothingItem(2, "Sport Bra", "👙", "Pink", "tops"),
                ClothingItem(3, "Workout Hoodie", "🧥", "Green", "tops"),
                ClothingItem(4, "Compression Top", "👔", "Black", "tops")
            ),
            "bottoms" to listOf(
                ClothingItem(1, "Running Shorts", "🩳", "Black", "bottoms"),
                ClothingItem(2, "Leggings", "👖", "Grey", "bottoms"),
                ClothingItem(3, "Yoga Pants", "👖", "Navy", "bottoms"),
                ClothingItem(4, "Track Pants", "👖", "Blue", "bottoms")
            ),
            "shoes" to listOf(
                ClothingItem(1, "Running Shoes", "👟", "White", "shoes"),
                ClothingItem(2, "Cross Trainers", "👟", "Black", "shoes"),
                ClothingItem(3, "Yoga Shoes", "🥿", "Pink", "shoes")
            )
        )
    }

    val allItems = remember { clothingCategories.values.flatten() }
    val tabCategories = listOf("all", "tops", "bottoms", "shoes")

    var activeCategory by remember { mutableStateOf("all") }

    fun selectItem(category: String, item: ClothingItem) {
        val effectiveCategory = if (category == "all") item.category else category
        selectedOutfit = when (effectiveCategory) {
            "tops" -> selectedOutfit.copy(top = item)
            "bottoms" -> selectedOutfit.copy(bottom = item)
            "shoes" -> selectedOutfit.copy(shoes = item)
            else -> selectedOutfit
        }
    }

    fun generateRandomOutfit() {
        val randomTop = clothingCategories["tops"]?.random()
        val randomBottom = clothingCategories["bottoms"]?.random()
        val randomShoes = clothingCategories["shoes"]?.random()

        selectedOutfit = SelectedOutfit(
            top = randomTop,
            bottom = randomBottom,
            shoes = randomShoes
        )
    }

    fun saveOutfit() {
        val items = listOfNotNull(
            selectedOutfit.top?.icon,
            selectedOutfit.bottom?.icon,
            selectedOutfit.shoes?.icon
        )
        if (items.isNotEmpty()) {
            val outfitName = when {
                selectedOutfit.top != null && selectedOutfit.bottom != null && selectedOutfit.shoes != null -> "Full Outfit ${savedOutfits.size + 1}"
                selectedOutfit.top != null -> "Top-Focused ${savedOutfits.size + 1}"
                selectedOutfit.bottom != null -> "Bottom-Focused ${savedOutfits.size + 1}"
                selectedOutfit.shoes != null -> "Shoes-Focused ${savedOutfits.size + 1}"
                else -> "Outfit ${savedOutfits.size + 1}"
            }
            val newOutfit = SavedOutfit(
                id = System.currentTimeMillis(),
                name = outfitName,
                items = items
            )
            savedOutfits = savedOutfits + newOutfit
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
                                    Color(0xFF10B981),
                                    Color(0xFF14B8A6)
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
                                text = "Build a Fit",
                                fontSize = 24.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            text = "Create the perfect workout outfit for your session",
                            color = Color(0xFFD1FAE5),
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Outfit Preview
            item {
                OutfitPreview(
                    selectedOutfit = selectedOutfit,
                    onRandomClick = { generateRandomOutfit() },
                    onSaveClick = { saveOutfit() },
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
                )
            }

            // Category Tabs
            item {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(tabCategories) { category ->
                        Button(
                            onClick = { activeCategory = category },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (activeCategory == category)
                                    Color(0xFF10B981)
                                else
                                    Color.White,
                                contentColor = if (activeCategory == category)
                                    Color.White
                                else
                                    Color(0xFF6B7280)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Text(
                                text = category.replaceFirstChar { it.uppercase() },
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            // Item Grid
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                ) {
                    val itemsToDisplay = if (activeCategory == "all") allItems else clothingCategories[activeCategory] ?: emptyList()
                    itemsToDisplay.chunked(2).forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            rowItems.forEach { item ->
                                ItemCard(
                                    item = item,
                                    isSelected = when (activeCategory) {
                                        "all" -> when (item.category) {
                                            "tops" -> selectedOutfit.top?.id == item.id
                                            "bottoms" -> selectedOutfit.bottom?.id == item.id
                                            "shoes" -> selectedOutfit.shoes?.id == item.id
                                            else -> false
                                        }
                                        "tops" -> selectedOutfit.top?.id == item.id
                                        "bottoms" -> selectedOutfit.bottom?.id == item.id
                                        "shoes" -> selectedOutfit.shoes?.id == item.id
                                        else -> false
                                    },
                                    onClick = { selectItem(activeCategory, item) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (rowItems.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }

            // Saved Outfits
            item {
                SavedOutfitsSection(
                    savedOutfits = savedOutfits,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
                )
            }

            item {
                Spacer(modifier = Modifier.height(96.dp))
            }
        }
    }
}

@Composable
fun OutfitPreview(
    selectedOutfit: SelectedOutfit,
    onRandomClick: () -> Unit,
    onSaveClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(24.dp)
        ) {
            Text(
                text = "Your Outfit",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF111827),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFFF9FAFB),
                                Color(0xFFF3F4F6)
                            )
                        )
                    )
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    selectedOutfit.top?.let {
                        Text(text = it.icon, fontSize = 48.sp)
                    }
                    selectedOutfit.bottom?.let {
                        Text(text = it.icon, fontSize = 48.sp)
                    }
                    selectedOutfit.shoes?.let {
                        Text(text = it.icon, fontSize = 48.sp)
                    }

                    if (selectedOutfit.top == null &&
                        selectedOutfit.bottom == null &&
                        selectedOutfit.shoes == null
                    ) {
                        Text(
                            text = "Select items to build your outfit",
                            color = Color(0xFF9CA3AF),
                            fontSize = 14.sp,
                            modifier = Modifier.padding(top = 16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onRandomClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF10B981)
                    ),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = Brush.linearGradient(listOf(Color(0xFFD1FAE5), Color(0xFFD1FAE5)))
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Random")
                }

                Button(
                    onClick = onSaveClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF10B981)
                    ),
                    enabled = selectedOutfit.top != null || selectedOutfit.bottom != null || selectedOutfit.shoes != null
                ) {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save")
                }

                OutlinedButton(
                    onClick = {
                        println("Share feature: Coming soon! Outfit: ${selectedOutfit.top?.name ?: "No top"}, ${selectedOutfit.bottom?.name ?: "No bottom"}, ${selectedOutfit.shoes?.name ?: "No shoes"}")
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF6B7280)
                    ),
                    enabled = selectedOutfit.top != null || selectedOutfit.bottom != null || selectedOutfit.shoes != null
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Share")
                }
            }
        }
    }
}

@Composable
fun ItemCard(
    item: ClothingItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFFD1FAE5) else Color.White
        ),
        border = if (isSelected)
            ButtonDefaults.outlinedButtonBorder.copy(
                width = 2.dp,
                brush = Brush.linearGradient(listOf(Color(0xFF10B981), Color(0xFF10B981)))
            )
        else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = item.icon,
                fontSize = 40.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = item.name,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF111827),
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = item.color,
                color = Color(0xFF6B7280),
                fontSize = 12.sp
            )

            if (isSelected) {
                Box(
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF10B981)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SavedOutfitsSection(
    savedOutfits: List<SavedOutfit>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(24.dp)
        ) {
            Text(
                text = "Saved Outfits",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF111827),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                savedOutfits.forEach { outfit ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF9FAFB)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    outfit.items.take(3).forEach { icon ->
                                        Text(text = icon, fontSize = 20.sp)
                                    }
                                }
                                Text(
                                    text = outfit.name,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF111827),
                                    fontSize = 14.sp
                                )
                            }

                            Button(
                                onClick = { },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.Transparent,
                                    contentColor = Color(0xFF10B981)
                                ),
                                shape = RoundedCornerShape(8.dp),
                                border = ButtonDefaults.outlinedButtonBorder.copy(
                                    brush = Brush.linearGradient(
                                        listOf(Color(0xFFD1FAE5), Color(0xFFD1FAE5))
                                    )
                                ),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Text("Use", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

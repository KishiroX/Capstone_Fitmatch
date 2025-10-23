package com.example.capstone.ui.screen

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import java.io.File

/**
 * Data class for AI detected clothing information
 */
data class DetectedClothingInfo(
    val category: String = "Tops",
    val colors: List<String> = emptyList(),
    val pattern: String = "Solid",
    val clothingType: String = "Clothing Item"
)

/**
 * Form screen for adding/editing clothing details.
 * Pre-fills detected information from AI and allows user customization.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClothingDetailsFormScreen(
    imageFile: File?,
    detectedInfo: DetectedClothingInfo,
    onBack: () -> Unit,
    onSave: (ClothingFormData) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(detectedInfo.category) }
    var brand by remember { mutableStateOf("") }
    var colors by remember { mutableStateOf(detectedInfo.colors) }
    var pattern by remember { mutableStateOf(detectedInfo.pattern) }
    var season by remember { mutableStateOf<String?>(null) }
    var preferences by remember { mutableStateOf<List<String>>(emptyList()) }
    var notes by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Clothing Details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    onSave(
                        ClothingFormData(
                            name = name,
                            category = category,
                            brand = brand,
                            colors = colors,
                            pattern = pattern,
                            season = season,
                            preferences = preferences,
                            notes = notes,
                            imageUri = imageFile?.let { Uri.fromFile(it) }
                        )
                    )
                },
                icon = { Icon(Icons.Default.Check, "Save") },
                text = { Text("Save Item") },
                containerColor = MaterialTheme.colorScheme.primary
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Image Preview
            if (imageFile != null) {
                ImagePreviewCard(imageFile = imageFile, isProcessing = isProcessing)
                Spacer(modifier = Modifier.height(24.dp))
            }

            // AI Detection Banner (if data detected)
            if (detectedInfo.category.isNotEmpty() || detectedInfo.colors.isNotEmpty() || detectedInfo.pattern.isNotEmpty()) {
                AIDetectionBanner()
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Form Fields
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Item Name") },
                placeholder = { Text("e.g., Blue Denim Jacket") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Category Selector
            CategorySelector(
                selectedCategory = category,
                onCategorySelected = { category = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = brand,
                onValueChange = { brand = it },
                label = { Text("Brand (Optional)") },
                placeholder = { Text("e.g., Levi's, H&M") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Color Chips
            ColorChipSelector(
                selectedColors = colors,
                onColorsChanged = { colors = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Pattern Selector
            PatternSelector(
                selectedPattern = pattern,
                onPatternSelected = { pattern = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Season Selector
            SeasonSelector(
                selectedSeason = season,
                onSeasonSelected = { season = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Preference Selector (NEW)
            PreferenceSelector(
                selectedPreferences = preferences,
                onPreferencesChanged = { preferences = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes (Optional)") },
                placeholder = { Text("Fit, occasion, styling tips...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                maxLines = 5
            )

            Spacer(modifier = Modifier.height(80.dp)) // Space for FAB
        }
    }
}

@Composable
private fun ImagePreviewCard(imageFile: File, isProcessing: Boolean) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = rememberAsyncImagePainter(imageFile),
                contentDescription = "Captured clothing",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            if (isProcessing) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun AIDetectionBanner() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "✨",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "AI Detection Active",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "Some details pre-filled. Review and edit as needed.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun CategorySelector(
    selectedCategory: String,
    onCategorySelected: (String) -> Unit
) {
    val categories = listOf("Tops", "Bottoms", "Dresses", "Outerwear", "Shoes", "Accessories")

    Column {
        Text(
            text = "Category *",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categories.take(3).forEach { category ->
                CategoryChip(
                    label = category,
                    isSelected = selectedCategory == category,
                    onClick = { onCategorySelected(category) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categories.drop(3).forEach { category ->
                CategoryChip(
                    label = category,
                    isSelected = selectedCategory == category,
                    onClick = { onCategorySelected(category) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun CategoryChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected)
            MaterialTheme.colorScheme.primary
        else
            MaterialTheme.colorScheme.surfaceVariant,
        border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected)
                    MaterialTheme.colorScheme.onPrimary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ColorChipSelector(
    selectedColors: List<String>,
    onColorsChanged: (List<String>) -> Unit
) {
    val availableColors = listOf(
        "Black" to Color(0xFF000000),
        "White" to Color(0xFFFFFFFF),
        "Red" to Color(0xFFE53935),
        "Blue" to Color(0xFF1E88E5),
        "Green" to Color(0xFF43A047),
        "Yellow" to Color(0xFFFDD835),
        "Orange" to Color(0xFFFF6F00),
        "Pink" to Color(0xFFEC407A),
        "Purple" to Color(0xFF8E24AA),
        "Brown" to Color(0xFF6D4C41),
        "Gray" to Color(0xFF757575),
        "Navy" to Color(0xFF1A237E),
        "Beige" to Color(0xFFD7CCC8)
    )

    Column {
        Text(
            text = "Colors (Select up to 3)",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalSpacing = 8.dp,
            verticalSpacing = 8.dp
        ) {
            availableColors.forEach { (colorName, colorValue) ->
                val isSelected = selectedColors.contains(colorName)
                ColorChip(
                    label = colorName,
                    color = colorValue,
                    isSelected = isSelected,
                    onClick = {
                        onColorsChanged(
                            if (isSelected) {
                                selectedColors - colorName
                            } else if (selectedColors.size < 3) {
                                selectedColors + colorName
                            } else {
                                selectedColors
                            }
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun ColorChip(
    label: String,
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = if (isSelected)
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        else
            null
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(color)
                    .border(
                        1.dp,
                        if (label == "White") Color.Gray.copy(alpha = 0.3f) else Color.Transparent,
                        RoundedCornerShape(4.dp)
                    )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

@Composable
private fun PatternSelector(
    selectedPattern: String,
    onPatternSelected: (String) -> Unit
) {
    val patterns = listOf("Solid", "Striped", "Checkered", "Floral", "Printed", "Other")

    Column {
        Text(
            text = "Pattern",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalSpacing = 8.dp,
            verticalSpacing = 8.dp
        ) {
            patterns.forEach { pattern ->
                FilterChip(
                    selected = selectedPattern == pattern,
                    onClick = { onPatternSelected(pattern) },
                    label = { Text(pattern) },
                    leadingIcon = if (selectedPattern == pattern) {
                        { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) }
                    } else null
                )
            }
        }
    }
}

@Composable
private fun SeasonSelector(
    selectedSeason: String?,
    onSeasonSelected: (String?) -> Unit
) {
    val seasons = listOf(
        "Spring" to "🌸",
        "Summer" to "☀️",
        "Fall" to "🍂",
        "Winter" to "❄️",
        "All Season" to "🔄"
    )

    Column {
        Text(
            text = "Season",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            seasons.forEach { (season, emoji) ->
                SeasonChip(
                    label = season,
                    emoji = emoji,
                    isSelected = selectedSeason == season,
                    onClick = {
                        onSeasonSelected(if (selectedSeason == season) null else season)
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun SeasonChip(
    label: String,
    emoji: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected)
            MaterialTheme.colorScheme.secondaryContainer
        else
            MaterialTheme.colorScheme.surfaceVariant,
        border = if (isSelected)
            BorderStroke(2.dp, MaterialTheme.colorScheme.secondary)
        else
            null
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = emoji,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label.split(" ").first(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected)
                    MaterialTheme.colorScheme.onSecondaryContainer
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * NEW: Preference Selector for wardrobe recommendations
 * Tags items with style preferences to enable smart outfit matching
 */
@Composable
private fun PreferenceSelector(
    selectedPreferences: List<String>,
    onPreferencesChanged: (List<String>) -> Unit
) {
    val preferenceOptions = listOf(
        "Casual" to "👕",
        "Formal" to "👔",
        "Business" to "💼",
        "Athletic" to "⚽",
        "Party" to "🎉",
        "Date Night" to "💝",
        "Streetwear" to "🛹",
        "Vintage" to "📻",
        "Minimalist" to "⚪",
        "Bohemian" to "🌿",
        "Preppy" to "🎓",
        "Edgy" to "⚡"
    )

    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Text(
                text = "Style Preferences",
                style = MaterialTheme.typography.labelLarge
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "(for outfit recommendations)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Info card explaining the feature
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "💡", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Select styles to get matching outfit suggestions from your wardrobe",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalSpacing = 8.dp,
            verticalSpacing = 8.dp
        ) {
            preferenceOptions.forEach { (preference, emoji) ->
                val isSelected = selectedPreferences.contains(preference)
                PreferenceChip(
                    label = preference,
                    emoji = emoji,
                    isSelected = isSelected,
                    onClick = {
                        onPreferencesChanged(
                            if (isSelected) {
                                selectedPreferences - preference
                            } else {
                                selectedPreferences + preference
                            }
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun PreferenceChip(
    label: String,
    emoji: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = emoji)
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = label)
            }
        },
        leadingIcon = if (isSelected) {
            { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) }
        } else null,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onTertiaryContainer
        )
    )
}

/**
 * Data class to hold form data
 */
data class ClothingFormData(
    val name: String,
    val category: String,
    val brand: String,
    val colors: List<String>,
    val pattern: String,
    val season: String?,
    val preferences: List<String>, // NEW: Style preferences for recommendations
    val notes: String,
    val imageUri: Uri?
)

/**
 * Custom FlowRow layout for wrapping chips
 */
@Composable
private fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalSpacing: Dp = 0.dp,
    verticalSpacing: Dp = 0.dp,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val horizontalSpacingPx = with(density) { horizontalSpacing.roundToPx() }
    val verticalSpacingPx = with(density) { verticalSpacing.roundToPx() }

    Layout(
        content = content,
        modifier = modifier
    ) { measurables, constraints ->
        val placeables = measurables.map { it.measure(constraints) }

        var xPos = 0
        var yPos = 0
        var maxHeight = 0

        val positions = mutableListOf<Pair<Int, Int>>()

        placeables.forEach { placeable ->
            if (xPos + placeable.width > constraints.maxWidth) {
                xPos = 0
                yPos += maxHeight + verticalSpacingPx
                maxHeight = 0
            }

            positions.add(xPos to yPos)
            xPos += placeable.width + horizontalSpacingPx
            maxHeight = maxOf(maxHeight, placeable.height)
        }

        val height = yPos + maxHeight

        layout(constraints.maxWidth, height) {
            placeables.forEachIndexed { index, placeable ->
                val (x, y) = positions[index]
                placeable.place(x, y)
            }
        }
    }
}
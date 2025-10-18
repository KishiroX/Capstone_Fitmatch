package com.example.capstone.ui.screen

import android.net.Uri
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.times
import coil.compose.AsyncImage
import com.example.capstone.utils.ImageProcessorUtil
import com.example.capstone.utils.ProcessedImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

// Cloudinary imports
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback

// Updated ClothingItem data class to support Firestore
data class ClothingItem(
    val id: String = "",
    val name: String = "",
    val category: String = "",
    val colors: List<String> = emptyList(),
    val brand: String = "",
    val pattern: String = "",
    val season: String? = null,
    val notes: String = "",
    val imageUri: String? = null,
    val processedImageUri: String? = null,
    val thumbnailUri: String? = null,
    val worn: Int = 0,
    val lastWorn: Long? = null,
    val isFavorite: Boolean = false,
    val timestamp: com.google.firebase.Timestamp = com.google.firebase.Timestamp.now()
)

enum class Category(val key: String, val label: String, val icon: String) {
    ALL("all", "All", "👕"),
    TOPS("Tops", "Tops", "👕"),
    BOTTOMS("Bottoms", "Bottoms", "👖"),
    DRESSES("Dresses", "Dresses", "👗"),
    OUTERWEAR("Outerwear", "Outerwear", "🧥"),
    SHOES("Shoes", "Shoes", "👟"),

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WardrobeScreen(onNavigate: (String) -> Unit) {
    val context = LocalContext.current
    var selectedCategory by remember { mutableStateOf(Category.ALL) }

    // Firebase instances
    val db = remember { FirebaseFirestore.getInstance() }
    val userId = remember { FirebaseAuth.getInstance().currentUser?.uid }

    // Cloudinary initialization
    var cloudinaryInitialized by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        try {
            if (!cloudinaryInitialized) {
                val config = mapOf(
                    "cloud_name" to "dt4vdr1qy",
                    "api_key" to "411389739366478",        // ⚠️ Replace with your NEW API key
                    "api_secret" to "5NlScSbjWrCzumI-M6569bm0NCU"
                )
                MediaManager.init(context, config)
                cloudinaryInitialized = true
                android.util.Log.d("Wardrobe", "✅ Cloudinary initialized")
            }
        } catch (e: Exception) {
            android.util.Log.e("Wardrobe", "❌ Failed to initialize Cloudinary", e)
        }
    }

    // Function to upload image to Cloudinary (embedded)
    suspend fun uploadToCloudinary(file: File, folder: String): String = suspendCancellableCoroutine { continuation ->
        if (!cloudinaryInitialized) {
            continuation.resumeWithException(Exception("Cloudinary not initialized"))
            return@suspendCancellableCoroutine
        }

        if (!file.exists()) {
            continuation.resumeWithException(Exception("File does not exist: ${file.name}"))
            return@suspendCancellableCoroutine
        }

        android.util.Log.d("Wardrobe", "📤 Uploading ${file.name} to $folder")

        try {
            val requestId = MediaManager.get()
                .upload(file.absolutePath)
                .option("folder", "$folder/$userId")
                .option("resource_type", "image")
                .unsigned("ml_default")
                .callback(object : UploadCallback {
                    override fun onStart(requestId: String) {
                        android.util.Log.d("Wardrobe", "⏳ Upload started")
                    }

                    override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {
                        val progress = (bytes * 100 / totalBytes).toInt()
                        android.util.Log.d("Wardrobe", "📊 Progress: $progress%")
                    }

                    override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                        val secureUrl = resultData["secure_url"] as? String
                        if (secureUrl != null) {
                            android.util.Log.d("Wardrobe", "✅ Upload complete: $secureUrl")
                            continuation.resume(secureUrl)
                        } else {
                            continuation.resumeWithException(Exception("No URL in response"))
                        }
                    }

                    override fun onError(requestId: String, error: ErrorInfo) {
                        android.util.Log.e("Wardrobe", "❌ Upload failed: ${error.description}")
                        continuation.resumeWithException(Exception("Upload failed: ${error.description}"))
                    }

                    override fun onReschedule(requestId: String, error: ErrorInfo) {
                        android.util.Log.w("Wardrobe", "⚠️ Upload rescheduled")
                    }
                })
                .dispatch()

            continuation.invokeOnCancellation {
                try {
                    MediaManager.get().cancelRequest(requestId)
                } catch (e: Exception) {
                    android.util.Log.w("Wardrobe", "Failed to cancel", e)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("Wardrobe", "❌ Upload error", e)
            continuation.resumeWithException(e)
        }
    }

    // Mutable list of clothing items (synced with Firestore)
    var clothingItems by remember { mutableStateOf(listOf<ClothingItem>()) }
    var isLoadingItems by remember { mutableStateOf(true) }

    // Bottom Sheet State
    val sheetState = rememberModalBottomSheetState()
    var showBottomSheet by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Navigation States for Add Flow
    var showCameraScreen by remember { mutableStateOf(false) }
    var showFormScreen by remember { mutableStateOf(false) }
    var capturedImageFile by remember { mutableStateOf<File?>(null) }
    var processedImages by remember { mutableStateOf<ProcessedImage?>(null) }
    var detectedInfo by remember { mutableStateOf<DetectedClothingInfo?>(null) }
    var isProcessing by remember { mutableStateOf(false) }

    // Snackbar
    val snackbarHostState = remember { SnackbarHostState() }

    // Load clothing items from Firestore on launch
    LaunchedEffect(userId) {
        if (userId != null) {
            try {
                isLoadingItems = true
                android.util.Log.d("Wardrobe", "Loading items for user: $userId")

                db.collection("users")
                    .document(userId)
                    .collection("wardrobe")
                    .get()
                    .await()
                    .let { snapshot ->
                        clothingItems = snapshot.documents.mapNotNull { doc ->
                            try {
                                doc.toObject(ClothingItem::class.java)?.copy(id = doc.id)
                            } catch (e: Exception) {
                                android.util.Log.e("Wardrobe", "Error parsing item ${doc.id}", e)
                                null
                            }
                        }
                        android.util.Log.d("Wardrobe", "✅ Loaded ${clothingItems.size} items")
                    }
            } catch (e: Exception) {
                android.util.Log.e("Wardrobe", "❌ Error loading items", e)
                scope.launch {
                    snackbarHostState.showSnackbar("Failed to load wardrobe items")
                }
            } finally {
                isLoadingItems = false
            }
        } else {
            android.util.Log.w("Wardrobe", "⚠️ No user logged in")
            isLoadingItems = false
        }
    }

    // Calculate stats
    val totalItems = clothingItems.size
    val favoritesCount = clothingItems.count { it.isFavorite }

    // Filter items by category
    val filteredItems = remember(selectedCategory, clothingItems) {
        if (selectedCategory == Category.ALL) {
            clothingItems
        } else {
            clothingItems.filter { it.category == selectedCategory.key }
        }
    }

    // Show Camera Screen
    if (showCameraScreen) {
        CameraClothingScreen(
            onBack = {
                showCameraScreen = false
            },
            onImageCaptured = { imageFile ->
                capturedImageFile = imageFile
                showCameraScreen = false
                isProcessing = true

                scope.launch {
                    try {
                        android.util.Log.d("Wardrobe", "Starting image processing...")
                        val imageProcessor = ImageProcessorUtil(context)

                        android.util.Log.d("Wardrobe", "Removing background...")
                        val processed = imageProcessor.processClothingImage(imageFile)
                        processedImages = processed

                        android.util.Log.d("Wardrobe", "Detecting colors...")
                        val colors = imageProcessor.detectDominantColors(
                            android.graphics.BitmapFactory.decodeFile(processed.processedFile.absolutePath),
                            numColors = 3
                        )

                        detectedInfo = DetectedClothingInfo(
                            category = "Tops",
                            colors = colors,
                            pattern = "Solid",
                            clothingType = "Athletic Top"
                        )

                        android.util.Log.d("Wardrobe", "Processing complete!")
                        isProcessing = false
                        showFormScreen = true

                    } catch (e: Exception) {
                        android.util.Log.e("Wardrobe", "Error processing image", e)
                        isProcessing = false
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                message = "Failed to process image: ${e.message}",
                                duration = SnackbarDuration.Long
                            )
                        }
                    }
                }
            }
        )
        return
    }

    // Show Processing Indicator
    if (isProcessing) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF9FAFB)),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        color = Color(0xFF10B981),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Processing...",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F2937)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Uploading images to cloud",
                        fontSize = 12.sp,
                        color = Color(0xFF6B7280),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        return
    }

    // Show Form Screen
    if (showFormScreen && processedImages != null && detectedInfo != null) {
        ClothingDetailsFormScreen(
            imageFile = processedImages!!.originalFile,
            detectedInfo = detectedInfo!!,
            onBack = {
                showFormScreen = false
                capturedImageFile = null
                processedImages = null
                detectedInfo = null
            },
            onSave = { formData ->
                scope.launch {
                    try {
                        if (userId == null) {
                            snackbarHostState.showSnackbar("Please log in to save items")
                            return@launch
                        }

                        isProcessing = true
                        android.util.Log.d("Wardrobe", "🚀 Starting save process...")

                        // Upload images to Cloudinary
                        android.util.Log.d("Wardrobe", "📤 Uploading to Cloudinary...")

                        val originalUrl = uploadToCloudinary(
                            processedImages!!.originalFile,
                            "wardrobe_images/original"
                        )

                        val processedUrl = uploadToCloudinary(
                            processedImages!!.processedFile,
                            "wardrobe_images/processed"
                        )

                        val thumbnailUrl = uploadToCloudinary(
                            processedImages!!.thumbnailFile,
                            "wardrobe_images/thumbnails"
                        )

                        android.util.Log.d("Wardrobe", "✅ All images uploaded")

                        // Create clothing item data
                        val itemData = hashMapOf(
                            "name" to formData.name,
                            "category" to formData.category,
                            "colors" to formData.colors,
                            "brand" to formData.brand,
                            "pattern" to formData.pattern,
                            "season" to formData.season,
                            "notes" to formData.notes,
                            "imageUri" to originalUrl,
                            "processedImageUri" to processedUrl,
                            "thumbnailUri" to thumbnailUrl,
                            "worn" to 0,
                            "lastWorn" to null,
                            "isFavorite" to false,
                            "timestamp" to com.google.firebase.Timestamp.now()
                        )

                        android.util.Log.d("Wardrobe", "💾 Saving to Firestore...")

                        val docRef = db.collection("users")
                            .document(userId)
                            .collection("wardrobe")
                            .add(itemData)
                            .await()

                        android.util.Log.d("Wardrobe", "✅ Saved with ID: ${docRef.id}")

                        // Create local item
                        val newItem = ClothingItem(
                            id = docRef.id,
                            name = formData.name,
                            category = formData.category,
                            colors = formData.colors,
                            brand = formData.brand,
                            pattern = formData.pattern,
                            season = formData.season,
                            notes = formData.notes,
                            imageUri = originalUrl,
                            processedImageUri = processedUrl,
                            thumbnailUri = thumbnailUrl,
                            worn = 0,
                            lastWorn = null,
                            isFavorite = false,
                            timestamp = com.google.firebase.Timestamp.now()
                        )

                        // Update local list
                        clothingItems = listOf(newItem) + clothingItems

                        // Cleanup temp files
                        try {
                            processedImages!!.originalFile.delete()
                            processedImages!!.processedFile.delete()
                            processedImages!!.thumbnailFile.delete()
                        } catch (e: Exception) {
                            android.util.Log.w("Wardrobe", "Failed to delete temp files", e)
                        }

                        // Reset states
                        showFormScreen = false
                        capturedImageFile = null
                        processedImages = null
                        detectedInfo = null
                        isProcessing = false

                        snackbarHostState.showSnackbar(
                            message = "✓ ${formData.name} added successfully!",
                            duration = SnackbarDuration.Short
                        )

                    } catch (e: Exception) {
                        android.util.Log.e("Wardrobe", "❌ Save failed", e)
                        isProcessing = false

                        val errorMessage = when {
                            e.message?.contains("Cloudinary not initialized") == true ->
                                "Cloudinary error. Check your credentials."
                            e.message?.contains("Upload failed") == true ->
                                "Upload failed: ${e.message}"
                            else ->
                                "Failed to save: ${e.message}"
                        }

                        snackbarHostState.showSnackbar(
                            message = errorMessage,
                            duration = SnackbarDuration.Long
                        )
                    }
                }
            }
        )
        return
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showBottomSheet = true },
                containerColor = Color(0xFF10B981),
                shape = CircleShape,
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Clothing",
                    tint = Color.White
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF9FAFB))
                .verticalScroll(rememberScrollState())
                .padding(padding)
        ) {
            WardrobeHeader(onNavigate)
            StatsCard(
                totalItems = totalItems,
                favoritesCount = favoritesCount
            )
            CategoryFilter(selectedCategory) { selectedCategory = it }

            if (isLoadingItems) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF10B981))
                }
            } else {
                ItemsGrid(
                    filteredItems = filteredItems,
                    selectedCategory = selectedCategory,
                    onFavoriteToggle = { item ->
                        scope.launch {
                            try {
                                if (userId == null) return@launch

                                db.collection("users")
                                    .document(userId)
                                    .collection("wardrobe")
                                    .document(item.id)
                                    .update("isFavorite", !item.isFavorite)
                                    .await()

                                clothingItems = clothingItems.map {
                                    if (it.id == item.id) it.copy(isFavorite = !it.isFavorite)
                                    else it
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("Wardrobe", "❌ Failed to update favorite", e)
                                snackbarHostState.showSnackbar("Failed to update favorite")
                            }
                        }
                    },
                    onMarkAsWorn = { item ->
                        scope.launch {
                            try {
                                if (userId == null) return@launch

                                val newWorn = item.worn + 1
                                val newLastWorn = System.currentTimeMillis()

                                db.collection("users")
                                    .document(userId)
                                    .collection("wardrobe")
                                    .document(item.id)
                                    .update(
                                        mapOf(
                                            "worn" to newWorn,
                                            "lastWorn" to newLastWorn
                                        )
                                    )
                                    .await()

                                clothingItems = clothingItems.map {
                                    if (it.id == item.id) {
                                        it.copy(worn = newWorn, lastWorn = newLastWorn)
                                    } else it
                                }

                                snackbarHostState.showSnackbar(
                                    message = "✓ Marked as worn!",
                                    duration = SnackbarDuration.Short
                                )
                            } catch (e: Exception) {
                                android.util.Log.e("Wardrobe", "❌ Failed to mark as worn", e)
                                snackbarHostState.showSnackbar("Failed to update")
                            }
                        }
                    }
                )
            }
        }
    }

    // Add Clothing Bottom Sheet
    if (showBottomSheet) {
        AddClothingBottomSheet(
            sheetState = sheetState,
            onDismiss = {
                scope.launch {
                    sheetState.hide()
                    showBottomSheet = false
                }
            },
            onTakePhoto = {
                showCameraScreen = true
                showBottomSheet = false
            },
            onChooseFromGallery = {
                scope.launch {
                    snackbarHostState.showSnackbar("Gallery picker coming soon!")
                    sheetState.hide()
                    showBottomSheet = false
                }
            },
            onManualEntry = {
                capturedImageFile = null
                processedImages = null
                detectedInfo = DetectedClothingInfo(
                    category = "Tops",
                    colors = emptyList(),
                    pattern = "Solid",
                    clothingType = "Clothing Item"
                )
                showFormScreen = true
                showBottomSheet = false
            }
        )
    }
}

// Keep all your existing composable functions below:
// WardrobeHeader, StatsCard, StatItem, CategoryFilter, ItemsGrid,
// ClothingCard, ClothingImageSection, ClothingDetailsSection,
// EmptyState, getRelativeTime...

@Composable
private fun WardrobeHeader(onNavigate: (String) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.horizontalGradient(
                    colors = listOf(Color(0xFF10B981), Color(0xFF0D9488))
                )
            )
            .padding(24.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { onNavigate("home") }) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Wardrobe",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Organize your fitness wardrobe and track your outfits",
                fontSize = 14.sp,
                color = Color(0xFFD1FAE5),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun StatsCard(totalItems: Int, favoritesCount: Int) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .offset(y = (-16).dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(value = "$totalItems", label = "Items", color = Color(0xFF1F2937))
            StatItem(value = "$favoritesCount", label = "Favorites", color = Color(0xFF10B981))
            StatItem(value = "0", label = "Outfits", color = Color(0xFF3B82F6))
        }
    }
}

@Composable
private fun StatItem(value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            fontSize = 14.sp,
            color = Color(0xFF6B7280)
        )
    }
}

@Composable
private fun CategoryFilter(
    selectedCategory: Category,
    onCategorySelected: (Category) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Category.values().forEach { category ->
            FilterChip(
                selected = selectedCategory == category,
                onClick = { onCategorySelected(category) },
                label = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(category.icon, fontSize = 14.sp)
                        Text(category.label, fontSize = 14.sp)
                    }
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFF10B981),
                    selectedLabelColor = Color.White,
                    containerColor = Color.White,
                    labelColor = Color(0xFF6B7280)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.wrapContentWidth()
            )
        }
    }
}

@Composable
private fun ItemsGrid(
    filteredItems: List<ClothingItem>,
    selectedCategory: Category,
    onFavoriteToggle: (ClothingItem) -> Unit,
    onMarkAsWorn: (ClothingItem) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 80.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (selectedCategory == Category.ALL) "All Items" else selectedCategory.label,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1F2937)
            )
            Text(
                text = "${filteredItems.size} items",
                fontSize = 14.sp,
                color = Color(0xFF10B981)
            )
        }

        if (filteredItems.isEmpty()) {
            EmptyState(selectedCategory.label)
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.height((filteredItems.size / 2 + 1) * 320.dp)
            ) {
                items(filteredItems, key = { it.id }) { item ->
                    ClothingCard(
                        item = item,
                        onFavoriteClick = { onFavoriteToggle(item) },
                        onMarkAsWorn = { onMarkAsWorn(item) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ClothingCard(
    item: ClothingItem,
    onFavoriteClick: () -> Unit,
    onMarkAsWorn: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column {
            ClothingImageSection(item, onFavoriteClick)
            ClothingDetailsSection(item, onMarkAsWorn)
        }
    }
}

@Composable
private fun ClothingImageSection(
    item: ClothingItem,
    onFavoriteClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFFF9FAFB), Color(0xFFF3F4F6))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        val displayImage = item.thumbnailUri ?: item.processedImageUri ?: item.imageUri

        if (displayImage != null) {
            AsyncImage(
                model = displayImage,
                contentDescription = item.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Icon(
                imageVector = Icons.Default.ShoppingBag,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = Color(0xFF9CA3AF)
            )
        }

        IconButton(
            onClick = onFavoriteClick,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
                .size(32.dp)
                .background(Color.White.copy(alpha = 0.9f), CircleShape)
        ) {
            Icon(
                imageVector = if (item.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = "Favorite",
                tint = if (item.isFavorite) Color(0xFFEF4444) else Color(0xFF9CA3AF),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun ClothingDetailsSection(
    item: ClothingItem,
    onMarkAsWorn: () -> Unit
) {
    Column(modifier = Modifier.padding(12.dp)) {
        Text(
            text = item.name,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF1F2937),
            maxLines = 1
        )

        if (item.brand.isNotEmpty()) {
            Text(
                text = item.brand,
                fontSize = 12.sp,
                color = Color(0xFF6B7280),
                maxLines = 1
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (item.colors.isNotEmpty()) {
            Text(
                text = item.colors.joinToString(", "),
                fontSize = 11.sp,
                color = Color(0xFF10B981),
                maxLines = 1
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Worn ${item.worn}×",
                fontSize = 11.sp,
                color = Color(0xFF6B7280)
            )

            item.lastWorn?.let { date ->
                Text(
                    text = getRelativeTime(date),
                    fontSize = 10.sp,
                    color = Color(0xFF9CA3AF)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            onClick = onMarkAsWorn,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(vertical = 8.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = Color(0xFF10B981)
            )
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("Wore This", fontSize = 12.sp)
        }
    }
}

@Composable
private fun EmptyState(category: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(Color(0xFFF3F4F6), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.ShoppingBag,
                contentDescription = "No items",
                tint = Color(0xFF9CA3AF),
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "No Items Found",
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF1F2937)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Tap the + button to add clothing items",
            fontSize = 14.sp,
            color = Color(0xFF6B7280),
            textAlign = TextAlign.Center
        )
    }
}

// Helper function to format relative time
private fun getRelativeTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60000 -> "Just now"
        diff < 3600000 -> "${diff / 60000}m ago"
        diff < 86400000 -> "${diff / 3600000}h ago"
        diff < 604800000 -> "${diff / 86400000}d ago"
        else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(timestamp))
    }
}
package com.example.capstone.ui.screen

import android.net.Uri
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.draw.clip
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
import com.example.capstone.missions.MissionManager
import com.example.capstone.missions.UserProgress
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
fun WardrobeScreen(
    onNavigate: (String) -> Unit,
    capturedPhotoPath: String? = null,
    onPhotoProcessed: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedCategory by remember { mutableStateOf(Category.ALL) }

    // Firebase instances
    val db = remember { FirebaseFirestore.getInstance() }
    val userId = remember { FirebaseAuth.getInstance().currentUser?.uid }

    // Mission Manager for storage limits
    val missionManager = remember(userId) {
        if (userId != null) MissionManager(userId, db) else null
    }

    // User Progress State
    var userProgress by remember { mutableStateOf<UserProgress?>(null) }
    var showUpgradeDialog by remember { mutableStateOf(false) }

    val cloudinaryInitialized = remember { mutableStateOf(true) }

    // Function to upload image to Cloudinary
    suspend fun uploadToCloudinary(file: File, folder: String): String = suspendCancellableCoroutine { continuation ->
        if (!cloudinaryInitialized.value) {
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

    var clothingItems by remember { mutableStateOf(listOf<ClothingItem>()) }
    var isLoadingItems by remember { mutableStateOf(true) }

    val sheetState = rememberModalBottomSheetState()
    var showBottomSheet by remember { mutableStateOf(false) }

    var showFormScreen by remember { mutableStateOf(false) }
    var capturedImageFile by remember { mutableStateOf<File?>(null) }
    var processedImages by remember { mutableStateOf<ProcessedImage?>(null) }
    var detectedInfo by remember { mutableStateOf<DetectedClothingInfo?>(null) }
    var isProcessing by remember { mutableStateOf(false) }

    // Detail sheet state
    var selectedItem by remember { mutableStateOf<ClothingItem?>(null) }
    var showDetailSheet by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    // Load user progress for storage limits
    LaunchedEffect(userId) {
        if (userId != null && missionManager != null) {
            try {
                userProgress = missionManager.getUserProgress()
                android.util.Log.d("Wardrobe", "User storage: ${userProgress?.currentStorage}/${userProgress?.maxStorage}")
            } catch (e: Exception) {
                android.util.Log.e("Wardrobe", "Failed to load user progress", e)
            }
        }
    }

    // Load clothing items from Firestore
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

                        // Update currentStorage in Firestore
                        try {
                            db.collection("users")
                                .document(userId)
                                .update("currentStorage", clothingItems.size)
                                .await()

                            if (missionManager != null) {
                                userProgress = missionManager.getUserProgress()
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("Wardrobe", "Failed to update storage count", e)
                        }
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

    // Handle captured image path from navigation
    LaunchedEffect(capturedPhotoPath) {
        capturedPhotoPath?.let { path ->
            val imageFile = File(path)
            if (imageFile.exists()) {
                capturedImageFile = imageFile
                isProcessing = true
                onPhotoProcessed()

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
        }
    }

    val totalItems = clothingItems.size
    val favoritesCount = clothingItems.count { it.isFavorite }

    val filteredItems = remember(selectedCategory, clothingItems) {
        if (selectedCategory == Category.ALL) {
            clothingItems
        } else {
            clothingItems.filter { it.category == selectedCategory.key }
        }
    }

    fun canAddItem(): Boolean {
        val progress = userProgress ?: return false
        return progress.currentStorage < progress.maxStorage
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

                        if (!canAddItem()) {
                            snackbarHostState.showSnackbar(
                                message = "⚠️ Wardrobe is full! Level up to add more items.",
                                duration = SnackbarDuration.Long
                            )
                            showUpgradeDialog = true
                            return@launch
                        }

                        isProcessing = true
                        android.util.Log.d("Wardrobe", "🚀 Starting save process...")

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

                        clothingItems = listOf(newItem) + clothingItems

                        val newStorageCount = clothingItems.size
                        db.collection("users")
                            .document(userId)
                            .update("currentStorage", newStorageCount)
                            .await()

                        if (missionManager != null) {
                            userProgress = missionManager.getUserProgress()
                        }

                        try {
                            processedImages!!.originalFile.delete()
                            processedImages!!.processedFile.delete()
                            processedImages!!.thumbnailFile.delete()
                        } catch (e: Exception) {
                            android.util.Log.w("Wardrobe", "Failed to delete temp files", e)
                        }

                        showFormScreen = false
                        capturedImageFile = null
                        processedImages = null
                        detectedInfo = null
                        isProcessing = false

                        snackbarHostState.showSnackbar(
                            message = "✓ ${formData.name} added successfully! (${newStorageCount}/${userProgress?.maxStorage ?: 20})",
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
                onClick = {
                    if (!canAddItem()) {
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                message = "⚠️ Wardrobe is full! Level up to add more items.",
                                duration = SnackbarDuration.Long
                            )
                        }
                        showUpgradeDialog = true
                    } else {
                        showBottomSheet = true
                    }
                },
                containerColor = if (canAddItem()) Color(0xFF10B981) else Color(0xFF9CA3AF),
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

            userProgress?.let { progress ->
                StorageStatsCard(
                    currentStorage = progress.currentStorage,
                    maxStorage = progress.maxStorage,
                    userLevel = progress.level,
                    favoritesCount = favoritesCount,
                    onUpgradeClick = { onNavigate("missions") }
                )
            }

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
                    },
                    onDeleteItem = { item ->
                        scope.launch {
                            try {
                                if (userId == null) return@launch

                                db.collection("users")
                                    .document(userId)
                                    .collection("wardrobe")
                                    .document(item.id)
                                    .delete()
                                    .await()

                                clothingItems = clothingItems.filter { it.id != item.id }

                                val newStorageCount = clothingItems.size
                                db.collection("users")
                                    .document(userId)
                                    .update("currentStorage", newStorageCount)
                                    .await()

                                if (missionManager != null) {
                                    userProgress = missionManager.getUserProgress()
                                }

                                snackbarHostState.showSnackbar(
                                    message = "✓ Item removed",
                                    duration = SnackbarDuration.Short
                                )
                            } catch (e: Exception) {
                                android.util.Log.e("Wardrobe", "❌ Failed to delete item", e)
                                snackbarHostState.showSnackbar("Failed to delete")
                            }
                        }
                    },
                    onViewDetails = { item ->
                        selectedItem = item
                        showDetailSheet = true
                    }
                )
            }
        }
    }

    // Upgrade Dialog
    if (showUpgradeDialog) {
        UpgradeStorageDialog(
            currentStorage = userProgress?.currentStorage ?: 0,
            maxStorage = userProgress?.maxStorage ?: 20,
            currentLevel = userProgress?.level ?: 1,
            onDismiss = { showUpgradeDialog = false },
            onGoToMissions = {
                showUpgradeDialog = false
                onNavigate("missions")
            }
        )
    }

    // Detail Bottom Sheet
    if (showDetailSheet && selectedItem != null) {
        ClothingDetailBottomSheet(
            item = selectedItem!!,
            onDismiss = {
                showDetailSheet = false
                selectedItem = null
            },
            onEdit = {
                // TODO: Navigate to edit screen
                showDetailSheet = false
                scope.launch {
                    snackbarHostState.showSnackbar("Edit feature coming soon!")
                }
            },
            onDelete = { item ->
                scope.launch {
                    try {
                        if (userId == null) return@launch

                        db.collection("users")
                            .document(userId)
                            .collection("wardrobe")
                            .document(item.id)
                            .delete()
                            .await()

                        clothingItems = clothingItems.filter { it.id != item.id }

                        val newStorageCount = clothingItems.size
                        db.collection("users")
                            .document(userId)
                            .update("currentStorage", newStorageCount)
                            .await()

                        if (missionManager != null) {
                            userProgress = missionManager.getUserProgress()
                        }

                        showDetailSheet = false
                        selectedItem = null

                        snackbarHostState.showSnackbar(
                            message = "✓ Item removed",
                            duration = SnackbarDuration.Short
                        )
                    } catch (e: Exception) {
                        android.util.Log.e("Wardrobe", "❌ Failed to delete item", e)
                        snackbarHostState.showSnackbar("Failed to delete")
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

                        // Update selected item for detail sheet
                        selectedItem = clothingItems.find { it.id == item.id }

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
                onNavigate("camera/clothing")
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
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
private fun StorageStatsCard(
    currentStorage: Int,
    maxStorage: Int,
    userLevel: Int,
    favoritesCount: Int,
    onUpgradeClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .offset(y = (-16).dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Wardrobe Storage",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F2937)
                    )
                    Text(
                        "Level $userLevel",
                        fontSize = 12.sp,
                        color = Color(0xFF6B7280)
                    )
                }

                Text(
                    "$currentStorage / $maxStorage",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (currentStorage >= maxStorage) Color(0xFFEF4444) else Color(0xFF10B981)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            val progress = (currentStorage.toFloat() / maxStorage).coerceIn(0f, 1f)
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp)),
                color = when {
                    progress >= 1f -> Color(0xFFEF4444)
                    progress >= 0.8f -> Color(0xFFFBBF24)
                    else -> Color(0xFF10B981)
                },
                trackColor = Color(0xFFE5E7EB)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(value = "$currentStorage", label = "Items", color = Color(0xFF1F2937))
                StatItem(value = "$favoritesCount", label = "Favorites", color = Color(0xFF10B981))
                StatItem(value = "${maxStorage - currentStorage}", label = "Slots Left", color = Color(0xFF3B82F6))
            }

            if (currentStorage >= maxStorage * 0.8f) {
                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onUpgradeClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFFD700)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = Color.White
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (currentStorage >= maxStorage) "Wardrobe Full - Level Up!"
                        else "Almost Full - Level Up!",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
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
    onMarkAsWorn: (ClothingItem) -> Unit,
    onDeleteItem: (ClothingItem) -> Unit,
    onViewDetails: (ClothingItem) -> Unit
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
                        onMarkAsWorn = { onMarkAsWorn(item) },
                        onDeleteItem = { onDeleteItem(item) },
                        onViewDetails = { onViewDetails(item) }
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
    onMarkAsWorn: () -> Unit,
    onDeleteItem: () -> Unit,
    onViewDetails: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column {
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

                // Top-left: Worn badge
                if (item.worn > 0) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp),
                        color = Color(0xFF10B981).copy(alpha = 0.9f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "${item.worn}×",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                // Top-right: Favorite only
                IconButton(
                    onClick = onFavoriteClick,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(36.dp)
                        .background(Color.White.copy(alpha = 0.95f), CircleShape)
                ) {
                    Icon(
                        imageVector = if (item.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (item.isFavorite) Color(0xFFEF4444) else Color(0xFF9CA3AF),
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Bottom-right: More menu
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                ) {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color.White.copy(alpha = 0.95f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More options",
                            tint = Color(0xFF6B7280),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("View Details") },
                            onClick = {
                                showMenu = false
                                onViewDetails()
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Info, contentDescription = null)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete", color = Color(0xFFEF4444)) },
                            onClick = {
                                showMenu = false
                                showDeleteDialog = true
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = Color(0xFFEF4444)
                                )
                            }
                        )
                    }
                }
            }

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
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        item.colors.take(3).forEach { color ->
                            Surface(
                                color = Color(0xFFECFDF5),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = color,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    fontSize = 10.sp,
                                    color = Color(0xFF10B981),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        if (item.colors.size > 3) {
                            Surface(
                                color = Color(0xFFF3F4F6),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "+${item.colors.size - 3}",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    fontSize = 10.sp,
                                    color = Color(0xFF6B7280),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                item.lastWorn?.let { date ->
                    Text(
                        text = "Last worn: ${getRelativeTime(date)}",
                        fontSize = 10.sp,
                        color = Color(0xFF9CA3AF)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

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
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    "Delete Item?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    "Are you sure you want to remove \"${item.name}\" from your wardrobe? This action cannot be undone.",
                    color = Color(0xFF6B7280)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        onDeleteItem()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFEF4444)
                    )
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel", color = Color(0xFF6B7280))
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClothingDetailBottomSheet(
    item: ClothingItem,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: (ClothingItem) -> Unit,
    onMarkAsWorn: (ClothingItem) -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Item Details",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F2937)
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(Modifier.height(16.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFF9FAFB)),
                    contentAlignment = Alignment.Center
                ) {
                    val displayImage = item.processedImageUri ?: item.imageUri

                    if (displayImage != null) {
                        AsyncImage(
                            model = displayImage,
                            contentDescription = item.name,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.ShoppingBag,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = Color(0xFF9CA3AF)
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            DetailRow(label = "Name", value = item.name)

            if (item.brand.isNotEmpty()) {
                DetailRow(label = "Brand", value = item.brand)
            }

            DetailRow(label = "Category", value = item.category)

            if (item.colors.isNotEmpty()) {
                DetailRow(label = "Colors", value = item.colors.joinToString(", "))
            }

            if (item.pattern.isNotEmpty()) {
                DetailRow(label = "Pattern", value = item.pattern)
            }

            item.season?.let { season ->
                if (season.isNotEmpty()) {
                    DetailRow(label = "Season", value = season)
                }
            }

            DetailRow(label = "Times Worn", value = "${item.worn}×")

            item.lastWorn?.let { date ->
                DetailRow(label = "Last Worn", value = getRelativeTime(date))
            }

            if (item.notes.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Notes",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF6B7280)
                )
                Text(
                    item.notes,
                    fontSize = 14.sp,
                    color = Color(0xFF1F2937),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { onMarkAsWorn(item) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF10B981)
                    )
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Wore This")
                }

                Button(
                    onClick = onEdit,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF3B82F6)
                    )
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Edit")
                }
            }

            Spacer(Modifier.height(12.dp))

            OutlinedButton(
                onClick = { showDeleteDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFFEF4444)
                ),
                border = BorderStroke(1.dp, Color(0xFFEF4444))
            ) {
                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Delete Item")
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    "Delete Item?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    "Are you sure you want to remove \"${item.name}\" from your wardrobe? This action cannot be undone.",
                    color = Color(0xFF6B7280)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        onDelete(item)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFEF4444)
                    )
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel", color = Color(0xFF6B7280))
                }
            }
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF6B7280),
            modifier = Modifier.weight(0.4f)
        )
        Text(
            text = value,
            fontSize = 14.sp,
            color = Color(0xFF1F2937),
            modifier = Modifier.weight(0.6f),
            textAlign = TextAlign.End
        )
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

@Composable
fun UpgradeStorageDialog(
    currentStorage: Int,
    maxStorage: Int,
    currentLevel: Int,
    onDismiss: () -> Unit,
    onGoToMissions: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.EmojiEvents,
                contentDescription = null,
                tint = Color(0xFFFFD700),
                modifier = Modifier.size(48.dp)
            )
        },
        title = {
            Text(
                "Wardrobe Full!",
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "You've reached your storage limit ($currentStorage/$maxStorage items)",
                    textAlign = TextAlign.Center,
                    color = Color(0xFF6B7280)
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    "Level up to unlock more space!",
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Next level: +5 more slots",
                    fontSize = 14.sp,
                    color = Color(0xFF10B981),
                    textAlign = TextAlign.Center
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onGoToMissions,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF10B981)
                )
            ) {
                Text("Go to Missions", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Later", color = Color(0xFF6B7280))
            }
        }
    )
}

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
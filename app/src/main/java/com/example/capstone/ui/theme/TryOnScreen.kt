package com.example.capstone.ui.screens

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.example.capstone.analysis.BodyAnalyzer
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlin.math.*

// Data classes
data class ClothingItemTryOn(
    val id: String = "",
    val name: String = "",
    val category: String = "",
    val colors: List<String> = emptyList(),
    val brand: String = "",
    val imageUri: String? = null,
    val processedImageUri: String? = null,
    val thumbnailUri: String? = null,
    val worn: Int = 0,
    val isFavorite: Boolean = false
)

enum class CategoryTryOn(val key: String, val label: String) {
    TOPS("Tops", "Tops"),
    BOTTOMS("Bottoms", "Bottoms"),
    DRESSES("Dresses", "Dresses"),
    OUTERWEAR("Outerwear", "Outerwear"),
    SHOES("Shoes", "Shoes")
}

data class OutfitState(
    val tops: ClothingItemTryOn? = null,
    val bottoms: ClothingItemTryOn? = null,
    val shoes: ClothingItemTryOn? = null,
    val dress: ClothingItemTryOn? = null,
    val outerwear: ClothingItemTryOn? = null
)

fun OutfitState.getItemByCategory(category: String) = when(category) {
    "Tops" -> tops
    "Bottoms" -> bottoms
    "Shoes" -> shoes
    "Dresses" -> dress
    "Outerwear" -> outerwear
    else -> null
}

fun OutfitState.updateWithSelection(item: ClothingItemTryOn): OutfitState {
    return when(item.category) {
        "Tops" -> copy(tops = if (tops?.id == item.id) null else item, dress = null)
        "Bottoms" -> copy(bottoms = if (bottoms?.id == item.id) null else item, dress = null)
        "Dresses" -> copy(dress = if (dress?.id == item.id) null else item, tops = null, bottoms = null)
        "Shoes" -> copy(shoes = if (shoes?.id == item.id) null else item)
        "Outerwear" -> copy(outerwear = if (outerwear?.id == item.id) null else item)
        else -> this
    }
}

@Composable
fun TryOnScreen(onNavigate: (String) -> Unit) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val userId = auth.currentUser?.uid
    val scope = rememberCoroutineScope()

    val bodyAnalyzer = remember { BodyAnalyzer(context) }

    LaunchedEffect(Unit) {
        bodyAnalyzer.loadModel()
    }

    var outfit by remember { mutableStateOf(OutfitState()) }
    var activeCategory by remember { mutableStateOf(CategoryTryOn.TOPS) }
    var bodyScanUrl by remember { mutableStateOf<String?>(null) }
    var clothingItems by remember { mutableStateOf<List<ClothingItemTryOn>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isProcessingTryOn by remember { mutableStateOf(false) }
    var tryOnResultBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    // Load data with asset fallback
    LaunchedEffect(userId) {
        if (userId != null) {
            try {
                val userDoc = db.collection("users").document(userId).get().await()
                bodyScanUrl = userDoc.getString("bodyScanUrl")

                val wardrobeSnapshot = db.collection("users")
                    .document(userId)
                    .collection("wardrobe")
                    .get()
                    .await()

                val firebaseItems = wardrobeSnapshot.documents.mapNotNull { doc ->
                    try {
                        ClothingItemTryOn(
                            id = doc.id,
                            name = doc.getString("name") ?: "",
                            category = doc.getString("category") ?: "",
                            colors = (doc.get("colors") as? List<*>)?.mapNotNull { it.toString() } ?: emptyList(),
                            brand = doc.getString("brand") ?: "",
                            imageUri = doc.getString("imageUri"),
                            processedImageUri = doc.getString("processedImageUri"),
                            thumbnailUri = doc.getString("thumbnailUri"),
                            worn = (doc.getLong("worn") ?: 0).toInt(),
                            isFavorite = doc.getBoolean("isFavorite") ?: false
                        )
                    } catch (e: Exception) {
                        android.util.Log.e("TryOn", "Error parsing item ${doc.id}", e)
                        null
                    }
                }

                // Add demo items from assets if wardrobe is empty
                clothingItems = if (firebaseItems.isEmpty()) {
                    android.util.Log.d("TryOn", "📦 Loading demo items from assets")
                    loadDemoClothingItems(context)
                } else {
                    firebaseItems
                }

            } catch (e: Exception) {
                android.util.Log.e("TryOn", "Error loading data", e)
                // Fallback to demo items on error
                clothingItems = loadDemoClothingItems(context)
                scope.launch {
                    snackbarHostState.showSnackbar("Using demo wardrobe")
                }
            } finally {
                isLoading = false
            }
        } else {
            // No user logged in - show demo items
            clothingItems = loadDemoClothingItems(context)
            isLoading = false
        }
    }

    // Enhanced try-on generation
    LaunchedEffect(outfit, bodyScanUrl) {
        if (bodyScanUrl == null) return@LaunchedEffect

        val selectedItem = outfit.tops ?: outfit.dress ?: outfit.bottoms ?: outfit.outerwear ?: outfit.shoes ?: return@LaunchedEffect

        isProcessingTryOn = true
        tryOnResultBitmap = null

        try {
            android.util.Log.d("TryOn", "🎯 Generating try-on for ${selectedItem.name}")

            val clothingImageUri = selectedItem.processedImageUri ?: selectedItem.imageUri
            if (clothingImageUri == null) {
                android.util.Log.e("TryOn", "No image URI")
                return@LaunchedEffect
            }

            val bodyScanBitmap = loadBitmapFromUri(context, bodyScanUrl!!)
            var clothingBitmap = loadBitmapFromUri(context, clothingImageUri)

            if (bodyScanBitmap == null || clothingBitmap == null) {
                android.util.Log.e("TryOn", "Failed to load images")
                return@LaunchedEffect
            }

            val poseResult = withContext(Dispatchers.Default) {
                bodyAnalyzer.analyze(bodyScanBitmap)
            }

            if (poseResult == null) {
                android.util.Log.e("TryOn", "❌ No pose detected")
                scope.launch {
                    snackbarHostState.showSnackbar("Could not detect pose in body scan")
                }
                return@LaunchedEffect
            }

            android.util.Log.d("TryOn", "✅ Pose detected! Processing clothing...")

            // ENHANCED: Remove background from clothing and fix orientation
            clothingBitmap = withContext(Dispatchers.Default) {
                var processed = removeBackgroundFromClothing(clothingBitmap!!)

                // FIX: Force rotate 180° for all clothing items
                android.util.Log.d("TryOn", "🔄 Rotating clothing 180° to fix orientation")
                val rotateMatrix = Matrix().apply {
                    postRotate(180f, processed.width / 2f, processed.height / 2f)
                }
                processed = Bitmap.createBitmap(processed, 0, 0, processed.width, processed.height, rotateMatrix, true)

                processed
            }

            // ENHANCED: Apply improved overlay
            val result = withContext(Dispatchers.Default) {
                when(selectedItem.category) {
                    "Tops" -> enhancedOverlayTop(bodyScanBitmap, clothingBitmap, poseResult)
                    "Bottoms" -> enhancedOverlayBottoms(bodyScanBitmap, clothingBitmap, poseResult)
                    "Dresses" -> enhancedOverlayDress(bodyScanBitmap, clothingBitmap, poseResult)
                    "Shoes" -> enhancedOverlayShoes(bodyScanBitmap, clothingBitmap, poseResult)
                    "Outerwear" -> enhancedOverlayOuterwear(bodyScanBitmap, clothingBitmap, poseResult)
                    else -> bodyScanBitmap
                }
            }

            tryOnResultBitmap = result
            android.util.Log.d("TryOn", "✅ Try-on complete!")

        } catch (e: Exception) {
            android.util.Log.e("TryOn", "❌ Error generating try-on", e)
            scope.launch {
                snackbarHostState.showSnackbar("Try-on failed: ${e.message}")
            }
        } finally {
            isProcessingTryOn = false
        }
    }

    val categories = listOf(CategoryTryOn.TOPS, CategoryTryOn.BOTTOMS, CategoryTryOn.DRESSES, CategoryTryOn.OUTERWEAR, CategoryTryOn.SHOES)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Virtual Try-On") },
                navigationIcon = {
                    IconButton(onClick = { onNavigate("wardrobe") }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { /* Save */ }) {
                        Icon(Icons.Default.Favorite, contentDescription = "Save")
                    }
                    IconButton(onClick = { /* Share */ }) {
                        Icon(Icons.Default.Share, contentDescription = "Share")
                    }
                },
                backgroundColor = Color(0xFF10B981),
                contentColor = Color.White
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF9FAFB))
        ) {
            // Try-On Display
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(500.dp)
                    .background(Color(0xFFF9FAFB)),
                contentAlignment = Alignment.Center
            ) {
                when {
                    isProcessingTryOn -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(32.dp)
                        ) {
                            CircularProgressIndicator(color = Color(0xFF10B981), modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("🎯 AI Processing...", textAlign = TextAlign.Center, color = Color.Gray)
                        }
                    }
                    tryOnResultBitmap != null -> {
                        Image(
                            bitmap = tryOnResultBitmap!!.asImageBitmap(),
                            contentDescription = "Try-On Result",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )

                        Surface(
                            modifier = Modifier.align(Alignment.TopStart).padding(16.dp),
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF10B981).copy(alpha = 0.9f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Enhanced AI Fitting", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                    bodyScanUrl == null -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Icon(Icons.Default.Person, contentDescription = "No Body Scan", modifier = Modifier.size(64.dp), tint = Color.Gray)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Complete your body scan first", textAlign = TextAlign.Center, color = Color.Gray)
                        }
                    }
                    else -> {
                        Image(
                            painter = rememberAsyncImagePainter(bodyScanUrl),
                            contentDescription = "Body Scan",
                            modifier = Modifier.fillMaxHeight(),
                            contentScale = ContentScale.Fit
                        )
                    }
                }

                // Selected items badge
                if (outfit.tops != null || outfit.dress != null || outfit.bottoms != null || outfit.shoes != null || outfit.outerwear != null) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp)
                            .background(Color.White.copy(alpha = 0.9f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Text("Selected:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                        Spacer(modifier = Modifier.height(6.dp))
                        outfit.dress?.let { Row(verticalAlignment = Alignment.CenterVertically) { Text("👗 ", fontSize = 14.sp); Text(it.name, fontSize = 10.sp, maxLines = 1) } }
                        outfit.tops?.let { Row(verticalAlignment = Alignment.CenterVertically) { Text("👕 ", fontSize = 14.sp); Text(it.name, fontSize = 10.sp, maxLines = 1) } }
                        outfit.bottoms?.let { Row(verticalAlignment = Alignment.CenterVertically) { Text("👖 ", fontSize = 14.sp); Text(it.name, fontSize = 10.sp, maxLines = 1) } }
                        outfit.outerwear?.let { Row(verticalAlignment = Alignment.CenterVertically) { Text("🧥 ", fontSize = 14.sp); Text(it.name, fontSize = 10.sp, maxLines = 1) } }
                        outfit.shoes?.let { Row(verticalAlignment = Alignment.CenterVertically) { Text("👟 ", fontSize = 14.sp); Text(it.name, fontSize = 10.sp, maxLines = 1) } }
                    }
                }
            }

            // Categories
            LazyRow(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(categories) { category ->
                    Button(
                        onClick = { activeCategory = category },
                        colors = ButtonDefaults.buttonColors(backgroundColor = if (activeCategory == category) Color(0xFF10B981) else Color.White),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(category.label, color = if (activeCategory == category) Color.White else Color.Black)
                    }
                }
            }

            // Clothing grid
            if (isLoading) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF10B981))
                }
            } else {
                val filteredItems = clothingItems.filter { it.category == activeCategory.key }
                if (filteredItems.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.ShoppingBag, contentDescription = null, modifier = Modifier.size(48.dp), tint = Color.Gray)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("No ${activeCategory.label}", color = Color.Gray)
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredItems) { item ->
                            ClothingCardTryOn(
                                item = item,
                                isSelected = outfit.getItemByCategory(item.category)?.id == item.id,
                                onSelect = { outfit = outfit.updateWithSelection(it) }
                            )
                        }
                    }
                }
            }

            // Bottom buttons
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = { outfit = OutfitState(); tryOnResultBitmap = null },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Reset")
                }
                Button(
                    onClick = { scope.launch { snackbarHostState.showSnackbar("Outfit saved!") } },
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF10B981)),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Favorite, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save", color = Color.White)
                }
            }
        }
    }
}

@Composable
fun ClothingCardTryOn(item: ClothingItemTryOn, isSelected: Boolean, onSelect: (ClothingItemTryOn) -> Unit) {
    Column(
        modifier = Modifier
            .padding(4.dp)
            .border(if (isSelected) 3.dp else 1.dp, if (isSelected) Color(0xFF10B981) else Color(0xFFE5E7EB), RoundedCornerShape(12.dp))
            .clickable { onSelect(item) }
            .background(Color.White, RoundedCornerShape(12.dp))
    ) {
        val displayImage = item.thumbnailUri ?: item.processedImageUri ?: item.imageUri
        if (displayImage != null) {
            Image(
                painter = rememberAsyncImagePainter(displayImage),
                contentDescription = item.name,
                modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f).background(Color(0xFFF3F4F6)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.ShoppingBag, contentDescription = null, modifier = Modifier.size(32.dp), tint = Color.Gray)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
            Text(item.name, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
            if (item.brand.isNotEmpty()) Text(item.brand, fontSize = 10.sp, color = Color.Gray, maxLines = 1)
            if (item.colors.isNotEmpty()) Text(item.colors.joinToString(", "), fontSize = 10.sp, color = Color(0xFF10B981), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

// ========== HELPER FUNCTIONS ==========

// Load demo clothing items from assets
private suspend fun loadDemoClothingItems(context: android.content.Context): List<ClothingItemTryOn> {
    return listOf(
        ClothingItemTryOn(
            id = "demo_tshirt",
            name = "Demo T-Shirt",
            category = "Tops",
            colors = listOf("Blue"),
            brand = "Demo",
            imageUri = "asset://t-shirt.png",
            processedImageUri = "asset://t-shirt.png",
            worn = 0,
            isFavorite = false
        ),
        ClothingItemTryOn(
            id = "demo_jeans",
            name = "Demo Jeans",
            category = "Bottoms",
            colors = listOf("Denim"),
            brand = "Demo",
            imageUri = "asset://jeans.png",
            processedImageUri = "asset://jeans.png",
            worn = 0,
            isFavorite = false
        )
    )
}

// Load bitmap from assets
private suspend fun loadBitmapFromAssets(
    context: android.content.Context,
    fileName: String
): Bitmap? = withContext(Dispatchers.IO) {
    try {
        val inputStream = context.assets.open(fileName)
        val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
        inputStream.close()
        android.util.Log.d("TryOn", "✅ Loaded asset: $fileName")
        bitmap
    } catch (e: Exception) {
        android.util.Log.e("TryOn", "❌ Error loading asset: $fileName", e)
        null
    }
}

// Load bitmap from URI (Firebase or Assets)
private suspend fun loadBitmapFromUri(context: android.content.Context, uri: String): Bitmap? {
    // Check if it's an asset URL
    if (uri.startsWith("asset://")) {
        val fileName = uri.removePrefix("asset://")
        return loadBitmapFromAssets(context, fileName)
    }

    // Original Firebase/HTTP loading code
    return withContext(Dispatchers.IO) {
        try {
            val loader = ImageLoader(context)
            val request = ImageRequest.Builder(context).data(uri).allowHardware(false).build()
            val result = (loader.execute(request) as? SuccessResult)?.drawable
            (result as? android.graphics.drawable.BitmapDrawable)?.bitmap
        } catch (e: Exception) {
            android.util.Log.e("TryOn", "Error loading bitmap", e)
            null
        }
    }
}

// ========== ENHANCED BACKGROUND REMOVAL ==========

private fun removeBackgroundFromClothing(bitmap: Bitmap): Bitmap {
    val result = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
    val pixels = IntArray(bitmap.width * bitmap.height)
    bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

    val threshold = 220 // Brightness threshold for background

    for (i in pixels.indices) {
        val pixel = pixels[i]
        val r = AndroidColor.red(pixel)
        val g = AndroidColor.green(pixel)
        val b = AndroidColor.blue(pixel)

        // Calculate brightness
        val brightness = (r * 0.299 + g * 0.587 + b * 0.114).toInt()

        // If too bright (likely background), make transparent
        if (brightness > threshold) {
            pixels[i] = AndroidColor.TRANSPARENT
        } else if (brightness > threshold - 30) {
            // Soft edge transition
            val alpha = ((threshold - brightness) * 255 / 30).coerceIn(0, 255)
            pixels[i] = AndroidColor.argb(alpha, r, g, b)
        }
    }

    result.setPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
    return result
}

// Helper: Check content density
private fun checkContentDensity(bitmap: Bitmap, startY: Int, endY: Int): Int {
    var contentPixels = 0
    for (y in startY until endY) {
        for (x in 0 until bitmap.width) {
            val pixel = bitmap.getPixel(x, y)
            val alpha = AndroidColor.alpha(pixel)
            if (alpha > 50) contentPixels++
        }
    }
    return contentPixels
}

// ========== ENHANCED OVERLAY FUNCTIONS ==========

private fun enhancedOverlayTop(
    bodyBitmap: Bitmap,
    clothingBitmap: Bitmap,
    poseResult: com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
): Bitmap {
    val landmarks = poseResult.landmarks().firstOrNull() ?: return bodyBitmap
    val resultBitmap = bodyBitmap.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = Canvas(resultBitmap)

    val ls = landmarks[11]
    val rs = landmarks[12]
    val lh = landmarks[23]
    val rh = landmarks[24]

    val shoulderMidX = (ls.x() + rs.x()) / 2f * bodyBitmap.width
    val shoulderMidY = (ls.y() + rs.y()) / 2f * bodyBitmap.height
    val hipMidY = ((lh.y() + rh.y()) / 2f) * bodyBitmap.height

    val shoulderWidth = sqrt(((rs.x() - ls.x()) * bodyBitmap.width).pow(2) + ((rs.y() - ls.y()) * bodyBitmap.height).pow(2))
    val torsoHeight = hipMidY - shoulderMidY

    val scaleWidth = (shoulderWidth * 1.4f) / clothingBitmap.width
    val scaleHeight = (torsoHeight * 1.5f) / clothingBitmap.height

    val shoulderAngle = atan2((rs.y() - ls.y()) * bodyBitmap.height, (rs.x() - ls.x()) * bodyBitmap.width)

    val matrix = Matrix().apply {
        postScale(scaleWidth, scaleHeight, 0f, 0f)
        postRotate(Math.toDegrees(shoulderAngle.toDouble()).toFloat(), clothingBitmap.width * scaleWidth / 2f, clothingBitmap.height * scaleHeight / 2f)
        postTranslate(shoulderMidX - (clothingBitmap.width * scaleWidth / 2f), shoulderMidY - (clothingBitmap.height * scaleHeight * 0.1f))
    }

    val paint = Paint().apply {
        isAntiAlias = true
        isFilterBitmap = true
        isDither = true
    }

    canvas.drawBitmap(clothingBitmap, matrix, paint)
    return resultBitmap
}

private fun enhancedOverlayBottoms(
    bodyBitmap: Bitmap,
    clothingBitmap: Bitmap,
    poseResult: com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
): Bitmap {
    val landmarks = poseResult.landmarks().firstOrNull() ?: return bodyBitmap
    val resultBitmap = bodyBitmap.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = Canvas(resultBitmap)

    val lh = landmarks[23]
    val rh = landmarks[24]
    val la = landmarks[27]
    val ra = landmarks[28]

    val hipMidX = (lh.x() + rh.x()) / 2f * bodyBitmap.width
    val hipMidY = (lh.y() + rh.y()) / 2f * bodyBitmap.height

    val hipWidth = sqrt(((rh.x() - lh.x()) * bodyBitmap.width).pow(2) + ((rh.y() - lh.y()) * bodyBitmap.height).pow(2))
    val avgLegLength = (sqrt(((la.x() - lh.x()) * bodyBitmap.width).pow(2) + ((la.y() - lh.y()) * bodyBitmap.height).pow(2)) +
            sqrt(((ra.x() - rh.x()) * bodyBitmap.width).pow(2) + ((ra.y() - rh.y()) * bodyBitmap.height).pow(2))) / 2f

    val scaleWidth = (hipWidth * 1.3f) / clothingBitmap.width
    val scaleHeight = (avgLegLength * 1.2f) / clothingBitmap.height

    val matrix = Matrix().apply {
        postScale(scaleWidth, scaleHeight)
        postTranslate(hipMidX - (clothingBitmap.width * scaleWidth / 2f), hipMidY - (clothingBitmap.height * scaleHeight * 0.05f))
    }

    val paint = Paint().apply {
        isAntiAlias = true
        isFilterBitmap = true
        isDither = true
    }

    canvas.drawBitmap(clothingBitmap, matrix, paint)
    return resultBitmap
}

private fun enhancedOverlayDress(
    bodyBitmap: Bitmap,
    clothingBitmap: Bitmap,
    poseResult: com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
): Bitmap {
    val landmarks = poseResult.landmarks().firstOrNull() ?: return bodyBitmap
    val resultBitmap = bodyBitmap.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = Canvas(resultBitmap)

    val ls = landmarks[11]
    val rs = landmarks[12]
    val la = landmarks[27]
    val ra = landmarks[28]

    val shoulderMidX = (ls.x() + rs.x()) / 2f * bodyBitmap.width
    val shoulderMidY = (ls.y() + rs.y()) / 2f * bodyBitmap.height
    val ankleMidY = ((la.y() + ra.y()) / 2f) * bodyBitmap.height

    val shoulderWidth = sqrt(((rs.x() - ls.x()) * bodyBitmap.width).pow(2) + ((rs.y() - ls.y()) * bodyBitmap.height).pow(2))
    val dressHeight = ankleMidY - shoulderMidY

    val scaleWidth = (shoulderWidth * 1.35f) / clothingBitmap.width
    val scaleHeight = (dressHeight * 1.15f) / clothingBitmap.height

    val matrix = Matrix().apply {
        postScale(scaleWidth, scaleHeight)
        postTranslate(shoulderMidX - (clothingBitmap.width * scaleWidth / 2f), shoulderMidY - (clothingBitmap.height * scaleHeight * 0.05f))
    }

    val paint = Paint().apply {
        isAntiAlias = true
        isFilterBitmap = true
        isDither = true
    }

    canvas.drawBitmap(clothingBitmap, matrix, paint)
    return resultBitmap
}

private fun enhancedOverlayShoes(
    bodyBitmap: Bitmap,
    clothingBitmap: Bitmap,
    poseResult: com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
): Bitmap {
    val landmarks = poseResult.landmarks().firstOrNull() ?: return bodyBitmap
    val resultBitmap = bodyBitmap.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = Canvas(resultBitmap)

    val la = landmarks[27]
    val ra = landmarks[28]

    val ankleDistance = sqrt(((ra.x() - la.x()) * bodyBitmap.width).pow(2) + ((ra.y() - la.y()) * bodyBitmap.height).pow(2))
    val scale = (ankleDistance * 0.85f) / clothingBitmap.width

    val centerX = ((la.x() + ra.x()) / 2f) * bodyBitmap.width
    val centerY = ((la.y() + ra.y()) / 2f) * bodyBitmap.height + (clothingBitmap.height * scale * 0.35f)

    val matrix = Matrix().apply {
        postScale(scale, scale)
        postTranslate(centerX - (clothingBitmap.width * scale / 2f), centerY - (clothingBitmap.height * scale / 2f))
    }

    val paint = Paint().apply {
        isAntiAlias = true
        isFilterBitmap = true
        isDither = true
    }

    canvas.drawBitmap(clothingBitmap, matrix, paint)
    return resultBitmap
}

private fun enhancedOverlayOuterwear(
    bodyBitmap: Bitmap,
    clothingBitmap: Bitmap,
    poseResult: com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
): Bitmap {
    val landmarks = poseResult.landmarks().firstOrNull() ?: return bodyBitmap
    val resultBitmap = bodyBitmap.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = Canvas(resultBitmap)

    val ls = landmarks[11]
    val rs = landmarks[12]
    val lh = landmarks[23]
    val rh = landmarks[24]

    val shoulderMidX = (ls.x() + rs.x()) / 2f * bodyBitmap.width
    val shoulderMidY = (ls.y() + rs.y()) / 2f * bodyBitmap.height
    val hipMidY = ((lh.y() + rh.y()) / 2f) * bodyBitmap.height

    val shoulderWidth = sqrt(((rs.x() - ls.x()) * bodyBitmap.width).pow(2) + ((rs.y() - ls.y()) * bodyBitmap.height).pow(2))
    val jacketHeight = hipMidY - shoulderMidY

    val scaleWidth = (shoulderWidth * 1.55f) / clothingBitmap.width
    val scaleHeight = (jacketHeight * 1.65f) / clothingBitmap.height

    val matrix = Matrix().apply {
        postScale(scaleWidth, scaleHeight)
        postTranslate(shoulderMidX - (clothingBitmap.width * scaleWidth / 2f), shoulderMidY - (clothingBitmap.height * scaleHeight * 0.12f))
    }

    val paint = Paint().apply {
        isAntiAlias = true
        isFilterBitmap = true
        isDither = true
    }

    canvas.drawBitmap(clothingBitmap, matrix, paint)
    return resultBitmap
}
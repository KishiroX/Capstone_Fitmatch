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

enum class CategoryTryOn(val key: String, val label: String, val icon: String) {
    TOPS("Tops", "Tops", "👕"),
    BOTTOMS("Bottoms", "Bottoms", "👖"),
    DRESSES("Dresses", "Dresses", "👗"),
    OUTERWEAR("Outerwear", "Outerwear", "🧥"),
    SHOES("Shoes", "Shoes", "👟")
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

    // Load data
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

                clothingItems = if (firebaseItems.isEmpty()) {
                    android.util.Log.d("TryOn", "📦 Loading demo items")
                    loadDemoClothingItems()
                } else {
                    firebaseItems
                }

            } catch (e: Exception) {
                android.util.Log.e("TryOn", "Error loading data", e)
                clothingItems = loadDemoClothingItems()
                scope.launch {
                    snackbarHostState.showSnackbar("Using demo wardrobe")
                }
            } finally {
                isLoading = false
            }
        } else {
            clothingItems = loadDemoClothingItems()
            isLoading = false
        }
    }

    // ⭐ ENHANCED TRY-ON WITH MANUELA-STYLE IMPROVEMENTS
    LaunchedEffect(outfit, bodyScanUrl) {
        if (bodyScanUrl == null) {
            tryOnResultBitmap = null
            return@LaunchedEffect
        }

        val selectedItem = outfit.tops ?: outfit.dress ?: outfit.bottoms ?: outfit.outerwear ?: outfit.shoes
        if (selectedItem == null) {
            tryOnResultBitmap = null
            return@LaunchedEffect
        }

        isProcessingTryOn = true
        tryOnResultBitmap = null

        try {
            android.util.Log.d("TryOn", "🎯 Generating try-on for ${selectedItem.name}")

            val clothingImageUri = selectedItem.processedImageUri ?: selectedItem.imageUri
            if (clothingImageUri == null) {
                android.util.Log.e("TryOn", "No image URI")
                isProcessingTryOn = false
                return@LaunchedEffect
            }

            val bodyScanBitmap = loadBitmapFromUri(context, bodyScanUrl!!)
            var clothingBitmap = loadBitmapFromUri(context, clothingImageUri)

            if (bodyScanBitmap == null || clothingBitmap == null) {
                android.util.Log.e("TryOn", "Failed to load images")
                isProcessingTryOn = false
                return@LaunchedEffect
            }

            val poseResult = withContext(Dispatchers.Default) {
                bodyAnalyzer.analyze(bodyScanBitmap)
            }

            if (poseResult == null) {
                android.util.Log.e("TryOn", "❌ No pose detected")
                scope.launch {
                    snackbarHostState.showSnackbar("Could not detect pose")
                }
                isProcessingTryOn = false
                return@LaunchedEffect
            }

            android.util.Log.d("TryOn", "✅ Pose detected! Processing clothing...")

            // ⭐ MANUELA-STYLE PROCESSING PIPELINE
            clothingBitmap = withContext(Dispatchers.Default) {
                var processed = clothingBitmap!!

                // 1. Remove background
                if (!hasTransparentBackground(processed)) {
                    android.util.Log.d("TryOn", "🔧 Removing background...")
                    processed = removeBackgroundFromClothing(processed)
                }

                // 2. Fix orientation
                val rotationAngle = detectCorrectOrientation(processed, selectedItem.category)
                if (rotationAngle != 0f) {
                    android.util.Log.d("TryOn", "🔄 Auto-rotating ${rotationAngle}°")
                    val rotateMatrix = Matrix().apply {
                        postRotate(rotationAngle, processed.width / 2f, processed.height / 2f)
                    }
                    processed = Bitmap.createBitmap(processed, 0, 0, processed.width, processed.height, rotateMatrix, true)
                }

                // 3. Feather edges
                android.util.Log.d("TryOn", "✨ Feathering edges...")
                processed = featherClothingEdges(processed, 3)

                // 4. ⭐ ADD DEPTH SHADING (MANUELA-STYLE!)
                android.util.Log.d("TryOn", "🎨 Adding depth shading...")
                processed = addSimpleDepth(processed)

                // 5. Adjust lighting
                android.util.Log.d("TryOn", "💡 Adjusting lighting...")
                val bodyRegion = android.graphics.Rect(
                    (bodyScanBitmap.width * 0.25f).toInt(),
                    (bodyScanBitmap.height * 0.3f).toInt(),
                    (bodyScanBitmap.width * 0.75f).toInt(),
                    (bodyScanBitmap.height * 0.7f).toInt()
                )
                processed = adjustClothingLighting(processed, bodyScanBitmap, bodyRegion)

                processed
            }

            // Apply overlay with improved scaling
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
            android.util.Log.e("TryOn", "❌ Error: ${e.message}", e)
            scope.launch {
                snackbarHostState.showSnackbar("Try-on failed")
            }
        } finally {
            isProcessingTryOn = false
        }
    }

    val categories = CategoryTryOn.values().toList()

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
                    IconButton(
                        onClick = {
                            scope.launch {
                                snackbarHostState.showSnackbar("Outfit saved!")
                            }
                        },
                        enabled = tryOnResultBitmap != null
                    ) {
                        Icon(
                            Icons.Default.Favorite,
                            contentDescription = "Save",
                            tint = if (tryOnResultBitmap != null) Color.White else Color.White.copy(alpha = 0.5f)
                        )
                    }
                    IconButton(onClick = {
                        scope.launch {
                            snackbarHostState.showSnackbar("Share coming soon!")
                        }
                    }) {
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
                    .height(380.dp)
                    .background(Color(0xFFE5E7EB)),
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
                            Text("🎯 AI Processing...", textAlign = TextAlign.Center, color = Color.Gray, fontWeight = FontWeight.Medium)
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
                            modifier = Modifier.align(Alignment.TopStart).padding(12.dp),
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF10B981).copy(alpha = 0.9f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(5.dp))
                                Text("AI Enhanced", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        if (outfit.tops != null || outfit.dress != null || outfit.bottoms != null || outfit.shoes != null || outfit.outerwear != null) {
                            Column(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(12.dp)
                                    .background(Color.White.copy(alpha = 0.95f), RoundedCornerShape(10.dp))
                                    .padding(10.dp)
                            ) {
                                outfit.dress?.let { Text("👗", fontSize = 16.sp) }
                                outfit.tops?.let { Text("👕", fontSize = 16.sp) }
                                outfit.bottoms?.let { Text("👖", fontSize = 16.sp) }
                                outfit.outerwear?.let { Text("🧥", fontSize = 16.sp) }
                                outfit.shoes?.let { Text("👟", fontSize = 16.sp) }
                            }
                        }
                    }
                    bodyScanUrl == null -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.Gray)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Complete your body scan first", textAlign = TextAlign.Center, color = Color.Gray, fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { onNavigate("scan") },
                                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF10B981))
                            ) {
                                Text("Start Scan", color = Color.White)
                            }
                        }
                    }
                    else -> {
                        Image(
                            painter = rememberAsyncImagePainter(bodyScanUrl),
                            contentDescription = "Body Scan",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            }

            // Categories
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { category ->
                    Button(
                        onClick = { activeCategory = category },
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = if (activeCategory == category) Color(0xFF10B981) else Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        elevation = ButtonDefaults.elevation(
                            defaultElevation = if (activeCategory == category) 4.dp else 2.dp
                        ),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Text(
                            "${category.icon} ${category.label}",
                            color = if (activeCategory == category) Color.White else Color.Black,
                            fontWeight = if (activeCategory == category) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            Divider(color = Color(0xFFE5E7EB), thickness = 1.dp)

            // Clothing items
            if (isLoading) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF10B981))
                }
            } else {
                val filteredItems = clothingItems.filter { it.category == activeCategory.key }
                if (filteredItems.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                            Icon(Icons.Default.ShoppingBag, contentDescription = null, modifier = Modifier.size(48.dp), tint = Color.Gray)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("No ${activeCategory.label}", color = Color.Gray, fontWeight = FontWeight.Medium, fontSize = 16.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Add items to your wardrobe", fontSize = 13.sp, color = Color.Gray)
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { onNavigate("wardrobe") },
                                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF10B981))
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add Items", color = Color.White)
                            }
                        }
                    }
                } else {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
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
            Surface(
                modifier = Modifier.fillMaxWidth(),
                elevation = 8.dp,
                color = Color.White
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            outfit = OutfitState()
                            tryOnResultBitmap = null
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Reset")
                    }
                    Button(
                        onClick = {
                            scope.launch {
                                snackbarHostState.showSnackbar("Outfit saved!")
                            }
                        },
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF10B981)),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        enabled = tryOnResultBitmap != null
                    ) {
                        Icon(Icons.Default.Favorite, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save Outfit", color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun ClothingCardTryOn(item: ClothingItemTryOn, isSelected: Boolean, onSelect: (ClothingItemTryOn) -> Unit) {
    Column(
        modifier = Modifier
            .width(160.dp)
            .border(
                if (isSelected) 3.dp else 1.dp,
                if (isSelected) Color(0xFF10B981) else Color(0xFFE5E7EB),
                RoundedCornerShape(16.dp)
            )
            .clickable { onSelect(item) }
            .background(Color.White, RoundedCornerShape(16.dp))
    ) {
        val displayImage = item.thumbnailUri ?: item.processedImageUri ?: item.imageUri
        if (displayImage != null) {
            Image(
                painter = rememberAsyncImagePainter(displayImage),
                contentDescription = item.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .background(Color(0xFFF3F4F6), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.ShoppingBag, contentDescription = null, modifier = Modifier.size(40.dp), tint = Color.Gray)
            }
        }

        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                item.name,
                fontSize = 14.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 18.sp
            )
            if (item.brand.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(item.brand, fontSize = 11.sp, color = Color.Gray, maxLines = 1)
            }
            if (item.colors.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Palette, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color(0xFF10B981))
                    Text(
                        item.colors.take(2).joinToString(", "),
                        fontSize = 11.sp,
                        color = Color(0xFF10B981),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// ========== HELPER FUNCTIONS ==========

private fun loadDemoClothingItems(): List<ClothingItemTryOn> {
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

private suspend fun loadBitmapFromAssets(
    context: android.content.Context,
    fileName: String
): Bitmap? = withContext(Dispatchers.IO) {
    try {
        val inputStream = context.assets.open(fileName)
        val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
        inputStream.close()
        bitmap
    } catch (e: Exception) {
        android.util.Log.e("TryOn", "Error loading asset: $fileName", e)
        null
    }
}

private suspend fun loadBitmapFromUri(context: android.content.Context, uri: String): Bitmap? {
    if (uri.startsWith("asset://")) {
        val fileName = uri.removePrefix("asset://")
        return loadBitmapFromAssets(context, fileName)
    }

    return withContext(Dispatchers.IO) {
        try {
            val loader = ImageLoader(context)
            val request = ImageRequest.Builder(context)
                .data(uri)
                .allowHardware(false)
                .build()
            val result = (loader.execute(request) as? SuccessResult)?.drawable
            (result as? android.graphics.drawable.BitmapDrawable)?.bitmap
        } catch (e: Exception) {
            android.util.Log.e("TryOn", "Error loading bitmap from $uri", e)
            null
        }
    }
}

// ========== BACKGROUND REMOVAL ==========

private fun removeBackgroundFromClothing(bitmap: Bitmap): Bitmap {
    val result = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
    val pixels = IntArray(bitmap.width * bitmap.height)
    bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

    val threshold = 235

    for (i in pixels.indices) {
        val pixel = pixels[i]
        val r = AndroidColor.red(pixel)
        val g = AndroidColor.green(pixel)
        val b = AndroidColor.blue(pixel)

        val brightness = (r * 0.299 + g * 0.587 + b * 0.114).toInt()
        val maxChannel = maxOf(r, g, b)
        val minChannel = minOf(r, g, b)
        val saturation = if (maxChannel != 0) {
            ((maxChannel - minChannel).toFloat() / maxChannel) * 100
        } else 0f

        if (brightness > threshold && saturation < 15) {
            pixels[i] = AndroidColor.TRANSPARENT
        } else if (brightness > threshold - 40 && saturation < 20) {
            val alpha = ((threshold - brightness) * 255 / 40).coerceIn(0, 255)
            pixels[i] = AndroidColor.argb(alpha, r, g, b)
        }
    }

    result.setPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
    return result
}

private fun hasTransparentBackground(bitmap: Bitmap): Boolean {
    var transparentPixels = 0
    val sampleSize = 10

    for (y in 0 until bitmap.height step sampleSize) {
        for (x in 0 until bitmap.width step sampleSize) {
            if (AndroidColor.alpha(bitmap.getPixel(x, y)) < 50) {
                transparentPixels++
            }
        }
    }

    val sampledPixels = (bitmap.width / sampleSize) * (bitmap.height / sampleSize)
    val transparentPercentage = (transparentPixels.toFloat() / sampledPixels) * 100

    return transparentPercentage > 20
}

// ========== ORIENTATION DETECTION ==========

private fun detectCorrectOrientation(bitmap: Bitmap, category: String): Float {
    val width = bitmap.width
    val height = bitmap.height
    val sampleStep = 4

    val topThird = countContentInRegion(bitmap, 0, 0, width, height / 3)
    val middleThird = countContentInRegion(bitmap, 0, height / 3, width, (height * 2) / 3)
    val bottomThird = countContentInRegion(bitmap, 0, (height * 2) / 3, width, height)

    val topQuarter = countContentInRegion(bitmap, 0, 0, width, height / 4)
    val bottomQuarter = countContentInRegion(bitmap, 0, (height * 3) / 4, width, height)

    var totalMass = 0
    var weightedYSum = 0

    for (y in 0 until height step sampleStep) {
        for (x in 0 until width step sampleStep) {
            val pixel = bitmap.getPixel(x, y)
            val alpha = AndroidColor.alpha(pixel)
            if (alpha > 50) {
                totalMass++
                weightedYSum += y
            }
        }
    }

    val centerOfMassY = if (totalMass > 0) {
        (weightedYSum.toFloat() / totalMass) / height
    } else {
        0.5f
    }

    android.util.Log.d("TryOn", "=== Orientation: $category ===")
    android.util.Log.d("TryOn", "Top: $topThird | Mid: $middleThird | Bot: $bottomThird")
    android.util.Log.d("TryOn", "Center of mass: ${(centerOfMassY * 100).toInt()}%")

    val needsRotation = when (category) {
        "Tops", "Outerwear" -> {
            val isUpsideDown = centerOfMassY > 0.58f ||
                    (bottomThird > topThird * 1.4f) ||
                    (bottomQuarter > topQuarter * 2.5f)

            android.util.Log.d("TryOn", if (isUpsideDown) "❌ UPSIDE DOWN" else "✅ Correct")
            isUpsideDown
        }

        "Bottoms" -> {
            val isUpsideDown = centerOfMassY < 0.42f ||
                    (topThird > bottomThird * 1.4f) ||
                    (topQuarter > bottomQuarter * 2.5f)

            android.util.Log.d("TryOn", if (isUpsideDown) "❌ UPSIDE DOWN" else "✅ Correct")
            isUpsideDown
        }

        "Dresses" -> {
            val isUpsideDown = centerOfMassY < 0.35f ||
                    (topThird > bottomThird * 1.6f)

            android.util.Log.d("TryOn", if (isUpsideDown) "❌ UPSIDE DOWN" else "✅ Correct")
            isUpsideDown
        }

        "Shoes" -> {
            val isUpsideDown = centerOfMassY > 0.65f ||
                    (bottomQuarter > topQuarter * 2.0f)

            android.util.Log.d("TryOn", if (isUpsideDown) "❌ UPSIDE DOWN" else "✅ Correct")
            isUpsideDown
        }

        else -> false
    }

    return if (needsRotation) 180f else 0f
}

private fun countContentInRegion(bitmap: Bitmap, startX: Int, startY: Int, endX: Int, endY: Int): Int {
    var contentPixels = 0
    val sampleStep = 3

    for (y in startY until endY step sampleStep) {
        for (x in startX until endX step sampleStep) {
            if (x < bitmap.width && y < bitmap.height) {
                val alpha = AndroidColor.alpha(bitmap.getPixel(x, y))
                if (alpha > 50) {
                    contentPixels++
                }
            }
        }
    }

    return contentPixels
}

// ========== EDGE FEATHERING ==========

private fun featherClothingEdges(bitmap: Bitmap, featherRadius: Int = 3): Bitmap {
    val result = bitmap.copy(Bitmap.Config.ARGB_8888, true)
    val width = result.width
    val height = result.height

    val isEdge = Array(height) { BooleanArray(width) }

    for (y in 0 until height) {
        for (x in 0 until width) {
            val alpha = AndroidColor.alpha(result.getPixel(x, y))

            if (alpha > 50) {
                var nearTransparent = false
                for (dy in -featherRadius..featherRadius) {
                    for (dx in -featherRadius..featherRadius) {
                        val nx = (x + dx).coerceIn(0, width - 1)
                        val ny = (y + dy).coerceIn(0, height - 1)
                        if (AndroidColor.alpha(result.getPixel(nx, ny)) < 50) {
                            nearTransparent = true
                            break
                        }
                    }
                    if (nearTransparent) break
                }
                isEdge[y][x] = nearTransparent
            }
        }
    }

    for (y in 0 until height) {
        for (x in 0 until width) {
            if (isEdge[y][x]) {
                val pixel = result.getPixel(x, y)
                val alpha = AndroidColor.alpha(pixel)
                val newAlpha = (alpha * 0.7f).toInt().coerceIn(0, 255)
                result.setPixel(x, y, AndroidColor.argb(
                    newAlpha,
                    AndroidColor.red(pixel),
                    AndroidColor.green(pixel),
                    AndroidColor.blue(pixel)
                ))
            }
        }
    }

    return result
}

// ========== ⭐ MANUELA-STYLE DEPTH SHADING ==========

private fun addSimpleDepth(clothingBitmap: Bitmap): Bitmap {
    val result = clothingBitmap.copy(Bitmap.Config.ARGB_8888, true)
    val width = clothingBitmap.width
    val height = clothingBitmap.height
    val centerX = width / 2f

    for (y in 0 until height) {
        for (x in 0 until width) {
            val pixel = result.getPixel(x, y)
            val alpha = AndroidColor.alpha(pixel)

            if (alpha > 50) {
                // Calculate distance from center (0 to 1)
                val distFromCenter = abs(x - centerX) / centerX

                // Darken edges to simulate body curvature (Manuela-style!)
                val shadowAmount = distFromCenter * 0.25f
                val brightness = 1f - shadowAmount

                val r = (AndroidColor.red(pixel) * brightness).toInt().coerceIn(0, 255)
                val g = (AndroidColor.green(pixel) * brightness).toInt().coerceIn(0, 255)
                val b = (AndroidColor.blue(pixel) * brightness).toInt().coerceIn(0, 255)

                result.setPixel(x, y, AndroidColor.argb(alpha, r, g, b))
            }
        }
    }

    return result
}

// ========== LIGHTING ADJUSTMENT ==========

private fun adjustClothingLighting(
    clothingBitmap: Bitmap,
    bodyBitmap: Bitmap,
    bodyRegion: android.graphics.Rect
): Bitmap {
    var bodyBrightness = 0f
    var sampleCount = 0
    val sampleStep = 10

    for (y in bodyRegion.top until bodyRegion.bottom step sampleStep) {
        for (x in bodyRegion.left until bodyRegion.right step sampleStep) {
            if (x < bodyBitmap.width && y < bodyBitmap.height) {
                val pixel = bodyBitmap.getPixel(x, y)
                val r = AndroidColor.red(pixel)
                val g = AndroidColor.green(pixel)
                val b = AndroidColor.blue(pixel)
                bodyBrightness += (r + g + b) / 3f
                sampleCount++
            }
        }
    }

    if (sampleCount == 0) return clothingBitmap

    bodyBrightness /= sampleCount

    var clothingBrightness = 0f
    var clothingSamples = 0

    for (y in 0 until clothingBitmap.height step sampleStep) {
        for (x in 0 until clothingBitmap.width step sampleStep) {
            val pixel = clothingBitmap.getPixel(x, y)
            val alpha = AndroidColor.alpha(pixel)
            if (alpha > 50) {
                val r = AndroidColor.red(pixel)
                val g = AndroidColor.green(pixel)
                val b = AndroidColor.blue(pixel)
                clothingBrightness += (r + g + b) / 3f
                clothingSamples++
            }
        }
    }

    if (clothingSamples == 0) return clothingBitmap

    clothingBrightness /= clothingSamples

    val adjustFactor = (bodyBrightness / clothingBrightness).coerceIn(0.7f, 1.3f)

    val result = Bitmap.createBitmap(clothingBitmap.width, clothingBitmap.height, Bitmap.Config.ARGB_8888)

    for (y in 0 until clothingBitmap.height) {
        for (x in 0 until clothingBitmap.width) {
            val pixel = clothingBitmap.getPixel(x, y)
            val alpha = AndroidColor.alpha(pixel)

            if (alpha > 0) {
                val r = (AndroidColor.red(pixel) * adjustFactor).toInt().coerceIn(0, 255)
                val g = (AndroidColor.green(pixel) * adjustFactor).toInt().coerceIn(0, 255)
                val b = (AndroidColor.blue(pixel) * adjustFactor).toInt().coerceIn(0, 255)
                result.setPixel(x, y, AndroidColor.argb(alpha, r, g, b))
            }
        }
    }

    return result
}

// ========== ⭐ ENHANCED OVERLAY FUNCTIONS WITH BETTER SCALING ==========

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

    val shoulderWidth = sqrt(
        ((rs.x() - ls.x()) * bodyBitmap.width).pow(2) +
                ((rs.y() - ls.y()) * bodyBitmap.height).pow(2)
    )
    val torsoHeight = hipMidY - shoulderMidY

    // ⭐ IMPROVED SCALING for better coverage
    val scaleWidth = (shoulderWidth * 2.1f) / clothingBitmap.width
    val scaleHeight = (torsoHeight * 1.35f) / clothingBitmap.height

    val shoulderAngle = atan2(
        (rs.y() - ls.y()) * bodyBitmap.height,
        (rs.x() - ls.x()) * bodyBitmap.width
    ) * (180f / Math.PI.toFloat())

    val matrix = Matrix().apply {
        postScale(scaleWidth, scaleHeight)
        postRotate(
            shoulderAngle,
            clothingBitmap.width * scaleWidth / 2f,
            clothingBitmap.height * scaleHeight / 2f
        )
        postTranslate(
            shoulderMidX - (clothingBitmap.width * scaleWidth / 2f),
            shoulderMidY - (clothingBitmap.height * scaleHeight * 0.05f)
        )
    }

    val paint = Paint().apply {
        isAntiAlias = true
        isFilterBitmap = true
        isDither = true
        alpha = 250
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

    val hipWidth = sqrt(
        ((rh.x() - lh.x()) * bodyBitmap.width).pow(2) +
                ((rh.y() - lh.y()) * bodyBitmap.height).pow(2)
    )

    val leftLegLength = sqrt(
        ((la.x() - lh.x()) * bodyBitmap.width).pow(2) +
                ((la.y() - lh.y()) * bodyBitmap.height).pow(2)
    )
    val rightLegLength = sqrt(
        ((ra.x() - rh.x()) * bodyBitmap.width).pow(2) +
                ((ra.y() - rh.y()) * bodyBitmap.height).pow(2)
    )
    val avgLegLength = (leftLegLength + rightLegLength) / 2f

    // ⭐ IMPROVED SCALING
    val scaleWidth = (hipWidth * 1.85f) / clothingBitmap.width
    val scaleHeight = (avgLegLength * 1.08f) / clothingBitmap.height

    val matrix = Matrix().apply {
        postScale(scaleWidth, scaleHeight)
        postTranslate(
            hipMidX - (clothingBitmap.width * scaleWidth / 2f),
            hipMidY - (clothingBitmap.height * scaleHeight * 0.02f)
        )
    }

    val paint = Paint().apply {
        isAntiAlias = true
        isFilterBitmap = true
        isDither = true
        alpha = 250
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

    val shoulderWidth = sqrt(
        ((rs.x() - ls.x()) * bodyBitmap.width).pow(2) +
                ((rs.y() - ls.y()) * bodyBitmap.height).pow(2)
    )
    val dressHeight = ankleMidY - shoulderMidY

    val scaleWidth = (shoulderWidth * 1.6f) / clothingBitmap.width
    val scaleHeight = (dressHeight * 1.08f) / clothingBitmap.height

    val matrix = Matrix().apply {
        postScale(scaleWidth, scaleHeight)
        postTranslate(
            shoulderMidX - (clothingBitmap.width * scaleWidth / 2f),
            shoulderMidY - (clothingBitmap.height * scaleHeight * 0.08f)
        )
    }

    val paint = Paint().apply {
        isAntiAlias = true
        isFilterBitmap = true
        isDither = true
        alpha = 250
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
    val lf = landmarks[31]
    val rf = landmarks[32]

    val ankleDistance = sqrt(
        ((ra.x() - la.x()) * bodyBitmap.width).pow(2) +
                ((ra.y() - la.y()) * bodyBitmap.height).pow(2)
    )

    val scale = (ankleDistance * 0.9f) / clothingBitmap.width

    val centerX = ((la.x() + ra.x()) / 2f) * bodyBitmap.width
    val footY = ((lf.y() + rf.y()) / 2f) * bodyBitmap.height
    val centerY = footY + (clothingBitmap.height * scale * 0.15f)

    val matrix = Matrix().apply {
        postScale(scale, scale)
        postTranslate(
            centerX - (clothingBitmap.width * scale / 2f),
            centerY - (clothingBitmap.height * scale / 2f)
        )
    }

    val paint = Paint().apply {
        isAntiAlias = true
        isFilterBitmap = true
        isDither = true
        alpha = 250
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

    val shoulderWidth = sqrt(
        ((rs.x() - ls.x()) * bodyBitmap.width).pow(2) +
                ((rs.y() - ls.y()) * bodyBitmap.height).pow(2)
    )
    val jacketHeight = hipMidY - shoulderMidY

    val scaleWidth = (shoulderWidth * 2.2f) / clothingBitmap.width
    val scaleHeight = (jacketHeight * 1.5f) / clothingBitmap.height

    val matrix = Matrix().apply {
        postScale(scaleWidth, scaleHeight)
        postTranslate(
            shoulderMidX - (clothingBitmap.width * scaleWidth / 2f),
            shoulderMidY - (clothingBitmap.height * scaleHeight * 0.12f)
        )
    }

    val paint = Paint().apply {
        isAntiAlias = true
        isFilterBitmap = true
        isDither = true
        alpha = 250
    }

    canvas.drawBitmap(clothingBitmap, matrix, paint)
    return resultBitmap
}
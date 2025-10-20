package com.example.capstone.ui.screen

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.exifinterface.media.ExifInterface
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.math.min

/**
 * Clothing type enum for adaptive guide frames
 */
enum class ClothingType(
    val displayName: String,
    val icon: String,
    val widthRatio: Float,
    val heightRatio: Float,
    val verticalOffset: Float
) {
    SHIRT("Shirt", "👕", 0.75f, 0.55f, -0.05f),
    PANTS("Pants", "👖", 0.60f, 0.70f, 0.05f),
    DRESS("Dress", "👗", 0.65f, 0.75f, 0.0f),
    SHOES("Shoes", "👟", 0.70f, 0.40f, 0.15f),
    JACKET("Jacket", "🧥", 0.75f, 0.60f, -0.05f),
    ACCESSORIES("Accessories", "🎒", 0.65f, 0.50f, 0.0f)
}

/**
 * Camera screen with adaptive guide frames based on clothing type
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraClothingScreen(
    onBack: () -> Unit,
    onImageCaptured: (File) -> Unit
) {
    var showGuidelines by remember { mutableStateOf(true) }
    var selectedClothingType by remember { mutableStateOf(ClothingType.SHIRT) }
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
    }

    when {
        showGuidelines -> {
            ClothingPhotoGuidelinesScreen(
                onContinue = { type ->
                    selectedClothingType = type
                    showGuidelines = false
                },
                onBack = onBack
            )
        }
        cameraPermissionState.status.isGranted -> {
            CameraContent(
                clothingType = selectedClothingType,
                onBack = onBack,
                onImageCaptured = onImageCaptured
            )
        }
        else -> {
            CameraPermissionDenied(onBack = onBack)
        }
    }
}

/**
 * Guidelines screen with clothing type selector
 */
@Composable
private fun ClothingPhotoGuidelinesScreen(
    onContinue: (ClothingType) -> Unit,
    onBack: () -> Unit
) {
    var selectedType by remember { mutableStateOf(ClothingType.SHIRT) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Top Bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFF10B981),
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, "Back", tint = Color.White)
                }
                Text(
                    "Photo Guidelines",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .background(Color(0xFFF9FAFB))
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFD1FAE5)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = Color(0xFF059669)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Select Clothing Type",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF065F46),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "The camera guide will adapt to your selection",
                        fontSize = 14.sp,
                        color = Color(0xFF047857),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Clothing Type Selector
            Text(
                "Choose what you're photographing:",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF059669)
            )

            // First row of types
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ClothingTypeButton(
                    type = ClothingType.SHIRT,
                    isSelected = selectedType == ClothingType.SHIRT,
                    onClick = { selectedType = ClothingType.SHIRT },
                    modifier = Modifier.weight(1f)
                )
                ClothingTypeButton(
                    type = ClothingType.PANTS,
                    isSelected = selectedType == ClothingType.PANTS,
                    onClick = { selectedType = ClothingType.PANTS },
                    modifier = Modifier.weight(1f)
                )
                ClothingTypeButton(
                    type = ClothingType.DRESS,
                    isSelected = selectedType == ClothingType.DRESS,
                    onClick = { selectedType = ClothingType.DRESS },
                    modifier = Modifier.weight(1f)
                )
            }

            // Second row of types
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ClothingTypeButton(
                    type = ClothingType.SHOES,
                    isSelected = selectedType == ClothingType.SHOES,
                    onClick = { selectedType = ClothingType.SHOES },
                    modifier = Modifier.weight(1f)
                )
                ClothingTypeButton(
                    type = ClothingType.JACKET,
                    isSelected = selectedType == ClothingType.JACKET,
                    onClick = { selectedType = ClothingType.JACKET },
                    modifier = Modifier.weight(1f)
                )
                ClothingTypeButton(
                    type = ClothingType.ACCESSORIES,
                    isSelected = selectedType == ClothingType.ACCESSORIES,
                    onClick = { selectedType = ClothingType.ACCESSORIES },
                    modifier = Modifier.weight(1f)
                )
            }

            Text(
                "Camera guide will be optimized for ${selectedType.displayName.lowercase()}",
                fontSize = 13.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            // Quick Tips
            Text(
                "📸 Quick Tips:",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF059669)
            )

            GuidelineCard(
                icon = Icons.Default.CheckCircle,
                title = "Lay Flat or Hang",
                description = "Shows the full shape clearly",
                isPositive = true
            )

            GuidelineCard(
                icon = Icons.Default.CheckCircle,
                title = "Plain Background",
                description = "Use solid colors, avoid patterns",
                isPositive = true
            )

            GuidelineCard(
                icon = Icons.Default.CheckCircle,
                title = "Good Lighting",
                description = "Natural daylight works best",
                isPositive = true
            )

            // Continue Button
            Button(
                onClick = { onContinue(selectedType) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF10B981)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(selectedType.icon, fontSize = 20.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Take ${selectedType.displayName} Photo",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(imageVector = Icons.Default.ArrowForward, contentDescription = null)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ClothingTypeButton(
    type: ClothingType,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(85.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) Color(0xFF10B981) else Color.White,
            contentColor = if (isSelected) Color.White else Color(0xFF10B981)
        ),
        shape = RoundedCornerShape(12.dp),
        border = if (!isSelected) BorderStroke(2.dp, Color(0xFF10B981)) else null
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(type.icon, fontSize = 28.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                type.displayName,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun GuidelineCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    isPositive: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isPositive) Color.White else Color(0xFFFEE2E2)
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isPositive) Color(0xFF059669) else Color(0xFFDC2626),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isPositive) Color(0xFF065F46) else Color(0xFF991B1B)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    description,
                    fontSize = 13.sp,
                    color = if (isPositive) Color(0xFF6B7280) else Color(0xFF7F1D1D),
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
private fun CameraContent(
    clothingType: ClothingType,
    onBack: () -> Unit,
    onImageCaptured: (File) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_BACK) }
    var flashMode by remember { mutableStateOf(ImageCapture.FLASH_MODE_OFF) }
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    var isCapturing by remember { mutableStateOf(false) }

    val previewView = remember { PreviewView(context) }

    LaunchedEffect(lensFacing, flashMode) {
        val cameraProvider = context.getCameraProvider()
        cameraProvider.unbindAll()

        val preview = Preview.Builder().build().apply {
            setSurfaceProvider(previewView.surfaceProvider)
        }

        imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .setFlashMode(flashMode)
            .build()

        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(lensFacing)
            .build()

        try {
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageCapture
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )

        SmartGuideOverlay(clothingType = clothingType)

        TopCameraControls(
            onBack = onBack,
            flashMode = flashMode,
            onFlashToggle = {
                flashMode = when (flashMode) {
                    ImageCapture.FLASH_MODE_OFF -> ImageCapture.FLASH_MODE_ON
                    ImageCapture.FLASH_MODE_ON -> ImageCapture.FLASH_MODE_AUTO
                    else -> ImageCapture.FLASH_MODE_OFF
                }
            }
        )

        BottomCameraControls(
            isCapturing = isCapturing,
            onCapture = {
                isCapturing = true
                imageCapture?.let { capture ->
                    takePictureWithCrop(context, capture, clothingType) { file ->
                        isCapturing = false
                        file?.let(onImageCaptured)
                    }
                }
            },
            onFlipCamera = {
                lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK)
                    CameraSelector.LENS_FACING_FRONT
                else
                    CameraSelector.LENS_FACING_BACK
            },
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        if (isCapturing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(48.dp))
            }
        }
    }
}

@Composable
private fun SmartGuideOverlay(clothingType: ClothingType) {
    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val rectWidth = size.width * clothingType.widthRatio
            val rectHeight = size.height * clothingType.heightRatio
            val rectLeft = (size.width - rectWidth) / 2
            val centerTop = (size.height - rectHeight) / 2
            val rectTop = centerTop + (size.height * clothingType.verticalOffset)

            drawRect(color = Color.Black.copy(alpha = 0.5f), size = size)

            drawRoundRect(
                color = Color.Transparent,
                topLeft = Offset(rectLeft, rectTop),
                size = Size(rectWidth, rectHeight),
                cornerRadius = CornerRadius(24f),
                blendMode = androidx.compose.ui.graphics.BlendMode.Clear
            )

            drawRoundRect(
                color = Color(0xFF10B981),
                topLeft = Offset(rectLeft, rectTop),
                size = Size(rectWidth, rectHeight),
                cornerRadius = CornerRadius(24f),
                style = Stroke(
                    width = 6f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(30f, 15f))
                )
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 60.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color.Black.copy(alpha = 0.75f)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "${clothingType.icon} Position ${clothingType.displayName.lowercase()} within frame",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Flat lay • Good lighting • Plain background",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun TopCameraControls(
    onBack: () -> Unit,
    flashMode: Int,
    onFlashToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)
        ) {
            Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
        }

        IconButton(
            onClick = onFlashToggle,
            modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)
        ) {
            val flashIcon = when (flashMode) {
                ImageCapture.FLASH_MODE_ON -> Icons.Default.FlashOn
                ImageCapture.FLASH_MODE_AUTO -> Icons.Default.FlashAuto
                else -> Icons.Default.FlashOff
            }
            Icon(imageVector = flashIcon, contentDescription = "Flash", tint = Color.White)
        }
    }
}

@Composable
private fun BottomCameraControls(
    isCapturing: Boolean,
    onCapture: () -> Unit,
    onFlipCamera: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 40.dp, start = 24.dp, end = 24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(modifier = Modifier.size(56.dp))

            Button(
                onClick = onCapture,
                enabled = !isCapturing,
                modifier = Modifier
                    .size(72.dp)
                    .border(4.dp, Color.White, CircleShape),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White.copy(alpha = 0.9f)
                ),
                shape = CircleShape,
                contentPadding = PaddingValues(0.dp)
            ) {
                if (isCapturing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        color = Color(0xFF10B981),
                        strokeWidth = 3.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Capture",
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            IconButton(
                onClick = onFlipCamera,
                enabled = !isCapturing,
                modifier = Modifier
                    .size(56.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.FlipCameraAndroid,
                    contentDescription = "Flip",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
private fun CameraPermissionDenied(onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF9FAFB)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = Color.Gray
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Camera Permission Required",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Please grant camera permission to capture clothing photos",
                textAlign = TextAlign.Center,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onBack) {
                Text("Go Back")
            }
        }
    }
}

private suspend fun Context.getCameraProvider(): ProcessCameraProvider =
    suspendCoroutine { continuation ->
        ProcessCameraProvider.getInstance(this).also { future ->
            future.addListener({
                continuation.resume(future.get())
            }, ContextCompat.getMainExecutor(this))
        }
    }

/**
 * 🎯 NEW: Takes picture and auto-crops to guide frame
 */
private fun takePictureWithCrop(
    context: Context,
    imageCapture: ImageCapture,
    clothingType: ClothingType,
    onImageCaptured: (File?) -> Unit
) {
    val photoFile = File(
        context.cacheDir,
        SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)
            .format(System.currentTimeMillis()) + ".jpg"
    )

    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

    imageCapture.takePicture(
        outputOptions,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                // Crop the image to match guide frame
                val croppedFile = cropImageToGuideFrame(photoFile, clothingType, context)
                onImageCaptured(croppedFile ?: photoFile) // Fallback to original if crop fails
            }

            override fun onError(exception: ImageCaptureException) {
                exception.printStackTrace()
                onImageCaptured(null)
            }
        }
    )
}

/**
 * 🎯 NEW: Crops captured image to match the guide frame dimensions
 */
private fun cropImageToGuideFrame(
    imageFile: File,
    clothingType: ClothingType,
    context: Context
): File? {
    try {
        // Load the full bitmap
        val originalBitmap = BitmapFactory.decodeFile(imageFile.absolutePath) ?: return null

        // Get image dimensions
        val imageWidth = originalBitmap.width
        val imageHeight = originalBitmap.height

        // Calculate crop rectangle using the same ratios as the guide overlay
        val cropWidth = (imageWidth * clothingType.widthRatio).toInt()
        val cropHeight = (imageHeight * clothingType.heightRatio).toInt()
        val cropLeft = (imageWidth - cropWidth) / 2
        val centerTop = (imageHeight - cropHeight) / 2
        val cropTop = (centerTop + (imageHeight * clothingType.verticalOffset)).toInt()

        // Ensure crop bounds are within image
        val safeCropTop = cropTop.coerceIn(0, imageHeight - cropHeight)
        val safeCropLeft = cropLeft.coerceIn(0, imageWidth - cropWidth)
        val safeCropWidth = cropWidth.coerceAtMost(imageWidth - safeCropLeft)
        val safeCropHeight = cropHeight.coerceAtMost(imageHeight - safeCropTop)

        // Crop the bitmap
        val croppedBitmap = Bitmap.createBitmap(
            originalBitmap,
            safeCropLeft,
            safeCropTop,
            safeCropWidth,
            safeCropHeight
        )

        // Handle rotation from EXIF data
        val rotatedBitmap = handleImageRotation(imageFile, croppedBitmap)

        // Save cropped image to new file
        val croppedFile = File(
            context.cacheDir,
            "cropped_${imageFile.name}"
        )

        FileOutputStream(croppedFile).use { out ->
            rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
        }

        // Clean up
        originalBitmap.recycle()
        croppedBitmap.recycle()
        if (rotatedBitmap != croppedBitmap) {
            rotatedBitmap.recycle()
        }
        imageFile.delete() // Delete original uncropped file

        return croppedFile
    } catch (e: Exception) {
        e.printStackTrace()
        return null
    }
}

/**
 * 🎯 NEW: Handles image rotation based on EXIF orientation
 */
private fun handleImageRotation(imageFile: File, bitmap: Bitmap): Bitmap {
    try {
        val exif = ExifInterface(imageFile.absolutePath)
        val orientation = exif.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )

        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            else -> return bitmap // No rotation needed
        }

        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    } catch (e: Exception) {
        e.printStackTrace()
        return bitmap
    }
}
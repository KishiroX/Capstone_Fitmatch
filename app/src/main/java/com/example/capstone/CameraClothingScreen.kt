package com.example.capstone.ui.screen

import android.Manifest
import android.content.Context
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FlashAuto
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Camera screen with smart guides for capturing clothing items.
 * Features: Real-time preview, guide overlay, flash control, camera flip.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraClothingScreen(
    onBack: () -> Unit,
    onImageCaptured: (File) -> Unit
) {
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
    }

    when {
        cameraPermissionState.status.isGranted -> {
            CameraContent(
                onBack = onBack,
                onImageCaptured = onImageCaptured
            )
        }
        else -> {
            CameraPermissionDenied(onBack = onBack)
        }
    }
}

@Composable
private fun CameraContent(
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

        val imageCaptureBuilder = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .setFlashMode(flashMode)

        imageCapture = imageCaptureBuilder.build()

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
        // Camera Preview
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )

        // Smart Guide Overlay
        SmartGuideOverlay()

        // Top Controls
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

        // Bottom Controls - FIXED LAYOUT
        BottomCameraControls(
            isCapturing = isCapturing,
            onCapture = {
                isCapturing = true
                imageCapture?.let { capture ->
                    takePicture(context, capture) { file ->
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

        // Capturing Overlay (optional feedback)
        if (isCapturing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(48.dp)
                )
            }
        }
    }
}

/**
 * Smart guide overlay showing optimal framing for clothing items.
 */
@Composable
private fun SmartGuideOverlay() {
    Box(modifier = Modifier.fillMaxSize()) {
        // Dimmed background
        Canvas(modifier = Modifier.fillMaxSize()) {
            val rectWidth = size.width * 0.75f
            val rectHeight = size.height * 0.6f
            val rectLeft = (size.width - rectWidth) / 2
            val rectTop = (size.height - rectHeight) / 2

            // Dimmed overlay
            drawRect(
                color = Color.Black.copy(alpha = 0.5f),
                size = size
            )

            // Clear center rectangle
            drawRoundRect(
                color = Color.Transparent,
                topLeft = Offset(rectLeft, rectTop),
                size = Size(rectWidth, rectHeight),
                cornerRadius = CornerRadius(20f),
                blendMode = androidx.compose.ui.graphics.BlendMode.Clear
            )

            // Guide border with dashed line
            drawRoundRect(
                color = Color.White,
                topLeft = Offset(rectLeft, rectTop),
                size = Size(rectWidth, rectHeight),
                cornerRadius = CornerRadius(20f),
                style = Stroke(
                    width = 4f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 10f))
                )
            )
        }

        // Instruction text
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 80.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.Black.copy(alpha = 0.7f),
                modifier = Modifier.padding(horizontal = 24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "📷 Position clothing within frame",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Lay flat • Good lighting • Clear background",
                        color = Color.White.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

/**
 * FIXED: Top camera controls with proper icons
 */
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
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Back button (LEFT)
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Back",
                tint = Color.White
            )
        }

        // Flash button (RIGHT) - IMPROVED ICONS
        IconButton(
            onClick = onFlashToggle,
            modifier = Modifier
                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
        ) {
            val flashIcon = when (flashMode) {
                ImageCapture.FLASH_MODE_ON -> Icons.Default.FlashOn
                ImageCapture.FLASH_MODE_AUTO -> Icons.Default.FlashAuto
                else -> Icons.Default.FlashOff
            }

            Icon(
                imageVector = flashIcon,
                contentDescription = when (flashMode) {
                    ImageCapture.FLASH_MODE_ON -> "Flash On"
                    ImageCapture.FLASH_MODE_AUTO -> "Flash Auto"
                    else -> "Flash Off"
                },
                tint = Color.White
            )
        }
    }
}

/**
 * FIXED: Bottom camera controls with correct button order
 * Layout: [Empty Space] [Capture Button] [Flip Camera]
 */
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
            // LEFT: Empty space for visual balance
            Spacer(modifier = Modifier.size(56.dp))

            // CENTER: Capture button
            Button(
                onClick = onCapture,
                enabled = !isCapturing,
                modifier = Modifier
                    .size(72.dp)
                    .border(4.dp, Color.White, CircleShape),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White.copy(alpha = 0.9f),
                    disabledContainerColor = Color.Gray.copy(alpha = 0.5f)
                ),
                shape = CircleShape,
                contentPadding = PaddingValues(0.dp)
            ) {
                if (isCapturing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 3.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Capture Photo",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            // RIGHT: Flip camera button
            IconButton(
                onClick = onFlipCamera,
                enabled = !isCapturing,
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        Color.Black.copy(alpha = if (isCapturing) 0.3f else 0.5f),
                        CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.FlipCameraAndroid,
                    contentDescription = "Flip Camera",
                    tint = Color.White.copy(alpha = if (isCapturing) 0.5f else 1f),
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
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = "📷",
                style = MaterialTheme.typography.displayLarge
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Camera Permission Required",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Please grant camera permission to capture clothing photos",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onBack) {
                Text("Go Back")
            }
        }
    }
}

// Helper functions
private suspend fun Context.getCameraProvider(): ProcessCameraProvider =
    suspendCoroutine { continuation ->
        ProcessCameraProvider.getInstance(this).also { future ->
            future.addListener({
                continuation.resume(future.get())
            }, ContextCompat.getMainExecutor(this))
        }
    }

private fun takePicture(
    context: Context,
    imageCapture: ImageCapture,
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
                onImageCaptured(photoFile)
            }

            override fun onError(exception: ImageCaptureException) {
                exception.printStackTrace()
                onImageCaptured(null)
            }
        }
    )
}
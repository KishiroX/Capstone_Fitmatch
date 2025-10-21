package com.example.capstone.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.UUID
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.example.capstone.CameraPreviewWithCapture
import java.io.InputStream
import java.nio.ByteBuffer
import com.example.capstone.R
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(navController: NavController) {
    val imageCapture = remember { ImageCapture.Builder().build() }
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isAligned by remember { mutableStateOf(false) }
    var isUploading by remember { mutableStateOf(false) }
    var uploadError by remember { mutableStateOf<String?>(null) }

    var useFrontCamera by remember { mutableStateOf(false) }

    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val context = navController.context
            val inputStream: InputStream? = context.contentResolver.openInputStream(it)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            bitmap?.let { selected ->
                capturedBitmap = selected
                Log.d("ScanScreen", "Image loaded from gallery")
            }
        }
    }

    // Show error dialog if upload fails
    uploadError?.let { error ->
        AlertDialog(
            onDismissRequest = { uploadError = null },
            title = { Text("Upload Failed") },
            text = { Text(error) },
            confirmButton = {
                TextButton(onClick = { uploadError = null }) {
                    Text("OK")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF00C8A0)),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(40.dp))
        Text(
            "Scan Body",
            fontSize = 22.sp,
            color = Color.White,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(16.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            if (capturedBitmap == null) {
                Box(modifier = Modifier.fillMaxSize()) {
                    CameraPreviewWithCapture(
                        imageCapture = imageCapture,
                        modifier = Modifier.fillMaxSize(),
                        onPoseDetected = { aligned -> isAligned = aligned },
                        useFrontCamera = useFrontCamera
                    )

                    IconButton(
                        onClick = { useFrontCamera = !useFrontCamera },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp)
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.7f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.FlipCameraAndroid,
                            contentDescription = "Switch Camera",
                            tint = Color(0xFF00C8A0)
                        )
                    }

                    Image(
                        painter = painterResource(id = R.drawable.body_outline),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        colorFilter = ColorFilter.tint(
                            if (isAligned) Color.Green.copy(alpha = 0.6f)
                            else Color.White.copy(alpha = 0.6f)
                        )
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Fit your body inside the outline",
                            color = Color.White,
                            fontSize = 16.sp
                        )
                    }
                }
            } else {
                Image(
                    bitmap = capturedBitmap!!.asImageBitmap(),
                    contentDescription = "Captured Preview",
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Loading overlay
            if (isUploading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.7f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Color.White)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Uploading...", color = Color.White, fontSize = 16.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            if (capturedBitmap == null) {
                Button(
                    onClick = {
                        imageCapture.takePicture(
                            ContextCompat.getMainExecutor(navController.context),
                            object : ImageCapture.OnImageCapturedCallback() {
                                override fun onCaptureSuccess(image: ImageProxy) {
                                    val bitmap = imageProxyToBitmap(image)
                                    capturedBitmap = bitmap
                                    Log.d("ScanScreen", "Photo captured successfully")
                                    image.close()
                                }

                                override fun onError(exception: ImageCaptureException) {
                                    Log.e("ScanScreen", "Capture failed: ${exception.message}", exception)
                                }
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color(0xFF00C8A0))
                    Spacer(Modifier.width(8.dp))
                    Text("Capture", color = Color(0xFF00C8A0))
                }

                Button(
                    onClick = { galleryLauncher.launch("image/*") },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(Icons.Default.Upload, contentDescription = null, tint = Color(0xFF00C8A0))
                    Spacer(Modifier.width(8.dp))
                    Text("Pick from Gallery", color = Color(0xFF00C8A0))
                }
            } else {
                Button(
                    onClick = {
                        val userId = auth.currentUser?.uid
                        val bitmap = capturedBitmap

                        Log.d("ScanScreen", "Confirm clicked - userId: $userId, bitmap: ${bitmap != null}")

                        if (userId != null && bitmap != null) {
                            isUploading = true
                            Log.d("ScanScreen", "Starting upload for user: $userId")

                            uploadScanToCloudinary(bitmap, userId) { downloadUrl ->
                                if (downloadUrl != null) {
                                    Log.d("ScanScreen", "✅ Upload successful: $downloadUrl")

                                    val scanData = mapOf(
                                        "scanId" to "bodyScan",
                                        "timestamp" to System.currentTimeMillis(),
                                        "imageUrl" to downloadUrl
                                    )

                                    db.collection("users")
                                        .document(userId)
                                        .collection("scans")
                                        .document("bodyScan")
                                        .set(scanData)
                                        .addOnSuccessListener {
                                            Log.d("ScanScreen", "✅ Saved to Firestore")
                                            isUploading = false

                                            // FIXED: Pass bitmap and URL via savedStateHandle
                                            navController.currentBackStackEntry?.savedStateHandle?.set("capturedBitmap", bitmap)
                                            navController.currentBackStackEntry?.savedStateHandle?.set("bodyScanUrl", downloadUrl)

                                            navController.navigate("result") {
                                                popUpTo("scan") { inclusive = false }
                                            }
                                        }
                                        .addOnFailureListener { e ->
                                            Log.e("ScanScreen", "❌ Firestore error: ${e.message}")
                                            isUploading = false
                                            uploadError = "Failed to save data: ${e.message}"
                                        }
                                } else {
                                    Log.e("ScanScreen", "❌ Upload failed - no URL")
                                    isUploading = false
                                    uploadError = "Failed to upload image. Please try again."
                                }
                            }
                        } else {
                            Log.e("ScanScreen", "❌ Missing userId or bitmap")
                            uploadError = if (userId == null) "Please log in first" else "No image captured"
                        }
                    },
                    enabled = !isUploading,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text("Confirm", color = Color(0xFF00C8A0))
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        capturedBitmap = null
                        Log.d("ScanScreen", "Photo cleared for retake")
                    },
                    enabled = !isUploading,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = Color(0xFF00C8A0))
                    Spacer(Modifier.width(8.dp))
                    Text("Retake Photo", color = Color(0xFF00C8A0))
                }
            }
        }
    }
}

fun imageProxyToBitmap(image: ImageProxy): Bitmap {
    val planeProxy = image.planes.firstOrNull()
        ?: return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)

    val buffer: ByteBuffer = planeProxy.buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)

    // Decode bitmap
    var bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

    // Fix rotation issue
    val rotationDegrees = image.imageInfo.rotationDegrees
    if (rotationDegrees != 0) {
        val matrix = Matrix()
        matrix.postRotate(rotationDegrees.toFloat())
        bitmap = Bitmap.createBitmap(
            bitmap,
            0,
            0,
            bitmap.width,
            bitmap.height,
            matrix,
            true
        )
    }

    return bitmap
}

fun uploadScanToCloudinary(bitmap: Bitmap, userId: String, onComplete: (String?) -> Unit) {
    try {
        val baos = java.io.ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos)
        val data = baos.toByteArray()

        Log.d("Cloudinary", "Image size: ${data.size / 1024}KB")

        MediaManager.get().upload(data)
            .option("folder", "body_scans")
            .option("public_id", "user_$userId")
            .option("overwrite", true)
            .callback(object : UploadCallback {
                override fun onStart(requestId: String) {
                    Log.d("Cloudinary", "Upload started: $requestId")
                }

                override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {
                    val progress = (bytes.toDouble() / totalBytes.toDouble() * 100).toInt()
                    Log.d("Cloudinary", "Progress: $progress%")
                }

                override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                    val url = resultData["secure_url"] as? String
                    Log.d("Cloudinary", "✅ Success: $url")
                    onComplete(url)
                }

                override fun onError(requestId: String, error: ErrorInfo) {
                    Log.e("Cloudinary", "❌ Error: ${error.description}")
                    onComplete(null)
                }

                override fun onReschedule(requestId: String, error: ErrorInfo) {
                    Log.w("Cloudinary", "⚠️ Rescheduled: ${error.description}")
                }
            })
            .dispatch()
    } catch (e: Exception) {
        Log.e("Cloudinary", "❌ Exception: ${e.message}", e)
        onComplete(null)
    }
}
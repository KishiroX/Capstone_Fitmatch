package com.example.capstone.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(navController: NavController) {
    val imageCapture = remember { ImageCapture.Builder().build() } //<Camera X
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isAligned by remember { mutableStateOf(false) }

    // Add camera toggle state
    var useFrontCamera by remember { mutableStateOf(false) }

    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    // Launcher for picking image from gallery
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val context = navController.context
            val inputStream: InputStream? = context.contentResolver.openInputStream(it)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            bitmap?.let { selected ->
                val userId = auth.currentUser?.uid
                if (userId != null) {
                    val scanId = UUID.randomUUID().toString()
                    val scanData = mapOf(
                        "scanId" to scanId,
                        "timestamp" to System.currentTimeMillis(),
                        "source" to "gallery"
                    )
                    db.collection("users")
                        .document(userId)
                        .collection("scans")
                        .document(scanId)
                        .set(scanData)
                        .addOnSuccessListener { Log.d("ScanScreen", "Scan saved successfully") }
                        .addOnFailureListener { e -> Log.e("ScanScreen", "Error saving scan", e) }
                }

                navController.currentBackStackEntry?.savedStateHandle?.set("capturedBitmap", selected)
                navController.navigate("result")
            }
        }
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
                // Camera preview with mannequin overlay
                Box(modifier = Modifier.fillMaxSize()) {
                    CameraPreviewWithCapture(
                        imageCapture = imageCapture,
                        modifier = Modifier.fillMaxSize(),
                        onPoseDetected = { aligned -> isAligned = aligned },
                        useFrontCamera = useFrontCamera // pass toggle state
                    )

                    // Switch camera button (top-right)
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

                    // Outline image overlay
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

                    // Instruction text
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
                //  Captured Preview
                Image(
                    bitmap = capturedBitmap!!.asImageBitmap(),
                    contentDescription = "Captured Preview",
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            if (capturedBitmap == null) {
                // Capture button
                Button(
                    onClick = {
                        imageCapture.takePicture(
                            ContextCompat.getMainExecutor(navController.context),
                            object : ImageCapture.OnImageCapturedCallback() {
                                override fun onCaptureSuccess(image: ImageProxy) {
                                    val bitmap = imageProxyToBitmap(image)
                                    capturedBitmap = bitmap
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

                // Gallery picker button
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
                //  Confirm button
                Button(
                    onClick = {
                        val userId = auth.currentUser?.uid
                        if (userId != null && capturedBitmap != null) {
                            uploadScanToFirebase(capturedBitmap!!, userId) { downloadUrl ->
                                if (downloadUrl != null) {
                                    // Save metadata in Firestore
                                    val scanId = UUID.randomUUID().toString()
                                    val scanData = mapOf(
                                        "scanId" to scanId,
                                        "timestamp" to System.currentTimeMillis(),
                                        "imageUrl" to downloadUrl
                                    )
                                    db.collection("users")
                                        .document(userId)
                                        .collection("wardrobe") //  Save under wardrobe collection
                                        .document(scanId)
                                        .set(scanData)

                                    // Go to wardrobe or result screen
                                    navController.navigate("wardrobe")
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text("Confirm", color = Color(0xFF00C8A0))
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Retake button
                Button(
                    onClick = { capturedBitmap = null },
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

// Convert ImageProxy to Bitmap
fun imageProxyToBitmap(image: ImageProxy): Bitmap {
    val planeProxy = image.planes.firstOrNull() ?: return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
    val buffer: ByteBuffer = planeProxy.buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
}

// Upload scanned bitmap to Firebase Storage
fun uploadScanToFirebase(bitmap: Bitmap, userId: String, onComplete: (String?) -> Unit) {
    val storageRef = com.google.firebase.storage.FirebaseStorage.getInstance().reference
    val scanId = UUID.randomUUID().toString()
    val scanRef = storageRef.child("users/$userId/scans/$scanId.jpg")

    // Convert bitmap to byte array
    val baos = java.io.ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, baos)
    val data = baos.toByteArray()

    val uploadTask = scanRef.putBytes(data)
    uploadTask.addOnSuccessListener {
        scanRef.downloadUrl.addOnSuccessListener { uri ->
            onComplete(uri.toString()) //  returns the download URL
        }
    }.addOnFailureListener {
        onComplete(null)
    }
}

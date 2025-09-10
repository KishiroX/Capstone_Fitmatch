package com.example.capstone.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.example.capstone.CameraPreviewWithCapture
import java.io.InputStream
import java.nio.ByteBuffer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(navController: NavController) {
    val imageCapture = remember { ImageCapture.Builder().build() }

    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }

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
        ) {
            CameraPreviewWithCapture(imageCapture = imageCapture, modifier = Modifier.fillMaxSize())
        }

        Spacer(modifier = Modifier.height(16.dp))

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

                            bitmap?.let {
                                navController.currentBackStackEntry?.savedStateHandle?.set("capturedBitmap", it)
                                navController.navigate("result")
                            }
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
                .padding(8.dp)
                .fillMaxWidth()
        ) {
            Text("Capture", color = Color(0xFF00C8A0))
        }

        //  Gallery button
        Button(
            onClick = { galleryLauncher.launch("image/*") },
            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth()
        ) {
            Text("Pick from Gallery", color = Color(0xFF00C8A0))
        }
    }
}


fun imageProxyToBitmap(image: ImageProxy): Bitmap? {
    val planeProxy = image.planes.firstOrNull() ?: return null
    val buffer: ByteBuffer = planeProxy.buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
}

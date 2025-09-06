package com.example.capstone.ui.screens

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.capstone.CameraPreviewWithCapture
import java.util.concurrent.Executor
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(navController: NavController) {
    val context = LocalContext.current
    val executor = remember { Executors.newSingleThreadExecutor() }

    val imageCapture = remember { ImageCapture.Builder().build() }

    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data
            uri?.let {
                val bitmap = if (Build.VERSION.SDK_INT < 28) {
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                } else {
                    val source = ImageDecoder.createSource(context.contentResolver, uri)
                    ImageDecoder.decodeBitmap(source)
                }
                capturedBitmap = bitmap
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF00C8A0)),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(40.dp))
        Text("Body Scan", fontSize = 24.sp, color = Color.Black)

        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .aspectRatio(0.6f)
                .clip(RoundedCornerShape(40.dp))
                .border(2.dp, Color.Black, RoundedCornerShape(40.dp))
        ) {
            if (capturedBitmap == null) {
                CameraPreviewWithCapture(imageCapture, Modifier.fillMaxSize())
            } else {
                Image(bitmap = capturedBitmap!!.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxSize())
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            if (capturedBitmap == null) {
                IconButton(
                    onClick = {
                        takePhoto(imageCapture, executor) { bitmap ->
                            capturedBitmap = bitmap
                        }
                    },
                    modifier = Modifier
                        .size(60.dp)
                        .background(Color(0xFF00816C), shape = CircleShape)
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = "Capture", tint = Color.White)
                }

                Button(onClick = {
                    val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                    galleryLauncher.launch(intent)
                }) {
                    Text("Gallery")
                }
            } else {
                Button(onClick = {
                    navController.currentBackStackEntry?.savedStateHandle?.set("capturedBitmap", capturedBitmap)
                    navController.navigate("result")
                }) {
                    Text("Confirm")
                }
                Button(onClick = {
                    capturedBitmap = null
                }) {
                    Text("Retake")
                }
            }
        }
    }
}

fun takePhoto(imageCapture: ImageCapture, executor: Executor, onPhotoTaken: (Bitmap) -> Unit) {
    imageCapture.takePicture(
        executor,
        object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                val bitmap = imageProxyToBitmap(image)
                image.close()
                onPhotoTaken(bitmap)
            }

            override fun onError(exception: ImageCaptureException) {
                exception.printStackTrace()
            }
        }
    )
}

fun imageProxyToBitmap(image: ImageProxy): Bitmap {
    val buffer = image.planes[0].buffer
    val bytes = ByteArray(buffer.capacity())
    buffer.get(bytes)
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
}

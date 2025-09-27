package com.example.capstone

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseDetector
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions
import com.google.mlkit.vision.pose.PoseLandmark
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.absoluteValue
import androidx.camera.core.ImageProxy

@SuppressLint("UnsafeOptInUsageError")
@Composable
fun CameraPreviewWithCapture(
    imageCapture: ImageCapture,
    modifier: Modifier = Modifier,
    onPoseDetected: (Boolean) -> Unit,
    useFrontCamera: Boolean // pass this from ScanScreen
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // single-threaded executor for analyzer
    val cameraExecutor: ExecutorService = remember { Executors.newSingleThreadExecutor() }
    DisposableEffect(Unit) {
        onDispose { cameraExecutor.shutdown() }
    }

    // ML Kit Pose detector (stream mode)
    val options = remember {
        PoseDetectorOptions.Builder()
            .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
            .build()
    }
    val poseDetector: PoseDetector = remember { PoseDetection.getClient(options) }

    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)

            // Bind initially when camera provider is ready
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                bindCamera(
                    context = ctx,
                    lifecycleOwner = lifecycleOwner,
                    cameraProvider = cameraProvider,
                    previewView = previewView,
                    imageCapture = imageCapture,
                    cameraExecutor = cameraExecutor,
                    poseDetector = poseDetector,
                    onPoseDetected = onPoseDetected,
                    useFrontCamera = useFrontCamera
                )
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        },
        update = { previewView ->
            // Rebind when Compose re-composes (e.g. when useFrontCamera toggles)
            val cameraProvider = cameraProviderFuture.get()
            bindCamera(
                context = context,
                lifecycleOwner = lifecycleOwner,
                cameraProvider = cameraProvider,
                previewView = previewView,
                imageCapture = imageCapture,
                cameraExecutor = cameraExecutor,
                poseDetector = poseDetector,
                onPoseDetected = onPoseDetected,
                useFrontCamera = useFrontCamera
            )
        },
        modifier = modifier
    )
}

private fun bindCamera(
    context: Context,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    cameraProvider: ProcessCameraProvider,
    previewView: PreviewView,
    imageCapture: ImageCapture,
    cameraExecutor: ExecutorService,
    poseDetector: PoseDetector,
    onPoseDetected: (Boolean) -> Unit,
    useFrontCamera: Boolean
) {
    try {
        // Unbind everything before (re)binding
        cameraProvider.unbindAll()

        // Choose camera
        val cameraSelector = if (useFrontCamera) CameraSelector.DEFAULT_FRONT_CAMERA
        else CameraSelector.DEFAULT_BACK_CAMERA

        // Preview use case
        val previewUseCase = Preview.Builder().build().also { preview ->
            preview.setSurfaceProvider(previewView.surfaceProvider)
        }

        // Mirror preview visually if using front camera (so it's selfie-like)
        previewView.scaleX = if (useFrontCamera) -1f else 1f

        // ImageAnalysis for ML Kit pose detection
        val analysisUseCase = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also { analysis ->
                analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                    processPoseWithDetector(imageProxy, poseDetector) { aligned ->
                        // Send alignment result to ScanScreen
                        onPoseDetected(aligned)
                    }
                }
            }

        cameraProvider.bindToLifecycle(
            lifecycleOwner,
            cameraSelector,
            previewUseCase,
            imageCapture,
            analysisUseCase
        )
    } catch (e: Exception) {
        Log.e("CameraPreview", "bindCamera failed: ${e.message}", e)
    }
}

@androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
private fun processPoseWithDetector(
    imageProxy: ImageProxy,
    detector: PoseDetector,
    onAligned: (Boolean) -> Unit
) {
    val mediaImage = imageProxy.image
    if (mediaImage != null) {
        val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

        detector.process(inputImage)
            .addOnSuccessListener { pose ->
                // Use imageProxy width/height (image coordinate space)
                val previewWidth = imageProxy.width.toFloat()
                val previewHeight = imageProxy.height.toFloat()
                val aligned = checkAlignment(pose, previewWidth, previewHeight)
                onAligned(aligned)
            }
            .addOnFailureListener { _ ->
                onAligned(false)
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    } else {
        imageProxy.close()
    }
}

fun checkAlignment(
    pose: com.google.mlkit.vision.pose.Pose,
    previewWidth: Float,
    previewHeight: Float
): Boolean {
    val leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
    val rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)
    val leftHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)
    val rightHip = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)

    if (leftShoulder != null && rightShoulder != null && leftHip != null && rightHip != null) {
        val shoulderCenterX = (leftShoulder.position.x + rightShoulder.position.x) / 2f
        val hipCenterX = (leftHip.position.x + rightHip.position.x) / 2f
        val bodyCenterX = (shoulderCenterX + hipCenterX) / 2f

        val shoulderCenterY = (leftShoulder.position.y + rightShoulder.position.y) / 2f
        val hipCenterY = (leftHip.position.y + rightHip.position.y) / 2f
        val torsoHeight = (hipCenterY - shoulderCenterY).absoluteValue
        val shoulderSpan = (rightShoulder.position.x - leftShoulder.position.x).absoluteValue

        val centerX = previewWidth / 2f
        val centerY = previewHeight / 2f

        Log.d("AlignmentCheck",
            "bodyCenterX=$bodyCenterX centerX=$centerX diffX=${(bodyCenterX - centerX).absoluteValue}, " +
                    "shoulderSpan=$shoulderSpan, torsoHeight=$torsoHeight, " +
                    "previewW=$previewWidth previewH=$previewHeight"
        )

        val toleranceX = previewWidth * 0.15f     // loosened to 15%
        val minShoulderSpan = previewWidth * 0.1f // looser
        val maxShoulderSpan = previewWidth * 0.9f
        val minTorso = previewHeight * 0.1f
        val maxTorso = previewHeight * 0.9f
        val toleranceY = previewHeight * 0.2f     // loosened vertical tolerance

        val horizontalAligned = (bodyCenterX - centerX).absoluteValue < toleranceX &&
                shoulderSpan in minShoulderSpan..maxShoulderSpan

        val verticalAligned = torsoHeight in minTorso..maxTorso &&
                (((shoulderCenterY + hipCenterY) / 2f - centerY).absoluteValue < toleranceY)

        val aligned = horizontalAligned && verticalAligned
        Log.d("AlignmentCheck", "horizontal=$horizontalAligned vertical=$verticalAligned -> $aligned")

        return aligned
    }
    return false
}

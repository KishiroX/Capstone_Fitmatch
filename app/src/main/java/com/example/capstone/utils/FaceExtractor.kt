package com.example.capstone.utils

import android.content.Context
import android.graphics.*
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.tasks.await
import kotlin.math.max

class FaceExtractor(private val context: Context) {

    private val faceDetectorOptions = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
        .build()

    private val faceDetector = FaceDetection.getClient(faceDetectorOptions)

    /**
     * Extract face from body scan and return circular face bitmap
     */
    suspend fun extractFace(bodyScanBitmap: Bitmap): Bitmap? {
        return try {
            val inputImage = InputImage.fromBitmap(bodyScanBitmap, 0)
            val faces = faceDetector.process(inputImage).await()

            if (faces.isEmpty()) {
                android.util.Log.e("FaceExtractor", "No face detected")
                return null
            }

            val face = faces.first()
            val boundingBox = face.boundingBox

            // Add padding around face
            val padding = 40
            val left = max(0, boundingBox.left - padding)
            val top = max(0, boundingBox.top - padding)
            val right = (boundingBox.right + padding).coerceAtMost(bodyScanBitmap.width)
            val bottom = (boundingBox.bottom + padding).coerceAtMost(bodyScanBitmap.height)

            val faceWidth = right - left
            val faceHeight = bottom - top

            // Extract face region
            val faceBitmap = Bitmap.createBitmap(
                bodyScanBitmap,
                left,
                top,
                faceWidth,
                faceHeight
            )

            // Create circular face
            createCircularFace(faceBitmap)

        } catch (e: Exception) {
            android.util.Log.e("FaceExtractor", "Face extraction failed", e)
            null
        }
    }

    /**
     * Create circular cropped face
     */
    private fun createCircularFace(faceBitmap: Bitmap): Bitmap {
        val size = max(faceBitmap.width, faceBitmap.height)
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)

        val canvas = Canvas(output)
        val paint = Paint().apply {
            isAntiAlias = true
            color = Color.WHITE
        }

        // Draw circle
        val radius = size / 2f
        canvas.drawCircle(radius, radius, radius, paint)

        // Apply circular mask
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)

        val rect = Rect(0, 0, faceBitmap.width, faceBitmap.height)
        val destRect = Rect(0, 0, size, size)
        canvas.drawBitmap(faceBitmap, rect, destRect, paint)

        faceBitmap.recycle()
        return output
    }
}
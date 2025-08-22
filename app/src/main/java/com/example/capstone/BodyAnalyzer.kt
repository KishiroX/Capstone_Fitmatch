package com.example.capstone.analysis

import android.content.Context
import android.graphics.Bitmap
import androidx.core.graphics.get
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.ImageProcessingOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import kotlin.math.pow
import kotlin.math.sqrt

class BodyAnalyzer(private val context: Context) {

    private lateinit var landmarker: PoseLandmarker

    fun loadModel() {
        val options = PoseLandmarker.PoseLandmarkerOptions.builder()
            .setBaseOptions(
                BaseOptions.builder()
                    .setModelAssetPath("pose_landmarker_lite.task")
                    .build()
            )
            .setRunningMode(RunningMode.IMAGE)
            .build()

        landmarker = PoseLandmarker.createFromOptions(context, options)
    }

    fun analyze(bitmap: Bitmap): PoseLandmarkerResult? {
        return try {
            if (!::landmarker.isInitialized) loadModel()
            val mpImage = BitmapImageBuilder(bitmap).build()
            landmarker.detect(mpImage, ImageProcessingOptions.builder().build())
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun calculateFullBodyComposition(bitmap: Bitmap, result: PoseLandmarkerResult?): Map<String, Any> {
        val landmarks = result?.landmarks()?.firstOrNull()?.takeIf { it.size >= 33 } ?: return emptyMap()

        fun distance(aX: Float, aY: Float, bX: Float, bY: Float): Float {
            return sqrt((bX - aX).pow(2) + (bY - aY).pow(2))
        }

        val ls = landmarks[11]
        val rs = landmarks[12]
        val lh = landmarks[23]
        val rh = landmarks[24]
        val la = landmarks[27]
        val ra = landmarks[28]
        val lw = landmarks[15]
        val rw = landmarks[16]

        val shoulderWidth = distance(ls.x(), ls.y(), rs.x(), rs.y())
        val hipWidth = distance(lh.x(), lh.y(), rh.x(), rh.y())
        val torsoLength = distance(
            (ls.x() + rs.x()) / 2f, (ls.y() + rs.y()) / 2f,
            (lh.x() + rh.x()) / 2f, (lh.y() + rh.y()) / 2f
        )
        val legLength = (distance(lh.x(), lh.y(), la.x(), la.y()) + distance(rh.x(), rh.y(), ra.x(), ra.y())) / 2f
        val armLength = (distance(ls.x(), ls.y(), lw.x(), lw.y()) + distance(rs.x(), rs.y(), rw.x(), rw.y())) / 2f

        val skinColor = estimateSkinColor(bitmap, ls.x(), ls.y())

        return mapOf(
            "Shoulder Width" to shoulderWidth,
            "Hip Width" to hipWidth,
            "Torso Length" to torsoLength,
            "Leg Length" to legLength,
            "Arm Length" to armLength,
            "Estimated Skin Color" to skinColor
        )
    }

    private fun estimateSkinColor(bitmap: Bitmap, xNorm: Float, yNorm: Float): String {
        val xCenter = (xNorm * bitmap.width).toInt().coerceIn(1, bitmap.width - 2)
        val yCenter = (yNorm * bitmap.height).toInt().coerceIn(1, bitmap.height - 2)

        var red = 0
        var green = 0
        var blue = 0
        val sampleSize = 9

        for (dx in -1..1) {
            for (dy in -1..1) {
                val pixel = bitmap.get(xCenter + dx, yCenter + dy)
                red += (pixel shr 16) and 0xFF
                green += (pixel shr 8) and 0xFF
                blue += pixel and 0xFF
            }
        }

        return "rgb(${red / sampleSize}, ${green / sampleSize}, ${blue / sampleSize})"
    }
}

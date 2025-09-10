package com.example.capstone.analysis

import android.content.Context
import android.graphics.Bitmap
import androidx.core.graphics.get
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import kotlin.math.pow
import kotlin.math.sqrt

class BodyAnalyzer(private val context: Context) {

    private lateinit var landmarker: PoseLandmarker

    /**
     * Loads the Mediapipe Pose Landmarker model.
     * Make sure `pose_landmarker_lite.task` is inside `app/src/main/assets/`
     */
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
        println("✅ Pose model loaded successfully.")
    }

    /**
     * Runs Mediapipe pose detection on a bitmap.
     */
    fun analyze(bitmap: Bitmap): PoseLandmarkerResult? {
        return try {
            if (!::landmarker.isInitialized) loadModel()

            val mpImage = BitmapImageBuilder(bitmap).build()
            val result = landmarker.detect(mpImage)

            if (result.landmarks().isEmpty()) {
                println("❌ No pose detected. (Check image quality or model file)")
                null
            } else {
                val count = result.landmarks()[0].size
                println("✅ Pose detected with $count landmarks")
                result
            }
        } catch (e: Exception) {
            e.printStackTrace()
            println("❌ Pose analysis failed: ${e.localizedMessage}")
            null
        }
    }

    /**
     * Calculates body measurements from landmarks.
     */
    fun calculateFullBodyComposition(
        bitmap: Bitmap,
        result: PoseLandmarkerResult?
    ): Map<String, Any> {
        val landmarks = result?.landmarks()?.firstOrNull()?.takeIf { it.size >= 33 }
            ?: return mapOf("Error" to "No valid landmarks found")

        fun distance(aX: Float, aY: Float, bX: Float, bY: Float): Float {
            return sqrt((bX - aX).pow(2) + (bY - aY).pow(2))
        }

        val ls = landmarks[11] // left shoulder
        val rs = landmarks[12] // right shoulder
        val lh = landmarks[23] // left hip
        val rh = landmarks[24] // right hip
        val la = landmarks[27] // left ankle
        val ra = landmarks[28] // right ankle
        val lw = landmarks[15] // left wrist
        val rw = landmarks[16] // right wrist

        val shoulderWidth = distance(ls.x(), ls.y(), rs.x(), rs.y())
        val hipWidth = distance(lh.x(), lh.y(), rh.x(), rh.y())
        val torsoLength = distance(
            (ls.x() + rs.x()) / 2f, (ls.y() + rs.y()) / 2f,
            (lh.x() + rh.x()) / 2f, (lh.y() + rh.y()) / 2f
        )
        val legLength = (distance(lh.x(), lh.y(), la.x(), la.y()) +
                distance(rh.x(), rh.y(), ra.x(), ra.y())) / 2f
        val armLength = (distance(ls.x(), ls.y(), lw.x(), lw.y()) +
                distance(rs.x(), rs.y(), rw.x(), rw.y())) / 2f

        val skinColor = estimateSkinColor(bitmap, ls.x(), ls.y())

        // 🔹 Ratios
        val shoulderToHip = if (hipWidth > 0) shoulderWidth / hipWidth else 0f
        val torsoToLeg = if (legLength > 0) torsoLength / legLength else 0f
        val armToLeg = if (legLength > 0) armLength / legLength else 0f
        val shoulderToHeight = if ((torsoLength + legLength) > 0) shoulderWidth / (torsoLength + legLength) else 0f

        return mapOf(
            "Shoulder Width" to shoulderWidth,
            "Hip Width" to hipWidth,
            "Torso Length" to torsoLength,
            "Leg Length" to legLength,
            "Arm Length" to armLength,
            "Estimated Skin Color" to skinColor,

            // ✅ Ratios
            "Shoulder-to-Hip Ratio" to shoulderToHip,
            "Torso-to-Leg Ratio" to torsoToLeg,
            "Arm-to-Leg Ratio" to armToLeg,
            "Shoulder-to-Height Ratio" to shoulderToHeight
        )
    }


    /**
     * Estimates skin color near the left shoulder landmark.
     */
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

        val avgR = red / sampleSize
        val avgG = green / sampleSize
        val avgB = blue / sampleSize

        // Compute perceived brightness (Y from YUV)
        val brightness = (0.299 * avgR + 0.587 * avgG + 0.114 * avgB).toInt()

        // Classify into realistic tones
        val label = when {
            brightness > 220 -> "Fair"
            brightness in 180..220 -> "Light Tan"
            brightness in 140..179 -> "Olive"
            brightness in 100..139 -> "Brown"
            else -> "Deep Brown"
        }

        // Also return the RGB values for debugging or preview if needed
        return "$label (RGB: $avgR, $avgG, $avgB)"
    }

}

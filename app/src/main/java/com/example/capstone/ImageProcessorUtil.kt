package com.example.capstone.utils

import android.content.Context
import android.graphics.*
import androidx.exifinterface.media.ExifInterface
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.Segmentation
import com.google.mlkit.vision.segmentation.SegmentationMask
import com.google.mlkit.vision.segmentation.selfie.SelfieSegmenterOptions
import com.google.mlkit.vision.segmentation.subject.SubjectSegmentation
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenterOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.min
import kotlin.math.abs
import kotlin.math.sqrt

data class ProcessedImage(
    val originalFile: File,
    val processedFile: File,
    val thumbnailFile: File
)

enum class CaptureMode {
    FLAT_CLOTHING,
    WORN_CLOTHING,
    AUTO_DETECT
}

class ImageProcessorUtil(private val context: Context) {

    suspend fun processClothingImage(
        imageFile: File,
        mode: CaptureMode = CaptureMode.AUTO_DETECT,
        onProgress: ((String) -> Unit)? = null
    ): ProcessedImage = withContext(Dispatchers.IO) {
        try {
            onProgress?.invoke("Loading image...")

            // Fix orientation first before any processing
            val orientedBitmap = loadBitmapWithCorrectOrientation(imageFile)
                ?: throw IllegalArgumentException("Failed to decode image")

            val workingBitmap = if (orientedBitmap.width > 2000 || orientedBitmap.height > 2000) {
                onProgress?.invoke("Resizing image...")
                resizeBitmap(orientedBitmap, 2000).also { orientedBitmap.recycle() }
            } else {
                orientedBitmap
            }

            onProgress?.invoke("Analyzing image...")
            val detectedMode = if (mode == CaptureMode.AUTO_DETECT) {
                detectCaptureMode(workingBitmap)
            } else {
                mode
            }

            onProgress?.invoke("Removing background (${detectedMode.name})...")
            val backgroundRemovedBitmap = when (detectedMode) {
                CaptureMode.FLAT_CLOTHING -> removeBackgroundSubjectSegmentation(workingBitmap)
                CaptureMode.WORN_CLOTHING -> removeBackgroundSubjectSegmentation(workingBitmap)
                CaptureMode.AUTO_DETECT -> removeBackgroundSubjectSegmentation(workingBitmap)
            }

            onProgress?.invoke("Cropping to bounds...")
            val croppedBitmap = autoCropFast(backgroundRemovedBitmap)

            onProgress?.invoke("Enhancing quality...")
            val enhancedBitmap = enhanceImage(croppedBitmap)

            onProgress?.invoke("Creating thumbnail...")
            val thumbnailBitmap = createThumbnail(enhancedBitmap, 400, 400)

            onProgress?.invoke("Saving files...")
            val processedFile = saveBitmapAsPNG(
                enhancedBitmap,
                "processed_${System.currentTimeMillis()}.png"
            )
            val thumbnailFile = saveBitmapAsPNG(
                thumbnailBitmap,
                "thumb_${System.currentTimeMillis()}.png"
            )

            workingBitmap.recycle()
            backgroundRemovedBitmap.recycle()
            croppedBitmap.recycle()
            enhancedBitmap.recycle()
            thumbnailBitmap.recycle()

            onProgress?.invoke("Complete!")

            ProcessedImage(
                originalFile = imageFile,
                processedFile = processedFile,
                thumbnailFile = thumbnailFile
            )
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }

    /**
     * Load bitmap and correct orientation based on EXIF data
     */
    private fun loadBitmapWithCorrectOrientation(imageFile: File): Bitmap? {
        val bitmap = BitmapFactory.decodeFile(imageFile.absolutePath) ?: return null

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
                ExifInterface.ORIENTATION_TRANSPOSE -> {
                    matrix.postRotate(90f)
                    matrix.postScale(-1f, 1f)
                }
                ExifInterface.ORIENTATION_TRANSVERSE -> {
                    matrix.postRotate(270f)
                    matrix.postScale(-1f, 1f)
                }
                else -> return bitmap // No rotation needed
            }

            val rotatedBitmap = Bitmap.createBitmap(
                bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
            )

            if (rotatedBitmap != bitmap) {
                bitmap.recycle()
            }

            return rotatedBitmap
        } catch (e: Exception) {
            e.printStackTrace()
            return bitmap
        }
    }

    private suspend fun detectCaptureMode(bitmap: Bitmap): CaptureMode {
        return try {
            val hasPerson = detectPerson(bitmap)
            if (hasPerson) CaptureMode.WORN_CLOTHING else CaptureMode.FLAT_CLOTHING
        } catch (e: Exception) {
            CaptureMode.FLAT_CLOTHING
        }
    }

    private suspend fun detectPerson(bitmap: Bitmap): Boolean {
        return try {
            val options = SelfieSegmenterOptions.Builder()
                .setDetectorMode(SelfieSegmenterOptions.SINGLE_IMAGE_MODE)
                .build()

            val segmenter = Segmentation.getClient(options)
            val inputImage = InputImage.fromBitmap(bitmap, 0)
            val mask = segmenter.process(inputImage).await()

            val maskBuffer = mask.buffer
            val maskSize = mask.width * mask.height
            var foregroundPixels = 0

            for (i in 0 until maskSize step 100) {
                val confidence = maskBuffer.getFloat(i * 4)
                if (confidence > 0.5f) {
                    foregroundPixels++
                }
            }

            val foregroundRatio = foregroundPixels.toFloat() / (maskSize / 100)
            segmenter.close()

            foregroundRatio > 0.15f
        } catch (e: Exception) {
            false
        }
    }

    /**
     * SIMPLIFIED: Use Subject Segmentation - just get foreground bitmap
     * Works for BOTH flat clothing and worn clothing
     */
    private suspend fun removeBackgroundSubjectSegmentation(bitmap: Bitmap): Bitmap {
        return try {
            val mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)

            // Simple options - just enable foreground bitmap
            val options = SubjectSegmenterOptions.Builder()
                .enableForegroundBitmap()
                .build()

            val segmenter = SubjectSegmentation.getClient(options)
            val inputImage = InputImage.fromBitmap(mutableBitmap, 0)

            val result = segmenter.process(inputImage).await()

            segmenter.close()

            // Get the foreground bitmap (already has transparent background)
            val foregroundBitmap = result.foregroundBitmap

            if (foregroundBitmap != null) {
                mutableBitmap.recycle()
                foregroundBitmap
            } else {
                // If no foreground detected, fallback to color-based
                mutableBitmap.recycle()
                removeBackgroundColorBased(bitmap)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // If Subject Segmentation fails entirely, use color-based
            removeBackgroundColorBased(bitmap)
        }
    }

    /**
     * Fallback: Color-based background removal
     */
    private fun removeBackgroundColorBased(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)

        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val backgroundSamples = mutableListOf<Int>()

        for (x in 0 until width step 10) {
            backgroundSamples.add(pixels[x])
            backgroundSamples.add(pixels[(height - 1) * width + x])
        }

        for (y in 0 until height step 10) {
            backgroundSamples.add(pixels[y * width])
            backgroundSamples.add(pixels[y * width + (width - 1)])
        }

        val backgroundColor = getMedianColor(backgroundSamples)
        val targetR = Color.red(backgroundColor)
        val targetG = Color.green(backgroundColor)
        val targetB = Color.blue(backgroundColor)

        val variance = calculateColorVariance(backgroundSamples, backgroundColor)
        val tolerance = (35 + sqrt(variance.toDouble())).toInt().coerceIn(25, 70)

        for (y in 0 until height) {
            for (x in 0 until width) {
                val index = y * width + x
                val pixel = pixels[index]

                val colorDistance = calculateColorDistance(
                    pixel, targetR, targetG, targetB
                )

                if (colorDistance < tolerance) {
                    if (!isNearObjectEdge(pixels, width, height, x, y, targetR, targetG, targetB, tolerance)) {
                        pixels[index] = Color.TRANSPARENT
                    }
                }
            }
        }

        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        result.setPixels(pixels, 0, width, 0, 0, width, height)

        return result
    }

    private fun calculateColorDistance(pixel: Int, targetR: Int, targetG: Int, targetB: Int): Int {
        val r = Color.red(pixel)
        val g = Color.green(pixel)
        val b = Color.blue(pixel)

        val rDiff = (r - targetR) * 0.30
        val gDiff = (g - targetG) * 0.59
        val bDiff = (b - targetB) * 0.11

        return sqrt(rDiff * rDiff + gDiff * gDiff + bDiff * bDiff).toInt()
    }

    private fun getMedianColor(samples: List<Int>): Int {
        val reds = samples.map { Color.red(it) }.sorted()
        val greens = samples.map { Color.green(it) }.sorted()
        val blues = samples.map { Color.blue(it) }.sorted()

        val mid = samples.size / 2
        return Color.rgb(reds[mid], greens[mid], blues[mid])
    }

    private fun calculateColorVariance(samples: List<Int>, median: Int): Float {
        val medianR = Color.red(median)
        val medianG = Color.green(median)
        val medianB = Color.blue(median)

        var variance = 0f
        for (sample in samples) {
            val r = Color.red(sample)
            val g = Color.green(sample)
            val b = Color.blue(sample)

            val diff = abs(r - medianR) + abs(g - medianG) + abs(b - medianB)
            variance += diff * diff
        }

        return variance / samples.size
    }

    private fun isNearObjectEdge(
        pixels: IntArray,
        width: Int,
        height: Int,
        x: Int,
        y: Int,
        targetR: Int,
        targetG: Int,
        targetB: Int,
        tolerance: Int
    ): Boolean {
        for (dy in -1..1) {
            for (dx in -1..1) {
                val nx = x + dx
                val ny = y + dy

                if (nx in 0 until width && ny in 0 until height) {
                    val neighborPixel = pixels[ny * width + nx]
                    val distance = calculateColorDistance(neighborPixel, targetR, targetG, targetB)

                    if (distance > tolerance * 1.5) {
                        return true
                    }
                }
            }
        }
        return false
    }

    private fun autoCropFast(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)

        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        var minX = width
        var minY = height
        var maxX = 0
        var maxY = 0

        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = pixels[y * width + x]
                if (Color.alpha(pixel) > 30) {
                    minX = min(minX, x)
                    minY = min(minY, y)
                    maxX = max(maxX, x)
                    maxY = max(maxY, y)
                }
            }
        }

        val paddingX = ((maxX - minX) * 0.1f).toInt()
        val paddingY = ((maxY - minY) * 0.1f).toInt()

        minX = max(0, minX - paddingX)
        minY = max(0, minY - paddingY)
        maxX = min(width - 1, maxX + paddingX)
        maxY = min(height - 1, maxY + paddingY)

        val cropWidth = maxX - minX + 1
        val cropHeight = maxY - minY + 1

        return if (cropWidth > 0 && cropHeight > 0) {
            Bitmap.createBitmap(bitmap, minX, minY, cropWidth, cropHeight)
        } else {
            bitmap
        }
    }

    private fun resizeBitmap(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val ratio = min(
            maxDimension.toFloat() / bitmap.width,
            maxDimension.toFloat() / bitmap.height
        )

        if (ratio >= 1.0f) return bitmap

        val newWidth = (bitmap.width * ratio).toInt()
        val newHeight = (bitmap.height * ratio).toInt()

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    private fun enhanceImage(bitmap: Bitmap): Bitmap {
        val enhanced = bitmap.copy(Bitmap.Config.ARGB_8888, true)

        val canvas = Canvas(enhanced)
        val paint = Paint().apply {
            colorFilter = ColorMatrixColorFilter(ColorMatrix().apply {
                set(floatArrayOf(
                    1.05f, 0f, 0f, 0f, 5f,
                    0f, 1.05f, 0f, 0f, 5f,
                    0f, 0f, 1.05f, 0f, 5f,
                    0f, 0f, 0f, 1f, 0f
                ))
            })
        }

        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return enhanced
    }

    private fun createThumbnail(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val ratio = min(
            maxWidth.toFloat() / bitmap.width,
            maxHeight.toFloat() / bitmap.height
        )

        val width = (bitmap.width * ratio).toInt()
        val height = (bitmap.height * ratio).toInt()

        return Bitmap.createScaledBitmap(bitmap, width, height, true)
    }

    private fun saveBitmapAsPNG(bitmap: Bitmap, filename: String): File {
        val file = File(context.cacheDir, filename)
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        return file
    }

    fun detectDominantColors(bitmap: Bitmap, numColors: Int = 3): List<String> {
        val colorMap = mutableMapOf<Int, Int>()
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)

        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        for (i in pixels.indices step 10) {
            val pixel = pixels[i]
            if (Color.alpha(pixel) > 128) {
                val simplified = simplifyColor(pixel)
                colorMap[simplified] = (colorMap[simplified] ?: 0) + 1
            }
        }

        return colorMap.entries
            .sortedByDescending { it.value }
            .take(numColors)
            .map { colorToName(it.key) }
    }

    private fun simplifyColor(color: Int): Int {
        val r = (Color.red(color) / 32) * 32
        val g = (Color.green(color) / 32) * 32
        val b = (Color.blue(color) / 32) * 32
        return Color.rgb(r, g, b)
    }

    private fun colorToName(color: Int): String {
        val r = Color.red(color)
        val g = Color.green(color)
        val b = Color.blue(color)

        return when {
            r > 200 && g > 200 && b > 200 -> "White"
            r < 50 && g < 50 && b < 50 -> "Black"
            r > 150 && g < 100 && b < 100 -> "Red"
            r < 100 && g > 150 && b < 100 -> "Green"
            r < 100 && g < 100 && b > 150 -> "Blue"
            r > 150 && g > 150 && b < 100 -> "Yellow"
            r > 150 && g < 100 && b > 150 -> "Purple"
            r > 150 && g > 100 && b < 100 -> "Orange"
            r < 100 && g > 150 && b > 150 -> "Cyan"
            r > 100 && g > 100 && b > 100 -> "Gray"
            r > 100 && g < 80 && b < 80 -> "Brown"
            r > 150 && g > 100 && b > 150 -> "Pink"
            else -> "Mixed"
        }
    }
}
package com.example.capstone.api

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

class MiragicClient(private val context: Context) {

    // ⚠️ REPLACE WITH YOUR ACTUAL API KEY
    private val apiKey = "sk_live_v5Oopua3xgzuexOt59kLv8gv3A_roWY95AeNKAZJaDA"

    private val baseUrl = "https://backend.miragic.ai/"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val apiService = retrofit.create(MiragicApiService::class.java)

    // Coil image loader for downloading images
    private val imageLoader = ImageLoader.Builder(context)
        .okHttpClient(
            OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()
        )
        .build()

    /**
     * Single item try-on (tops, bottoms, or dresses)
     */
    suspend fun singleTryOn(
        humanImageUrl: String,
        clothImageUrl: String,
        garmentType: String, // "upper_body", "lower_body", "full_body"
        onProgress: (TryOnResult) -> Unit
    ): String? {
        return try {
            android.util.Log.d("Miragic", "🎯 Starting single try-on")
            android.util.Log.d("Miragic", "Human URL: $humanImageUrl")
            android.util.Log.d("Miragic", "Cloth URL: $clothImageUrl")
            android.util.Log.d("Miragic", "Type: $garmentType")

            onProgress(TryOnResult.Loading)

            // Validate URLs
            if (humanImageUrl.isBlank() || clothImageUrl.isBlank()) {
                android.util.Log.e("Miragic", "❌ Empty URLs provided")
                onProgress(TryOnResult.Error("Invalid image URLs"))
                return null
            }

            // Download images using Coil
            val humanFile = downloadImageWithCoil(humanImageUrl, "human_temp.jpg")
            val clothFile = downloadImageWithCoil(clothImageUrl, "cloth_temp.jpg")

            if (humanFile == null || clothFile == null) {
                android.util.Log.e("Miragic", "❌ Failed to download images")
                onProgress(TryOnResult.Error("Failed to download images"))
                return null
            }

            android.util.Log.d("Miragic", "✅ Images downloaded successfully")
            android.util.Log.d("Miragic", "Human size: ${humanFile.length() / 1024}KB")
            android.util.Log.d("Miragic", "Cloth size: ${clothFile.length() / 1024}KB")

            // Create multipart request
            val humanPart = createMultipartPart(humanFile, "humanImage")
            val clothPart = createMultipartPart(clothFile, "clothImage")
            val garmentTypePart = garmentType.toRequestBody("text/plain".toMediaTypeOrNull())

            android.util.Log.d("Miragic", "📤 Sending request to Miragic API...")

            // Create job
            val response = apiService.createVirtualTryOn(
                apiKey = apiKey,
                garmentType = garmentTypePart,
                humanImage = humanPart,
                clothImage = clothPart
            )

            if (!response.isSuccessful) {
                val errorBody = response.errorBody()?.string()
                android.util.Log.e("Miragic", "❌ API Error: ${response.code()} - $errorBody")
                onProgress(TryOnResult.Error("API Error: ${response.code()}"))
                return null
            }

            if (response.body()?.success != true) {
                android.util.Log.e("Miragic", "❌ Job creation failed")
                onProgress(TryOnResult.Error("Failed to create try-on job"))
                return null
            }

            val jobId = response.body()?.data?.jobId
            if (jobId == null) {
                android.util.Log.e("Miragic", "❌ No job ID received")
                onProgress(TryOnResult.Error("No job ID received"))
                return null
            }

            android.util.Log.d("Miragic", "✅ Job created: $jobId")

            // Poll for result
            val resultUrl = pollJobStatus(jobId, onProgress)

            // Cleanup temp files
            humanFile.delete()
            clothFile.delete()

            resultUrl

        } catch (e: Exception) {
            android.util.Log.e("Miragic", "❌ Error: ${e.message}", e)
            onProgress(TryOnResult.Error(e.message ?: "Unknown error"))
            null
        }
    }

    /**
     * Combo try-on (top + bottom together)
     */
    suspend fun comboTryOn(
        humanImageUrl: String,
        topClothImageUrl: String,
        bottomClothImageUrl: String,
        onProgress: (TryOnResult) -> Unit
    ): String? {
        return try {
            android.util.Log.d("Miragic", "🎯 Starting combo try-on")
            android.util.Log.d("Miragic", "Human URL: $humanImageUrl")
            android.util.Log.d("Miragic", "Top URL: $topClothImageUrl")
            android.util.Log.d("Miragic", "Bottom URL: $bottomClothImageUrl")

            onProgress(TryOnResult.Loading)

            // Validate URLs
            if (humanImageUrl.isBlank() || topClothImageUrl.isBlank() || bottomClothImageUrl.isBlank()) {
                android.util.Log.e("Miragic", "❌ Empty URLs provided")
                onProgress(TryOnResult.Error("Invalid image URLs"))
                return null
            }

            // Download images
            val humanFile = downloadImageWithCoil(humanImageUrl, "human_temp.jpg")
            val topFile = downloadImageWithCoil(topClothImageUrl, "top_temp.jpg")
            val bottomFile = downloadImageWithCoil(bottomClothImageUrl, "bottom_temp.jpg")

            if (humanFile == null || topFile == null || bottomFile == null) {
                android.util.Log.e("Miragic", "❌ Failed to download images")
                onProgress(TryOnResult.Error("Failed to download images"))
                return null
            }

            android.util.Log.d("Miragic", "✅ All images downloaded")

            // Create multipart parts
            val humanPart = createMultipartPart(humanFile, "humanImage")
            val topPart = createMultipartPart(topFile, "clothImage")
            val bottomPart = createMultipartPart(bottomFile, "bottomClothImage")
            val garmentTypePart = "comb".toRequestBody("text/plain".toMediaTypeOrNull())

            android.util.Log.d("Miragic", "📤 Sending combo request...")

            // Create job
            val response = apiService.createVirtualTryOn(
                apiKey = apiKey,
                garmentType = garmentTypePart,
                humanImage = humanPart,
                clothImage = topPart,
                bottomClothImage = bottomPart
            )

            if (!response.isSuccessful) {
                val errorBody = response.errorBody()?.string()
                android.util.Log.e("Miragic", "❌ API Error: ${response.code()} - $errorBody")
                onProgress(TryOnResult.Error("API Error: ${response.code()}"))
                return null
            }

            if (response.body()?.success != true) {
                android.util.Log.e("Miragic", "❌ Combo job creation failed")
                onProgress(TryOnResult.Error("Failed to create combo try-on job"))
                return null
            }

            val jobId = response.body()?.data?.jobId
            if (jobId == null) {
                android.util.Log.e("Miragic", "❌ No job ID received")
                onProgress(TryOnResult.Error("No job ID received"))
                return null
            }

            android.util.Log.d("Miragic", "✅ Combo job created: $jobId")

            // Poll for result
            val resultUrl = pollJobStatus(jobId, onProgress)

            // Cleanup
            humanFile.delete()
            topFile.delete()
            bottomFile.delete()

            resultUrl

        } catch (e: Exception) {
            android.util.Log.e("Miragic", "❌ Combo error: ${e.message}", e)
            onProgress(TryOnResult.Error(e.message ?: "Unknown error"))
            null
        }
    }

    /**
     * Poll job status until completion
     */
    private suspend fun pollJobStatus(
        jobId: String,
        onProgress: (TryOnResult) -> Unit
    ): String? {
        var attempts = 0
        val maxAttempts = 60 // 2 minutes max (2 seconds * 60)

        while (attempts < maxAttempts) {
            delay(2000) // Wait 2 seconds between polls

            try {
                android.util.Log.d("Miragic", "⏳ Polling status... (attempt $attempts)")

                val statusResponse = apiService.getJobStatus(apiKey, jobId)

                if (!statusResponse.isSuccessful) {
                    android.util.Log.e("Miragic", "❌ Status check failed: ${statusResponse.code()}")
                    onProgress(TryOnResult.Error("Failed to check job status"))
                    return null
                }

                val statusData = statusResponse.body()?.data
                android.util.Log.d("Miragic", "Status: ${statusData?.status}")

                when (statusData?.status) {
                    "COMPLETED" -> {
                        android.util.Log.d("Miragic", "✅ Job completed!")
                        android.util.Log.d("Miragic", "Result URL: ${statusData.processedUrl}")
                        onProgress(TryOnResult.Success(statusData.processedUrl ?: ""))
                        return statusData.processedUrl
                    }
                    "FAILED" -> {
                        android.util.Log.e("Miragic", "❌ Job failed: ${statusData.errorMessage}")
                        onProgress(TryOnResult.Error(statusData.errorMessage ?: "Processing failed"))
                        return null
                    }
                    "PENDING" -> {
                        val progress = ((attempts.toFloat() / maxAttempts) * 100).toInt()
                        onProgress(TryOnResult.Processing(progress))
                        android.util.Log.d("Miragic", "⏳ Processing... ($progress%)")
                    }
                }

            } catch (e: Exception) {
                android.util.Log.e("Miragic", "❌ Poll error: ${e.message}")
            }

            attempts++
        }

        android.util.Log.e("Miragic", "❌ Processing timeout")
        onProgress(TryOnResult.Error("Processing timeout"))
        return null
    }

    /**
     * Download image using Coil (proper way for Android)
     */
    private suspend fun downloadImageWithCoil(url: String, filename: String): File? {
        return withContext(Dispatchers.IO) {
            try {
                android.util.Log.d("Miragic", "📥 Downloading: $url")

                val request = ImageRequest.Builder(context)
                    .data(url)
                    .allowHardware(false) // Required for bitmap manipulation
                    .build()

                val result = imageLoader.execute(request)

                if (result is SuccessResult) {
                    val bitmap = (result.drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap

                    if (bitmap == null) {
                        android.util.Log.e("Miragic", "❌ Failed to get bitmap from result")
                        return@withContext null
                    }

                    // Save to file
                    val file = File(context.cacheDir, filename)
                    FileOutputStream(file).use { out ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                    }

                    android.util.Log.d("Miragic", "✅ Downloaded: ${file.name} (${file.length() / 1024}KB)")
                    return@withContext file
                } else {
                    android.util.Log.e("Miragic", "❌ Image load failed for: $url")
                    return@withContext null
                }

            } catch (e: Exception) {
                android.util.Log.e("Miragic", "❌ Download error: ${e.message}", e)
                return@withContext null
            }
        }
    }

    /**
     * Create multipart body part from file
     */
    private fun createMultipartPart(file: File, partName: String): MultipartBody.Part {
        val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
        return MultipartBody.Part.createFormData(partName, file.name, requestFile)
    }
}
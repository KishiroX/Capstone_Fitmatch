package com.example.capstone.api

import com.google.gson.annotations.SerializedName

// Request body (not used for multipart, but kept for reference)
data class VirtualTryOnRequest(
    val garmentType: String,
    val humanImage: String,
    val clothImage: String,
    val bottomClothImage: String? = null
)

// Response when creating job
data class VirtualTryOnCreateResponse(
    val success: Boolean,
    val message: String,
    val data: JobData
)

data class JobData(
    val jobId: String,
    val status: String,
    val mode: String,
    val createdAt: String
)

// Response when checking status
data class VirtualTryOnStatusResponse(
    val success: Boolean,
    val data: JobStatusData
)

data class JobStatusData(
    val id: String,
    val status: String, // PENDING, COMPLETED, FAILED
    val mode: String,
    val humanImagePath: String?,
    val clothImagePath: String?,
    val bottomClothImagePath: String?,
    val resultImagePath: String?,
    val processedUrl: String?,
    val createdAt: String?,
    val processingStartedAt: String?,
    val processingCompletedAt: String?,
    val errorMessage: String?
)

// Try-on result state
sealed class TryOnResult {
    object Idle : TryOnResult()
    object Loading : TryOnResult()
    data class Processing(val progress: Int) : TryOnResult()
    data class Success(val imageUrl: String) : TryOnResult()
    data class Error(val message: String) : TryOnResult()
}
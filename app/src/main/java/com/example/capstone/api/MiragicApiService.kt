package com.example.capstone.api

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface MiragicApiService {

    @Multipart
    @POST("api/v1/virtual-try-on")
    suspend fun createVirtualTryOn(
        @Header("X-API-Key") apiKey: String,
        @Part("garmentType") garmentType: RequestBody,
        @Part humanImage: MultipartBody.Part,
        @Part clothImage: MultipartBody.Part,
        @Part bottomClothImage: MultipartBody.Part? = null
    ): Response<VirtualTryOnCreateResponse>

    @GET("api/v1/virtual-try-on/{jobId}")
    suspend fun getJobStatus(
        @Header("X-API-Key") apiKey: String,
        @Path("jobId") jobId: String
    ): Response<VirtualTryOnStatusResponse>
}
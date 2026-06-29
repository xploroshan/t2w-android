package com.taleson2wheels.app.data.remote.api

import com.taleson2wheels.app.data.remote.dto.UploadResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

/** `POST /api/v1/upload` — multipart image upload → public blob URL. */
interface UploadApi {

    @Multipart
    @POST("api/v1/upload")
    suspend fun upload(
        @Part file: MultipartBody.Part,
        @Part("type") type: RequestBody? = null,
    ): UploadResponse
}

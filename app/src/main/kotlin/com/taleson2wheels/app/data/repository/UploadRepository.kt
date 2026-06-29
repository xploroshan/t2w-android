package com.taleson2wheels.app.data.repository

import com.taleson2wheels.app.data.remote.ApiResult
import com.taleson2wheels.app.data.remote.api.UploadApi
import com.taleson2wheels.app.data.remote.safeApiCall
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

/** Uploads an image to `/api/v1/upload` and returns the public blob URL. */
class UploadRepository(
    private val uploadApi: UploadApi,
    private val json: Json,
) {
    suspend fun uploadImage(
        bytes: ByteArray,
        filename: String,
        mimeType: String,
        type: String = "misc",
    ): ApiResult<String> = safeApiCall(json) {
        val body = bytes.toRequestBody(mimeType.toMediaTypeOrNull(), 0, bytes.size)
        val part = MultipartBody.Part.createFormData("file", filename, body)
        val typePart = type.toRequestBody("text/plain".toMediaTypeOrNull())
        uploadApi.upload(part, typePart).url
    }
}

package com.taleson2wheels.app.data.remote

import kotlinx.serialization.json.Json
import retrofit2.HttpException
import java.io.IOException

/** Success or a typed failure — what repositories hand back to the UI layer. */
sealed interface ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>
    data class Failure(val error: ApiError) : ApiResult<Nothing>
}

inline fun <T> ApiResult<T>.onSuccess(block: (T) -> Unit): ApiResult<T> {
    if (this is ApiResult.Success) block(data)
    return this
}

inline fun <T> ApiResult<T>.onFailure(block: (ApiError) -> Unit): ApiResult<T> {
    if (this is ApiResult.Failure) block(error)
    return this
}

fun <T> ApiResult<T>.getOrNull(): T? = (this as? ApiResult.Success)?.data

/**
 * Run a suspending Retrofit call and fold every outcome into an [ApiResult],
 * translating Retrofit/OkHttp exceptions into the typed [ApiError] hierarchy so
 * callers never have to touch `try/catch`.
 */
suspend inline fun <T> safeApiCall(json: Json, crossinline block: suspend () -> T): ApiResult<T> =
    try {
        ApiResult.Success(block())
    } catch (e: HttpException) {
        ApiResult.Failure(e.toApiError(json))
    } catch (e: IOException) {
        ApiResult.Failure(ApiError.Network(e))
    } catch (e: Exception) {
        ApiResult.Failure(ApiError.Unexpected(e))
    }

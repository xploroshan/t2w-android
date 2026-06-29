package com.taleson2wheels.app.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import retrofit2.HttpException
import java.io.IOException

/**
 * The stable error envelope returned by every non-2xx `/api/v1` response:
 *
 *   { "error": { "code": "RIDE_FULL", "message": "...", "details": { } } }
 *
 * See `docs/openapi-v1.yaml` → components.schemas.ErrorResponse.
 */
@Serializable
data class ErrorEnvelope(
    @SerialName("error") val error: ApiErrorBody,
)

@Serializable
data class ApiErrorBody(
    val code: String,
    val message: String,
    val details: JsonObject? = null,
)

/** A normalized failure surfaced to repositories and the UI. */
sealed class ApiError {
    /** A structured HTTP failure carrying the server's error envelope. */
    data class Http(
        val status: Int,
        val code: String,
        val serverMessage: String,
    ) : ApiError()

    /** No usable response — offline, timeout, DNS, TLS, etc. */
    data class Network(val cause: IOException) : ApiError()

    /** Anything else (serialization mismatch, unexpected throwable). */
    data class Unexpected(val cause: Throwable) : ApiError()

    /** A human-friendly message safe to show in the UI. */
    val userMessage: String
        get() = when (this) {
            is Http -> serverMessage.ifBlank { "Request failed ($status)." }
            is Network -> "Network unavailable. Check your connection and try again."
            is Unexpected -> "Something went wrong. Please try again."
        }

    /** True for the 401 that the app handles by sending the user back to login. */
    val isUnauthorized: Boolean
        get() = this is Http && status == 401
}

/** Convert a Retrofit [HttpException] into a typed [ApiError.Http]. */
fun HttpException.toApiError(json: Json): ApiError.Http {
    val status = code()
    val raw = runCatching { response()?.errorBody()?.string() }.getOrNull()
    val envelope = raw
        ?.takeIf { it.isNotBlank() }
        ?.let { runCatching { json.decodeFromString<ErrorEnvelope>(it) }.getOrNull() }
    return ApiError.Http(
        status = status,
        code = envelope?.error?.code ?: "HTTP_$status",
        serverMessage = envelope?.error?.message ?: message(),
    )
}

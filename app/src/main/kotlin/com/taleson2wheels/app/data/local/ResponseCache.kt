package com.taleson2wheels.app.data.local

import com.taleson2wheels.app.data.remote.ApiError
import com.taleson2wheels.app.data.remote.ApiResult
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json

/**
 * Typed wrapper over a [CacheStore] that serializes DTOs with kotlinx and adds
 * the read-through / stale-fallback policy used by repositories.
 */
class ResponseCache(
    private val store: CacheStore,
    private val json: Json,
) {

    suspend fun <T> readOrNull(key: String, serializer: KSerializer<T>): T? {
        val raw = store.read(key) ?: return null
        // A schema change can leave undecodable blobs behind — treat as a miss.
        return runCatching { json.decodeFromString(serializer, raw) }.getOrNull()
    }

    suspend fun <T> save(key: String, value: T, serializer: KSerializer<T>) {
        runCatching { store.write(key, json.encodeToString(serializer, value)) }
    }

    /** Drop every cached response. Called at logout / forced-logout — a teardown
     *  path where a best-effort wipe is acceptable — so cached viewer-specific
     *  data doesn't linger; swallows failures so a cache hiccup can't break the
     *  sign-out itself. */
    suspend fun clear() {
        runCatching { store.clear() }
    }

    /** Like [clear] but RETHROWS on failure. Used at the login boundary so a new
     *  session is never established while the previous account's cached,
     *  viewer-specific rows might still be readable — the caller fails the sign-in
     *  rather than letting the new bearer token go live over a dirty cache. */
    suspend fun clearStrict() {
        store.clear()
    }

    /** Drop a single cached entry whose underlying data a local mutation has just
     *  made stale (e.g. the ride detail after the user registers). Best-effort. */
    suspend fun invalidate(key: String) {
        runCatching { store.delete(key) }
    }

    /**
     * Run [networkCall]; on success refresh the cache and return the fresh data.
     * On a *connectivity* failure (offline/timeout — [ApiError.Network]) fall back
     * to the last cached value if one exists. HTTP and unexpected failures are
     * propagated unchanged, so a 401/403/500 never gets masked by stale data.
     */
    suspend fun <T> networkWithFallback(
        key: String,
        serializer: KSerializer<T>,
        networkCall: suspend () -> ApiResult<T>,
    ): ApiResult<T> {
        return when (val result = networkCall()) {
            is ApiResult.Success -> {
                save(key, result.data, serializer)
                result
            }
            is ApiResult.Failure -> {
                if (result.error is ApiError.Network) {
                    readOrNull(key, serializer)?.let { return ApiResult.Success(it) }
                }
                result
            }
        }
    }
}

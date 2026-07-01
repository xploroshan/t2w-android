package com.taleson2wheels.app.data.repository

import com.taleson2wheels.app.data.remote.ApiResult
import com.taleson2wheels.app.data.remote.api.MapEditApi
import com.taleson2wheels.app.data.remote.dto.LiveBreak
import com.taleson2wheels.app.data.remote.dto.MapAddBreakRequest
import com.taleson2wheels.app.data.remote.dto.MapAuditEntry
import com.taleson2wheels.app.data.remote.dto.MapRevertRequest
import com.taleson2wheels.app.data.remote.dto.MapSmoothRequest
import com.taleson2wheels.app.data.remote.dto.MapSmoothResponse
import com.taleson2wheels.app.data.remote.dto.MapStatsSession
import com.taleson2wheels.app.data.remote.safeApiCall
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

/** Wraps the post-ride map-edit API; every method returns an [ApiResult]. */
class MapEditRepository(
    private val mapEditApi: MapEditApi,
    private val json: Json,
) {
    suspend fun addBreak(
        rideId: String,
        startedAt: String,
        endedAt: String?,
        reason: String?,
    ): ApiResult<LiveBreak> = safeApiCall(json) {
        mapEditApi.addBreak(rideId, MapAddBreakRequest(startedAt, endedAt, reason)).breakInfo
            ?: error("Missing break in response")
    }

    suspend fun deleteBreak(rideId: String, breakId: String): ApiResult<String> =
        safeApiCall(json) { mapEditApi.deleteBreak(rideId, breakId).id ?: breakId }

    /**
     * PATCH the stat overrides. `body` carries ONLY the fields the admin touched
     * (built by the ViewModel): a numeric value sets an override, an explicit JSON
     * null clears it, and an omitted key is left untouched server-side. Built as a
     * JsonObject because the app's Json has explicitNulls=false (a typed DTO would
     * drop the null-to-clear).
     */
    suspend fun updateStats(rideId: String, body: JsonObject): ApiResult<MapStatsSession> =
        safeApiCall(json) { mapEditApi.updateStats(rideId, body).session ?: MapStatsSession() }

    suspend fun smoothPreview(rideId: String, userId: String): ApiResult<MapSmoothResponse> =
        safeApiCall(json) { mapEditApi.smoothTrack(rideId, MapSmoothRequest(userId), preview = "1") }

    suspend fun smoothApply(rideId: String, userId: String): ApiResult<MapSmoothResponse> =
        safeApiCall(json) { mapEditApi.smoothTrack(rideId, MapSmoothRequest(userId)) }

    suspend fun revertSmooth(rideId: String, userId: String): ApiResult<Int> =
        safeApiCall(json) { mapEditApi.revertSmooth(rideId, MapRevertRequest(userId)).deleted }

    suspend fun audit(rideId: String): ApiResult<List<MapAuditEntry>> =
        safeApiCall(json) { mapEditApi.audit(rideId).edits }
}

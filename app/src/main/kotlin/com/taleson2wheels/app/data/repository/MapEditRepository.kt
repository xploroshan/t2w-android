package com.taleson2wheels.app.data.repository

import com.taleson2wheels.app.data.remote.ApiResult
import com.taleson2wheels.app.data.remote.api.MapEditApi
import com.taleson2wheels.app.data.remote.dto.LiveBreak
import com.taleson2wheels.app.data.remote.dto.MapAddBreakRequest
import com.taleson2wheels.app.data.remote.dto.MapAuditEntry
import com.taleson2wheels.app.data.remote.dto.MapDeleteTrackPointsRequest
import com.taleson2wheels.app.data.remote.dto.MapPlannedRouteRequest
import com.taleson2wheels.app.data.remote.dto.MapPlannedSession
import com.taleson2wheels.app.data.remote.dto.MapWaypoint
import com.taleson2wheels.app.data.remote.dto.MapRevertRequest
import com.taleson2wheels.app.data.remote.dto.MapSmoothRequest
import com.taleson2wheels.app.data.remote.dto.MapSmoothResponse
import com.taleson2wheels.app.data.remote.dto.MapGpxPlannedResponse
import com.taleson2wheels.app.data.remote.dto.MapGpxTrackResponse
import com.taleson2wheels.app.data.remote.dto.MapStatsSession
import com.taleson2wheels.app.data.remote.safeApiCall
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

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

    /**
     * Bulk-delete the rider's fixes in a time window: `after < recordedAt < before`.
     * Pass only [after] to trim the tail, only [before] to trim the head. Returns
     * the number of points removed.
     */
    suspend fun trimTrackPoints(
        rideId: String,
        userId: String,
        after: String?,
        before: String?,
    ): ApiResult<Int> = safeApiCall(json) {
        mapEditApi.deleteTrackPoints(
            rideId,
            MapDeleteTrackPointsRequest(userId = userId, after = after, before = before),
        ).deleted
    }

    /** Replace the planned-route overlay with [waypoints]. */
    suspend fun setPlannedRoute(rideId: String, waypoints: List<MapWaypoint>): ApiResult<MapPlannedSession> =
        safeApiCall(json) {
            mapEditApi.setPlannedRoute(rideId, MapPlannedRouteRequest(waypoints)).session ?: MapPlannedSession()
        }

    suspend fun importRecordedGpx(
        rideId: String,
        userId: String,
        filename: String,
        bytes: ByteArray,
    ): ApiResult<MapGpxTrackResponse> = safeApiCall(json) {
        mapEditApi.importRecordedGpx(rideId, gpxPart(filename, bytes), userId.toRequestBody(TEXT))
    }

    suspend fun importPlannedGpx(
        rideId: String,
        filename: String,
        bytes: ByteArray,
    ): ApiResult<MapGpxPlannedResponse> = safeApiCall(json) {
        mapEditApi.importPlannedGpx(rideId, gpxPart(filename, bytes))
    }

    private fun gpxPart(filename: String, bytes: ByteArray): MultipartBody.Part =
        MultipartBody.Part.createFormData("file", filename, bytes.toRequestBody(GPX_MEDIA))

    private companion object {
        val GPX_MEDIA = "application/gpx+xml".toMediaTypeOrNull()
        val TEXT = "text/plain".toMediaTypeOrNull()
    }
}

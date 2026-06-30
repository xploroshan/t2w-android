package com.taleson2wheels.app.data.repository

import com.taleson2wheels.app.data.remote.ApiResult
import com.taleson2wheels.app.data.remote.api.LiveApi
import com.taleson2wheels.app.data.remote.dto.LiveBreakRequest
import com.taleson2wheels.app.data.remote.dto.LiveBreakResponse
import com.taleson2wheels.app.data.remote.dto.LiveControlRequest
import com.taleson2wheels.app.data.remote.dto.LiveControlResponse
import com.taleson2wheels.app.data.remote.dto.LiveJoinResponse
import com.taleson2wheels.app.data.remote.dto.LiveMetrics
import com.taleson2wheels.app.data.remote.dto.LiveState
import com.taleson2wheels.app.data.remote.dto.LocationBatch
import com.taleson2wheels.app.data.remote.dto.LocationPoint
import com.taleson2wheels.app.data.remote.dto.LocationUploadResponse
import com.taleson2wheels.app.data.remote.safeApiCall
import kotlinx.serialization.json.Json

/** Live ride tracking over `/api/v1/rides/{id}/live/…`. */
class LiveRepository(
    private val liveApi: LiveApi,
    private val json: Json,
) {
    suspend fun state(rideId: String, since: String? = null): ApiResult<LiveState> =
        safeApiCall(json) { liveApi.state(rideId, since) }

    suspend fun metrics(rideId: String): ApiResult<LiveMetrics> =
        safeApiCall(json) { liveApi.metrics(rideId) }

    suspend fun join(rideId: String): ApiResult<LiveJoinResponse> =
        safeApiCall(json) { liveApi.join(rideId) }

    suspend fun control(rideId: String, action: String): ApiResult<LiveControlResponse> =
        safeApiCall(json) { liveApi.control(rideId, LiveControlRequest(action)) }

    suspend fun setBreak(rideId: String, action: String, reason: String? = null): ApiResult<LiveBreakResponse> =
        safeApiCall(json) { liveApi.breakControl(rideId, LiveBreakRequest(action, reason)) }

    suspend fun uploadLocations(rideId: String, points: List<LocationPoint>): ApiResult<LocationUploadResponse> =
        safeApiCall(json) { liveApi.uploadLocations(rideId, LocationBatch(points)) }
}

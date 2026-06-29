package com.taleson2wheels.app.data.repository

import com.taleson2wheels.app.data.remote.ApiResult
import com.taleson2wheels.app.data.remote.api.RidesApi
import com.taleson2wheels.app.data.remote.dto.Page
import com.taleson2wheels.app.data.remote.dto.RideDto
import com.taleson2wheels.app.data.remote.dto.RideRegistrationDto
import com.taleson2wheels.app.data.remote.dto.RideRegistrationRequest
import com.taleson2wheels.app.data.remote.safeApiCall
import kotlinx.serialization.json.Json

/** Reads rides and submits registrations against `/api/v1/rides`. */
class RidesRepository(
    private val ridesApi: RidesApi,
    private val json: Json,
) {
    suspend fun rides(
        cursor: String? = null,
        limit: Int = 20,
        status: String? = null,
    ): ApiResult<Page<RideDto>> =
        safeApiCall(json) { ridesApi.list(cursor = cursor, limit = limit, status = status) }

    suspend fun ride(id: String): ApiResult<RideDto> =
        safeApiCall(json) { ridesApi.detail(id) }

    suspend fun register(
        rideId: String,
        body: RideRegistrationRequest,
    ): ApiResult<RideRegistrationDto> =
        safeApiCall(json) { ridesApi.register(rideId, body) }
}

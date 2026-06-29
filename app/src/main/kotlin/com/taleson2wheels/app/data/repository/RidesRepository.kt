package com.taleson2wheels.app.data.repository

import com.taleson2wheels.app.data.remote.ApiResult
import com.taleson2wheels.app.data.remote.api.RidesApi
import com.taleson2wheels.app.data.remote.dto.Page
import com.taleson2wheels.app.data.remote.dto.RideCard
import com.taleson2wheels.app.data.remote.dto.RideDetail
import com.taleson2wheels.app.data.remote.safeApiCall
import kotlinx.serialization.json.Json

/** Reads the cursor-paginated ride list and ride detail from `/api/v1/rides`. */
class RidesRepository(
    private val ridesApi: RidesApi,
    private val json: Json,
) {
    suspend fun rides(
        cursor: String? = null,
        limit: Int = 20,
        status: String? = null,
    ): ApiResult<Page<RideCard>> =
        safeApiCall(json) { ridesApi.list(cursor = cursor, limit = limit, status = status) }

    suspend fun ride(id: String): ApiResult<RideDetail> =
        safeApiCall(json) { ridesApi.detail(id).ride }
}

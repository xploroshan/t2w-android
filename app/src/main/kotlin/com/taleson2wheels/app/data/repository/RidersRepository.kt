package com.taleson2wheels.app.data.repository

import com.taleson2wheels.app.data.remote.ApiResult
import com.taleson2wheels.app.data.remote.api.RidersApi
import com.taleson2wheels.app.data.remote.dto.Page
import com.taleson2wheels.app.data.remote.dto.RiderDto
import com.taleson2wheels.app.data.remote.safeApiCall
import kotlinx.serialization.json.Json

/** Reads the leaderboard and rider profiles from `/api/v1/riders`. */
class RidersRepository(
    private val ridersApi: RidersApi,
    private val json: Json,
) {
    suspend fun leaderboard(
        cursor: String? = null,
        limit: Int = 20,
        search: String? = null,
        period: String = "all",
        includeMerged: Boolean = false,
    ): ApiResult<Page<RiderDto>> = safeApiCall(json) {
        ridersApi.leaderboard(
            cursor = cursor,
            limit = limit,
            search = search,
            period = period,
            includeMerged = includeMerged,
        )
    }

    suspend fun profile(id: String): ApiResult<RiderDto> =
        safeApiCall(json) { ridersApi.profile(id).rider }
}

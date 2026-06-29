package com.taleson2wheels.app.data.remote.api

import com.taleson2wheels.app.data.remote.dto.BadgesResponse
import com.taleson2wheels.app.data.remote.dto.CrewResponse
import com.taleson2wheels.app.data.remote.dto.GuidelinesResponse
import com.taleson2wheels.app.data.remote.dto.HealthDto
import com.taleson2wheels.app.data.remote.dto.NotificationsResponse
import com.taleson2wheels.app.data.remote.dto.StatsDto
import kotlinx.serialization.json.JsonObject
import retrofit2.http.GET

/** Read-only catalog + the health probe. All paths are implemented on `main`. */
interface ContentApi {

    @GET("api/v1/health")
    suspend fun health(): HealthDto

    @GET("api/v1/stats")
    suspend fun stats(): StatsDto

    @GET("api/v1/guidelines")
    suspend fun guidelines(): GuidelinesResponse

    @GET("api/v1/crew")
    suspend fun crew(): CrewResponse

    @GET("api/v1/badges")
    suspend fun badges(): BadgesResponse

    /** Shape is open-ended (`additionalProperties`); kept as a raw JSON object. */
    @GET("api/v1/achievements")
    suspend fun achievements(): JsonObject

    @GET("api/v1/notifications")
    suspend fun notifications(): NotificationsResponse
}

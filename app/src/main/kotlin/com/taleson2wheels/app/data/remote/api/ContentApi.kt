package com.taleson2wheels.app.data.remote.api

import com.taleson2wheels.app.data.remote.dto.AchievementsResponse
import com.taleson2wheels.app.data.remote.dto.BadgesResponse
import com.taleson2wheels.app.data.remote.dto.CrewResponse
import com.taleson2wheels.app.data.remote.dto.GuidelinesResponse
import com.taleson2wheels.app.data.remote.dto.HealthDto
import com.taleson2wheels.app.data.remote.dto.MarkReadRequest
import com.taleson2wheels.app.data.remote.dto.MarkReadResponse
import com.taleson2wheels.app.data.remote.dto.NotificationsResponse
import com.taleson2wheels.app.data.remote.dto.StatsDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT

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

    /** Period "arena" standings; open-ended server shape, stable subset modelled. */
    @GET("api/v1/achievements")
    suspend fun achievements(): AchievementsResponse

    @GET("api/v1/notifications")
    suspend fun notifications(): NotificationsResponse

    /** Mark notifications read. Empty/null `ids` marks all of the caller's rows. */
    @PUT("api/v1/notifications")
    suspend fun markNotificationsRead(@Body body: MarkReadRequest): MarkReadResponse
}

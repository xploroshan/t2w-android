package com.taleson2wheels.app.data.remote.api

import com.taleson2wheels.app.data.remote.dto.BadgeDto
import com.taleson2wheels.app.data.remote.dto.BlogPostDto
import com.taleson2wheels.app.data.remote.dto.DeviceDto
import com.taleson2wheels.app.data.remote.dto.DeviceRegistration
import com.taleson2wheels.app.data.remote.dto.GuidelineDto
import com.taleson2wheels.app.data.remote.dto.HealthDto
import com.taleson2wheels.app.data.remote.dto.ItemList
import com.taleson2wheels.app.data.remote.dto.NotificationDto
import com.taleson2wheels.app.data.remote.dto.Page
import com.taleson2wheels.app.data.remote.dto.RidePostDto
import com.taleson2wheels.app.data.remote.dto.StatsDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/** Read-mostly content + push device registration + the health probe. */
interface ContentApi {

    @GET("health")
    suspend fun health(): HealthDto

    @GET("stats")
    suspend fun stats(): StatsDto

    @GET("guidelines")
    suspend fun guidelines(): ItemList<GuidelineDto>

    @GET("badges")
    suspend fun badges(): ItemList<BadgeDto>

    @GET("blogs")
    suspend fun blogs(
        @Query("cursor") cursor: String? = null,
        @Query("limit") limit: Int = 20,
    ): Page<BlogPostDto>

    @GET("blogs/{id}")
    suspend fun blog(@Path("id") id: String): BlogPostDto

    @GET("ride-posts")
    suspend fun ridePosts(
        @Query("cursor") cursor: String? = null,
        @Query("limit") limit: Int = 20,
        @Query("rideId") rideId: String? = null,
    ): Page<RidePostDto>

    @GET("notifications")
    suspend fun notifications(
        @Query("cursor") cursor: String? = null,
        @Query("limit") limit: Int = 20,
    ): Page<NotificationDto>

    @POST("notifications/{id}/read")
    suspend fun markNotificationRead(@Path("id") id: String)

    @POST("devices")
    suspend fun registerDevice(@Body body: DeviceRegistration): DeviceDto

    @DELETE("devices/{id}")
    suspend fun deregisterDevice(@Path("id") id: String)
}

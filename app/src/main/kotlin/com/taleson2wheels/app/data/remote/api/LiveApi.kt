package com.taleson2wheels.app.data.remote.api

import com.taleson2wheels.app.data.remote.dto.LiveAnalytics
import com.taleson2wheels.app.data.remote.dto.LiveBreakRequest
import com.taleson2wheels.app.data.remote.dto.LiveBreakResponse
import com.taleson2wheels.app.data.remote.dto.LiveControlRequest
import com.taleson2wheels.app.data.remote.dto.LiveControlResponse
import com.taleson2wheels.app.data.remote.dto.LiveJoinResponse
import com.taleson2wheels.app.data.remote.dto.LiveMetrics
import com.taleson2wheels.app.data.remote.dto.LiveState
import com.taleson2wheels.app.data.remote.dto.LocationBatch
import com.taleson2wheels.app.data.remote.dto.LocationUploadResponse
import kotlinx.serialization.json.JsonObject
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/** `/api/v1/rides/{id}/live/…` — live ride tracking (bearer required). */
interface LiveApi {

    @GET("api/v1/rides/{id}/live")
    suspend fun state(
        @Path("id") id: String,
        @Query("since") since: String? = null,
        // Admin-only: returns the full recorded path for an arbitrary rider in
        // `viewPath` (the map editor uses it to show/smooth a chosen rider's track).
        @Query("viewUserId") viewUserId: String? = null,
    ): LiveState

    @POST("api/v1/rides/{id}/live")
    suspend fun control(@Path("id") id: String, @Body body: LiveControlRequest): LiveControlResponse

    @POST("api/v1/rides/{id}/live/join")
    suspend fun join(@Path("id") id: String): LiveJoinResponse

    @POST("api/v1/rides/{id}/live/location")
    suspend fun uploadLocations(@Path("id") id: String, @Body body: LocationBatch): LocationUploadResponse

    @GET("api/v1/rides/{id}/live/metrics")
    suspend fun metrics(@Path("id") id: String): LiveMetrics

    @POST("api/v1/rides/{id}/live/break")
    suspend fun breakControl(@Path("id") id: String, @Body body: LiveBreakRequest): LiveBreakResponse

    /** Post-ride analytics — leaderboard modelled; other sections tolerated. */
    @GET("api/v1/rides/{id}/live/analytics")
    suspend fun analytics(@Path("id") id: String): LiveAnalytics

    /** Cached per-rider elevation profile — open-ended object. */
    @GET("api/v1/rides/{id}/live/elevation-profile")
    suspend fun elevationProfile(@Path("id") id: String, @Query("userId") userId: String): JsonObject
}

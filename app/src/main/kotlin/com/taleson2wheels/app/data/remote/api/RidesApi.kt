package com.taleson2wheels.app.data.remote.api

import com.taleson2wheels.app.data.remote.dto.LiveMetricsDto
import com.taleson2wheels.app.data.remote.dto.LocationBatch
import com.taleson2wheels.app.data.remote.dto.LocationBatchAck
import com.taleson2wheels.app.data.remote.dto.Page
import com.taleson2wheels.app.data.remote.dto.RideDto
import com.taleson2wheels.app.data.remote.dto.RideRegistrationDto
import com.taleson2wheels.app.data.remote.dto.RideRegistrationRequest
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/** `/api/v1/rides/…` including live-tracking sub-resources. */
interface RidesApi {

    @GET("rides")
    suspend fun list(
        @Query("cursor") cursor: String? = null,
        @Query("limit") limit: Int = 20,
        @Query("status") status: String? = null,
    ): Page<RideDto>

    @GET("rides/{id}")
    suspend fun detail(@Path("id") id: String): RideDto

    @POST("rides/{id}/register")
    suspend fun register(
        @Path("id") id: String,
        @Body body: RideRegistrationRequest,
    ): RideRegistrationDto

    @POST("rides/{id}/live/location")
    suspend fun uploadLocations(
        @Path("id") id: String,
        @Body body: LocationBatch,
    ): LocationBatchAck

    @GET("rides/{id}/live/metrics")
    suspend fun liveMetrics(@Path("id") id: String): LiveMetricsDto
}

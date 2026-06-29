package com.taleson2wheels.app.data.remote.api

import com.taleson2wheels.app.data.remote.dto.Page
import com.taleson2wheels.app.data.remote.dto.RideCard
import com.taleson2wheels.app.data.remote.dto.RideDetailResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/** `/api/v1/rides/…` — cursor-paginated list + detail (read surface, Phase 1). */
interface RidesApi {

    @GET("api/v1/rides")
    suspend fun list(
        @Query("cursor") cursor: String? = null,
        @Query("limit") limit: Int = 20,
        @Query("status") status: String? = null,
    ): Page<RideCard>

    @GET("api/v1/rides/{id}")
    suspend fun detail(@Path("id") id: String): RideDetailResponse
}

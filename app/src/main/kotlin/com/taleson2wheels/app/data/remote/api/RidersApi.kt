package com.taleson2wheels.app.data.remote.api

import com.taleson2wheels.app.data.remote.dto.Page
import com.taleson2wheels.app.data.remote.dto.RiderDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/** `/api/v1/riders/…` — leaderboard + rider profiles. */
interface RidersApi {

    @GET("riders")
    suspend fun leaderboard(
        @Query("cursor") cursor: String? = null,
        @Query("limit") limit: Int = 20,
        @Query("period") period: String = "all",
        @Query("search") search: String? = null,
    ): Page<RiderDto>

    @GET("riders/{id}")
    suspend fun profile(@Path("id") id: String): RiderDto
}

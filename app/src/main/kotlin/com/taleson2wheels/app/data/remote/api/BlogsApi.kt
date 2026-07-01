package com.taleson2wheels.app.data.remote.api

import com.taleson2wheels.app.data.remote.dto.BlogCard
import com.taleson2wheels.app.data.remote.dto.BlogInput
import com.taleson2wheels.app.data.remote.dto.BlogResponse
import com.taleson2wheels.app.data.remote.dto.LikeState
import com.taleson2wheels.app.data.remote.dto.Page
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/** `/api/v1/blogs` — public, cursor-paginated story feed + detail; create/like are bearer-gated. */
interface BlogsApi {

    @GET("api/v1/blogs")
    suspend fun list(
        @Query("cursor") cursor: String? = null,
        @Query("limit") limit: Int = 20,
        // Optional server-side filters. `type` = official|personal; an unknown value
        // is ignored server-side. `isVlog` narrows to vlogs (true) or written posts (false).
        @Query("type") type: String? = null,
        @Query("isVlog") isVlog: Boolean? = null,
    ): Page<BlogCard>

    @GET("api/v1/blogs/{id}")
    suspend fun detail(@Path("id") id: String): BlogResponse

    @POST("api/v1/blogs")
    suspend fun create(@Body body: BlogInput): BlogResponse

    /** Like a post (idempotent). Bearer required — 401 when anonymous, 404 when missing. */
    @POST("api/v1/blogs/{id}/like")
    suspend fun like(@Path("id") id: String): LikeState

    /** Remove the caller's like (idempotent). Bearer required. */
    @DELETE("api/v1/blogs/{id}/like")
    suspend fun unlike(@Path("id") id: String): LikeState
}

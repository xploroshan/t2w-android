package com.taleson2wheels.app.data.remote.api

import com.taleson2wheels.app.data.remote.dto.BlogCard
import com.taleson2wheels.app.data.remote.dto.BlogInput
import com.taleson2wheels.app.data.remote.dto.BlogResponse
import com.taleson2wheels.app.data.remote.dto.Page
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/** `/api/v1/blogs` — public, cursor-paginated story feed + detail; create is bearer-gated. */
interface BlogsApi {

    @GET("api/v1/blogs")
    suspend fun list(
        @Query("cursor") cursor: String? = null,
        @Query("limit") limit: Int = 20,
    ): Page<BlogCard>

    @GET("api/v1/blogs/{id}")
    suspend fun detail(@Path("id") id: String): BlogResponse

    @POST("api/v1/blogs")
    suspend fun create(@Body body: BlogInput): BlogResponse
}

package com.taleson2wheels.app.data.repository

import com.taleson2wheels.app.data.remote.ApiResult
import com.taleson2wheels.app.data.remote.api.BlogsApi
import com.taleson2wheels.app.data.remote.dto.BlogCard
import com.taleson2wheels.app.data.remote.dto.BlogInput
import com.taleson2wheels.app.data.remote.dto.Page
import com.taleson2wheels.app.data.remote.safeApiCall
import kotlinx.serialization.json.Json

/** Reads the cursor-paginated story feed and story detail from `/api/v1/blogs`, and creates posts. */
class BlogsRepository(
    private val blogsApi: BlogsApi,
    private val json: Json,
) {
    suspend fun blogs(cursor: String? = null, limit: Int = 20): ApiResult<Page<BlogCard>> =
        safeApiCall(json) { blogsApi.list(cursor = cursor, limit = limit) }

    suspend fun blog(id: String): ApiResult<BlogCard> =
        safeApiCall(json) { blogsApi.detail(id).blog }

    suspend fun createBlog(input: BlogInput): ApiResult<BlogCard> =
        safeApiCall(json) { blogsApi.create(input).blog }
}

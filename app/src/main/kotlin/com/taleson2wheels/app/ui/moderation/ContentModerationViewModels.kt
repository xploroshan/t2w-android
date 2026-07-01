package com.taleson2wheels.app.ui.moderation

import com.taleson2wheels.app.data.remote.ApiResult
import com.taleson2wheels.app.data.remote.dto.BlogCard
import com.taleson2wheels.app.data.remote.dto.Page
import com.taleson2wheels.app.data.remote.dto.RidePost
import com.taleson2wheels.app.data.repository.AdminRepository

/**
 * The core-member blog moderation queue: pending blog posts / vlogs to
 * approve/reject (`GET /api/v1/admin/blogs` + `POST .../{id}/moderate`).
 * Gated server-side by the canApproveContent toggle.
 */
class BlogModerationViewModel(
    private val adminRepository: AdminRepository,
) : ModerationQueueViewModel<BlogCard>() {

    override suspend fun fetchPage(cursor: String?): ApiResult<Page<BlogCard>> =
        adminRepository.blogs(status = STATUS, cursor = cursor, limit = PAGE_SIZE)

    override fun idOf(item: BlogCard): String = item.id

    override suspend fun applyModeration(id: String, approve: Boolean): ApiResult<*> =
        adminRepository.moderateBlog(id, approve)

    init {
        refresh()
    }

    private companion object {
        const val PAGE_SIZE = 20
        const val STATUS = "pending"
    }
}

/**
 * The core-member ride-post ("ride tales") moderation queue
 * (`GET /api/v1/admin/ride-posts` + `POST .../{id}/moderate`). Gated server-side
 * by the canApproveContent toggle.
 */
class RidePostModerationViewModel(
    private val adminRepository: AdminRepository,
) : ModerationQueueViewModel<RidePost>() {

    override suspend fun fetchPage(cursor: String?): ApiResult<Page<RidePost>> =
        adminRepository.ridePosts(status = STATUS, cursor = cursor, limit = PAGE_SIZE)

    override fun idOf(item: RidePost): String = item.id

    override suspend fun applyModeration(id: String, approve: Boolean): ApiResult<*> =
        adminRepository.moderateRidePost(id, approve)

    init {
        refresh()
    }

    private companion object {
        const val PAGE_SIZE = 20
        const val STATUS = "pending"
    }
}

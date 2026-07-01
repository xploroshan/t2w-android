package com.taleson2wheels.app.data.remote.api

import com.taleson2wheels.app.data.remote.dto.BlogCard
import com.taleson2wheels.app.data.remote.dto.BlogResponse
import com.taleson2wheels.app.data.remote.dto.ModerationAction
import com.taleson2wheels.app.data.remote.dto.Page
import com.taleson2wheels.app.data.remote.dto.RegistrationModeration
import com.taleson2wheels.app.data.remote.dto.RegistrationModerationResponse
import com.taleson2wheels.app.data.remote.dto.RidePost
import com.taleson2wheels.app.data.remote.dto.RidePostResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * The `/api/v1/admin` moderation endpoints — role-gated. Every call is
 * bearer-required and 403s unless the caller is a superadmin or a core_member
 * with the matching toggle (`canManageRegistrations` for registrations,
 * `canApproveContent` for blogs / ride posts).
 */
interface AdminApi {

    @GET("api/v1/admin/registrations")
    suspend fun registrations(
        @Query("status") status: String? = null,
        @Query("rideId") rideId: String? = null,
        @Query("cursor") cursor: String? = null,
        @Query("limit") limit: Int = 20,
    ): Page<RegistrationModeration>

    /** Approve or reject a registration (`{ action: "approve" | "reject" }`). */
    @POST("api/v1/admin/registrations/{id}")
    suspend fun moderateRegistration(
        @Path("id") id: String,
        @Body body: ModerationAction,
    ): RegistrationModerationResponse

    // ── Content moderation queues ───────────────────────────────────────────
    // Unlike the public feeds (approved-only), these surface pending/rejected
    // items so a core member can work the queue. Gated by canApproveContent.

    @GET("api/v1/admin/blogs")
    suspend fun moderationBlogs(
        @Query("status") status: String? = null,
        @Query("cursor") cursor: String? = null,
        @Query("limit") limit: Int = 20,
    ): Page<BlogCard>

    /** Approve or reject a blog post; wraps the updated row in `{ "blog": ... }`. */
    @POST("api/v1/admin/blogs/{id}/moderate")
    suspend fun moderateBlog(
        @Path("id") id: String,
        @Body body: ModerationAction,
    ): BlogResponse

    @GET("api/v1/admin/ride-posts")
    suspend fun moderationRidePosts(
        @Query("status") status: String? = null,
        @Query("rideId") rideId: String? = null,
        @Query("cursor") cursor: String? = null,
        @Query("limit") limit: Int = 20,
    ): Page<RidePost>

    /** Approve or reject a ride post ("ride tale"); wraps the row in `{ "post": ... }`. */
    @POST("api/v1/admin/ride-posts/{id}/moderate")
    suspend fun moderateRidePost(
        @Path("id") id: String,
        @Body body: ModerationAction,
    ): RidePostResponse
}

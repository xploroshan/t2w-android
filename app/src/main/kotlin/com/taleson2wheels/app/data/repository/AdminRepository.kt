package com.taleson2wheels.app.data.repository

import com.taleson2wheels.app.data.remote.ApiResult
import com.taleson2wheels.app.data.remote.api.AdminApi
import com.taleson2wheels.app.data.remote.dto.AdminUser
import com.taleson2wheels.app.data.remote.dto.BlockBody
import com.taleson2wheels.app.data.remote.dto.BlogCard
import com.taleson2wheels.app.data.remote.dto.DropOutBody
import com.taleson2wheels.app.data.remote.dto.DropOutResult
import com.taleson2wheels.app.data.remote.dto.ModerationAction
import com.taleson2wheels.app.data.remote.dto.Page
import com.taleson2wheels.app.data.remote.dto.ParticipationRow
import com.taleson2wheels.app.data.remote.dto.RegistrationModeration
import com.taleson2wheels.app.data.remote.dto.RideDetail
import com.taleson2wheels.app.data.remote.dto.RideInput
import com.taleson2wheels.app.data.remote.dto.RidePost
import com.taleson2wheels.app.data.remote.dto.RoleBody
import com.taleson2wheels.app.data.remote.dto.SetParticipationBody
import com.taleson2wheels.app.data.remote.dto.SetParticipationResult
import com.taleson2wheels.app.data.remote.safeApiCall
import kotlinx.serialization.json.Json

/** Reads the moderation queues and applies approve/reject actions (the `/api/v1/admin` endpoints). */
class AdminRepository(
    private val adminApi: AdminApi,
    private val json: Json,
) {
    suspend fun registrations(
        status: String? = null,
        rideId: String? = null,
        cursor: String? = null,
        limit: Int = 20,
    ): ApiResult<Page<RegistrationModeration>> =
        safeApiCall(json) { adminApi.registrations(status = status, rideId = rideId, cursor = cursor, limit = limit) }

    suspend fun moderateRegistration(id: String, approve: Boolean): ApiResult<RegistrationModeration> =
        safeApiCall(json) {
            adminApi.moderateRegistration(id, ModerationAction(if (approve) "approve" else "reject")).registration
        }

    // ── Content moderation ──────────────────────────────────────────────────

    suspend fun blogs(
        status: String? = "pending",
        cursor: String? = null,
        limit: Int = 20,
    ): ApiResult<Page<BlogCard>> =
        safeApiCall(json) { adminApi.moderationBlogs(status = status, cursor = cursor, limit = limit) }

    suspend fun moderateBlog(id: String, approve: Boolean): ApiResult<BlogCard> =
        safeApiCall(json) {
            adminApi.moderateBlog(id, ModerationAction(if (approve) "approve" else "reject")).blog
        }

    suspend fun ridePosts(
        status: String? = "pending",
        rideId: String? = null,
        cursor: String? = null,
        limit: Int = 20,
    ): ApiResult<Page<RidePost>> =
        safeApiCall(json) {
            adminApi.moderationRidePosts(status = status, rideId = rideId, cursor = cursor, limit = limit)
        }

    suspend fun moderateRidePost(id: String, approve: Boolean): ApiResult<RidePost> =
        safeApiCall(json) {
            adminApi.moderateRidePost(id, ModerationAction(if (approve) "approve" else "reject")).post
        }

    // ── User management ──────────────────────────────────────────────────────

    suspend fun users(
        status: String? = null,
        cursor: String? = null,
        limit: Int = 20,
    ): ApiResult<Page<AdminUser>> =
        safeApiCall(json) { adminApi.users(status = status, cursor = cursor, limit = limit) }

    suspend fun approveUser(id: String): ApiResult<AdminUser> =
        safeApiCall(json) { adminApi.approveUser(id).user }

    suspend fun rejectUser(id: String): ApiResult<String> =
        safeApiCall(json) { adminApi.rejectUser(id).id }

    suspend fun setUserBlocked(id: String, blocked: Boolean): ApiResult<AdminUser> =
        safeApiCall(json) { adminApi.blockUser(id, BlockBody(blocked)).user }

    suspend fun setUserRole(id: String, role: String): ApiResult<AdminUser> =
        safeApiCall(json) { adminApi.setUserRole(id, RoleBody(role)).user }

    // ── Ride CRUD ──────────────────────────────────────────────────────────────

    suspend fun createRide(input: RideInput): ApiResult<RideDetail> =
        safeApiCall(json) { adminApi.createRide(input).ride }

    suspend fun updateRide(id: String, input: RideInput): ApiResult<RideDetail> =
        safeApiCall(json) { adminApi.updateRide(id, input).ride }

    suspend fun deleteRide(id: String): ApiResult<String> =
        safeApiCall(json) { adminApi.deleteRide(id).id }

    // ── Per-ride participation ──────────────────────────────────────────────────

    suspend fun participation(rideId: String): ApiResult<List<ParticipationRow>> =
        safeApiCall(json) { adminApi.participation(rideId).items }

    /** Set a rider's points; pass [points] `0.0` (or negative) to remove, `null` to auto-award. */
    suspend fun setParticipationPoints(
        rideId: String,
        riderProfileId: String,
        points: Double?,
    ): ApiResult<SetParticipationResult> =
        safeApiCall(json) { adminApi.setParticipation(rideId, SetParticipationBody(riderProfileId, points)) }

    suspend fun setParticipationDroppedOut(
        rideId: String,
        riderProfileId: String,
        droppedOut: Boolean,
    ): ApiResult<DropOutResult> =
        safeApiCall(json) { adminApi.setParticipationDroppedOut(rideId, DropOutBody(riderProfileId, droppedOut)) }
}

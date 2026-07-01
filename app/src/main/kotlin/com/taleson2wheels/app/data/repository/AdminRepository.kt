package com.taleson2wheels.app.data.repository

import com.taleson2wheels.app.data.remote.ApiResult
import com.taleson2wheels.app.data.remote.api.AdminApi
import com.taleson2wheels.app.data.remote.dto.ModerationAction
import com.taleson2wheels.app.data.remote.dto.Page
import com.taleson2wheels.app.data.remote.dto.RegistrationModeration
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
}

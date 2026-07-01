package com.taleson2wheels.app.data.remote.api

import com.taleson2wheels.app.data.remote.dto.ModerationAction
import com.taleson2wheels.app.data.remote.dto.Page
import com.taleson2wheels.app.data.remote.dto.RegistrationModeration
import com.taleson2wheels.app.data.remote.dto.RegistrationModerationResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * The `/api/v1/admin` moderation endpoints — role-gated. Every call is
 * bearer-required and 403s unless the caller is a superadmin or a core_member
 * with the matching toggle (`canManageRegistrations` for registrations).
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
}

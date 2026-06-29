package com.taleson2wheels.app.data.remote.dto

import kotlinx.serialization.Serializable

// Shapes mirror T2W `docs/openapi-v1.yaml` (the implemented /api/v1 contract).

// ── Requests ───────────────────────────────────────────────────────────────

@Serializable
data class LoginRequest(
    val email: String,
    val password: String,
    val deviceId: String? = null,
    val platform: String = "android",
)

@Serializable
data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String,
    val phone: String? = null,
    val city: String? = null,
    val ridingExperience: String? = null,
    /** Optional starter bike (make), stored in the garage. */
    val motorcycle: String? = null,
    val deviceId: String? = null,
    val platform: String = "android",
)

@Serializable
data class RefreshRequest(
    val refreshToken: String,
    val deviceId: String? = null,
    val platform: String = "android",
)

@Serializable
data class EmailRequest(val email: String)

@Serializable
data class VerifyOtpRequest(val email: String, val code: String)

/** `/auth/reset-password` body — relies on a prior verified reset OTP. */
@Serializable
data class ResetPasswordRequest(val email: String, val newPassword: String)

@Serializable
data class ChangePasswordRequest(
    val currentPassword: String,
    val newPassword: String,
    val deviceId: String? = null,
    val platform: String = "android",
)

// ── Responses ──────────────────────────────────────────────────────────────

/** `{ success, verified?, emailSent? }` — shared shape of the OTP/reset routes. */
@Serializable
data class SimpleResponse(
    val success: Boolean = false,
    val verified: Boolean = false,
    val emailSent: Boolean = false,
)

/** `/auth/change-password` → fresh token pair (the tokenVersion bump revoked the old). */
@Serializable
data class ChangePasswordSuccess(
    val success: Boolean = true,
    val accessToken: String,
    val refreshToken: String,
    val refreshTokenExpiresAt: String,
)

/** `/auth/login` and `/auth/register` → token pair + the signed-in user. */
@Serializable
data class AuthSuccess(
    val accessToken: String,
    val refreshToken: String,
    /** ISO-8601 instant; opaque refresh token is rotating (~60 days). */
    val refreshTokenExpiresAt: String,
    val user: UserDto,
)

/** `/auth/refresh` → a fresh token pair (no user payload). */
@Serializable
data class RefreshSuccess(
    val accessToken: String,
    val refreshToken: String,
    val refreshTokenExpiresAt: String,
)

/** `/auth/me` wraps the user in `{ "user": ... }`. */
@Serializable
data class MeResponse(val user: UserDto)

/** `/auth/logout` → `{ "success": true }`. */
@Serializable
data class LogoutResponse(val success: Boolean = true)

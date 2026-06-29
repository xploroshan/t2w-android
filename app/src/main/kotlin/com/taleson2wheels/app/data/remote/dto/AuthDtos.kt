package com.taleson2wheels.app.data.remote.dto

import kotlinx.serialization.Serializable

// ── Requests ───────────────────────────────────────────────────────────────

@Serializable
data class LoginRequest(
    val email: String,
    val password: String,
    val deviceId: String? = null,
)

@Serializable
data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String,
    val phone: String? = null,
    val city: String? = null,
    val ridingExperience: String? = null,
    val deviceId: String? = null,
)

@Serializable
data class RefreshRequest(val refreshToken: String)

@Serializable
data class EmailRequest(val email: String)

@Serializable
data class VerifyOtpRequest(val email: String, val code: String)

@Serializable
data class ResetPasswordRequest(
    val email: String,
    val code: String,
    val password: String,
)

// ── Responses ──────────────────────────────────────────────────────────────

@Serializable
data class TokenPair(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Int = 900,
)

/** `/auth/login` and `/auth/register` — token pair plus the signed-in user. */
@Serializable
data class AuthSession(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Int = 900,
    val user: UserDto,
)

/** `/auth/me` wraps the user in a `{ "user": ... }` object. */
@Serializable
data class MeResponse(val user: UserDto)

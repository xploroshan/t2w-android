package com.taleson2wheels.app.data.repository

import com.taleson2wheels.app.data.remote.ApiResult
import com.taleson2wheels.app.data.remote.api.AuthApi
import com.taleson2wheels.app.data.remote.dto.ChangePasswordRequest
import com.taleson2wheels.app.data.remote.dto.EmailRequest
import com.taleson2wheels.app.data.remote.dto.LoginRequest
import com.taleson2wheels.app.data.remote.dto.RefreshRequest
import com.taleson2wheels.app.data.remote.dto.RegisterRequest
import com.taleson2wheels.app.data.remote.dto.ResetPasswordRequest
import com.taleson2wheels.app.data.remote.dto.UserDto
import com.taleson2wheels.app.data.remote.dto.VerifyOtpRequest
import com.taleson2wheels.app.data.remote.safeApiCall
import com.taleson2wheels.app.data.session.SessionStore
import com.taleson2wheels.app.data.session.Tokens
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.Json

/**
 * Owns the authentication lifecycle: signing in/registering (which persists the
 * token pair), exposing the reactive session, and signing out.
 */
class AuthRepository(
    private val authApi: AuthApi,
    private val session: SessionStore,
    private val json: Json,
    private val deviceId: String,
) {
    /** Reactive session — UI observes this to gate login vs. the app shell. */
    val tokens: StateFlow<Tokens?> = session.tokens

    suspend fun login(email: String, password: String): ApiResult<UserDto> =
        safeApiCall(json) {
            val res = authApi.login(
                LoginRequest(email = email.trim().lowercase(), password = password, deviceId = deviceId),
            )
            session.save(Tokens(res.accessToken, res.refreshToken))
            res.user
        }

    suspend fun register(
        name: String,
        email: String,
        password: String,
        phone: String? = null,
        city: String? = null,
        ridingExperience: String? = null,
    ): ApiResult<UserDto> = safeApiCall(json) {
        val res = authApi.register(
            RegisterRequest(
                name = name.trim(),
                email = email.trim().lowercase(),
                password = password,
                phone = phone,
                city = city,
                ridingExperience = ridingExperience,
                deviceId = deviceId,
            ),
        )
        session.save(Tokens(res.accessToken, res.refreshToken))
        res.user
    }

    suspend fun currentUser(): ApiResult<UserDto> =
        safeApiCall(json) { authApi.me().user }

    // ── Email verification (signup) ──────────────────────────────────────────

    suspend fun sendSignupOtp(email: String): ApiResult<Unit> =
        safeApiCall(json) { authApi.sendOtp(EmailRequest(email.trim().lowercase())); Unit }

    /** A bad/expired code surfaces as an [ApiResult.Failure] (the route returns 400). */
    suspend fun verifySignupOtp(email: String, code: String): ApiResult<Unit> =
        safeApiCall(json) { authApi.verifyOtp(VerifyOtpRequest(email.trim().lowercase(), code.trim())); Unit }

    // ── Password reset ───────────────────────────────────────────────────────

    suspend fun sendResetOtp(email: String): ApiResult<Unit> =
        safeApiCall(json) { authApi.sendResetOtp(EmailRequest(email.trim().lowercase())); Unit }

    suspend fun verifyResetOtp(email: String, code: String): ApiResult<Unit> =
        safeApiCall(json) { authApi.verifyResetOtp(VerifyOtpRequest(email.trim().lowercase(), code.trim())); Unit }

    suspend fun resetPassword(email: String, newPassword: String): ApiResult<Unit> =
        safeApiCall(json) { authApi.resetPassword(ResetPasswordRequest(email.trim().lowercase(), newPassword)); Unit }

    // ── Authenticated password change (re-issues this device's tokens) ───────

    suspend fun changePassword(currentPassword: String, newPassword: String): ApiResult<Unit> =
        safeApiCall(json) {
            val res = authApi.changePassword(
                ChangePasswordRequest(currentPassword = currentPassword, newPassword = newPassword, deviceId = deviceId),
            )
            session.save(Tokens(res.accessToken, res.refreshToken))
            Unit
        }

    /** Best-effort server-side revoke, then always clear the local session. */
    suspend fun logout() {
        session.peekRefreshToken()?.let { token ->
            runCatching { authApi.logout(RefreshRequest(token)) }
        }
        session.clear()
    }
}

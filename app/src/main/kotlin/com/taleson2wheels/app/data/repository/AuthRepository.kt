package com.taleson2wheels.app.data.repository

import com.taleson2wheels.app.data.remote.ApiResult
import com.taleson2wheels.app.data.remote.api.AuthApi
import com.taleson2wheels.app.data.remote.dto.ChangePasswordRequest
import com.taleson2wheels.app.data.remote.dto.EmailRequest
import com.taleson2wheels.app.data.remote.dto.LoginRequest
import com.taleson2wheels.app.data.remote.dto.RefreshRequest
import com.taleson2wheels.app.data.remote.dto.RegisterRequest
import com.taleson2wheels.app.data.remote.dto.ProfileUpdateRequest
import com.taleson2wheels.app.data.remote.dto.ResetPasswordRequest
import com.taleson2wheels.app.data.remote.dto.UserDto
import com.taleson2wheels.app.data.remote.dto.VerifyOtpRequest
import com.taleson2wheels.app.data.local.ResponseCache
import com.taleson2wheels.app.data.remote.safeApiCall
import com.taleson2wheels.app.data.push.NoOpPushTokenProvider
import com.taleson2wheels.app.data.push.PushTokenProvider
import com.taleson2wheels.app.data.session.SessionStore
import com.taleson2wheels.app.data.session.Tokens
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.Json

/**
 * Owns the authentication lifecycle: signing in/registering (which persists the
 * token pair), exposing the reactive session, and signing out. After a
 * successful auth it best-effort registers this device's push token, and
 * deregisters it on logout — both no-ops until a [PushTokenProvider] yields a
 * token (default [NoOpPushTokenProvider]).
 */
class AuthRepository(
    private val authApi: AuthApi,
    private val session: SessionStore,
    private val json: Json,
    private val deviceId: String,
    private val devicesRepository: DevicesRepository? = null,
    private val pushTokenProvider: PushTokenProvider = NoOpPushTokenProvider(),
    private val appBuild: String = "",
    private val responseCache: ResponseCache? = null,
) {
    /** Reactive session — UI observes this to gate login vs. the app shell. */
    val tokens: StateFlow<Tokens?> = session.tokens

    suspend fun login(email: String, password: String): ApiResult<UserDto> {
        val result = safeApiCall(json) {
            val res = authApi.login(
                LoginRequest(email = email.trim().lowercase(), password = password, deviceId = deviceId),
            )
            // Drop any prior account's cached, viewer-specific data BEFORE this
            // user's bearer token goes live (session.save) — and fail the sign-in
            // if it can't be cleared, rather than letting the new session read the
            // previous account's cached rows on a shared device.
            responseCache?.clearStrict()
            session.save(Tokens(res.accessToken, res.refreshToken))
            res.user
        }
        if (result is ApiResult.Success) {
            syncPushDevice()
        }
        return result
    }

    suspend fun register(
        name: String,
        email: String,
        password: String,
        phone: String? = null,
        city: String? = null,
        ridingExperience: String? = null,
    ): ApiResult<UserDto> {
        val result = safeApiCall(json) {
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
            // Clear any prior account's cached data before this session goes live
            // (see login()); a failed clear fails the registration rather than
            // leaking the previous user's cached rows.
            responseCache?.clearStrict()
            session.save(Tokens(res.accessToken, res.refreshToken))
            res.user
        }
        if (result is ApiResult.Success) {
            syncPushDevice()
        }
        return result
    }

    suspend fun currentUser(): ApiResult<UserDto> =
        safeApiCall(json) { authApi.me().user }

    suspend fun updateProfile(request: ProfileUpdateRequest): ApiResult<UserDto> =
        safeApiCall(json) { authApi.updateProfile(request).user }

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

    /** Best-effort server-side revoke, then always clear the local session and
     *  the offline cache (so the next account can't see this user's cached data). */
    suspend fun logout() {
        unsyncPushDevice()
        session.peekRefreshToken()?.let { token ->
            runCatching { authApi.logout(RefreshRequest(token)) }
        }
        session.clear()
        responseCache?.clear()
    }

    /** Register this device's push token after a successful auth (best-effort). */
    private suspend fun syncPushDevice() {
        val repo = devicesRepository ?: return
        val token = runCatching { pushTokenProvider.currentToken() }.getOrNull() ?: return
        runCatching { repo.register(token = token, deviceId = deviceId, appBuild = appBuild.ifBlank { null }) }
    }

    /** Deregister this device's push token on logout (best-effort, idempotent). */
    private suspend fun unsyncPushDevice() {
        val repo = devicesRepository ?: return
        val token = runCatching { pushTokenProvider.currentToken() }.getOrNull() ?: return
        runCatching { repo.deregister(token) }
    }
}

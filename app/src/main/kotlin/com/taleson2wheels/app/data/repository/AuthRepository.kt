package com.taleson2wheels.app.data.repository

import com.taleson2wheels.app.data.remote.ApiResult
import com.taleson2wheels.app.data.remote.api.AuthApi
import com.taleson2wheels.app.data.remote.dto.LoginRequest
import com.taleson2wheels.app.data.remote.dto.RefreshRequest
import com.taleson2wheels.app.data.remote.dto.RegisterRequest
import com.taleson2wheels.app.data.remote.dto.UserDto
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

    /** Best-effort server-side revoke, then always clear the local session. */
    suspend fun logout() {
        session.peekRefreshToken()?.let { token ->
            runCatching { authApi.logout(RefreshRequest(token)) }
        }
        session.clear()
    }
}

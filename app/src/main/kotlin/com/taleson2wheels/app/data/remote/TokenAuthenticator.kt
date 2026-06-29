package com.taleson2wheels.app.data.remote

import com.taleson2wheels.app.data.remote.api.AuthApi
import com.taleson2wheels.app.data.remote.dto.RefreshRequest
import com.taleson2wheels.app.data.session.SessionStore
import com.taleson2wheels.app.data.session.Tokens
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

/**
 * Transparently refreshes an expired access token on a `401`, then retries the
 * original request once.
 *
 * - Only acts on requests that actually carried a bearer token (so a failed
 *   login is reported to the caller, not retried).
 * - Serializes concurrent refreshes: if another thread already rotated the
 *   token while this request was failing, it retries with the new token instead
 *   of refreshing again.
 * - On refresh failure (invalid/expired/reuse-detected refresh token) it clears
 *   the session and gives up, sending the user back to login.
 *
 * [refreshApi] MUST be built on a client that has neither this authenticator nor
 * the [AuthInterceptor], otherwise a failing refresh would recurse.
 */
class TokenAuthenticator(
    private val session: SessionStore,
    private val refreshApi: AuthApi,
) : Authenticator {

    private val lock = Any()

    override fun authenticate(route: Route?, response: Response): Request? {
        // Never refresh for unauthenticated calls or for the refresh call itself.
        val failedAuth = response.request.header("Authorization") ?: return null
        if (response.request.url.encodedPath.endsWith("/auth/refresh")) return null
        // Bail out if we've already retried this request.
        if (priorResponseCount(response) >= 2) return null

        val failedToken = failedAuth.removePrefix("Bearer ").trim()

        synchronized(lock) {
            val current = session.peekAccessToken()
            // A concurrent request already refreshed — reuse its token.
            if (current != null && current != failedToken) {
                return response.request.retryWith(current)
            }

            val refreshToken = session.peekRefreshToken() ?: return null
            val newTokens = runCatching {
                runBlocking {
                    val pair = refreshApi.refresh(RefreshRequest(refreshToken))
                    Tokens(pair.accessToken, pair.refreshToken)
                }
            }.getOrNull()

            if (newTokens == null) {
                runBlocking { session.clear() }
                return null
            }

            runBlocking { session.save(newTokens) }
            return response.request.retryWith(newTokens.accessToken)
        }
    }

    private fun Request.retryWith(accessToken: String): Request =
        newBuilder().header("Authorization", "Bearer $accessToken").build()

    private fun priorResponseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }
}

package com.taleson2wheels.app.data.remote

import com.taleson2wheels.app.data.session.SessionStore
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Attaches `Authorization: Bearer <accessToken>` to every outgoing request that
 * doesn't already carry one. Unauthenticated calls (login, register, refresh)
 * simply go out without a header when no session exists.
 */
class AuthInterceptor(private val session: SessionStore) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val token = session.peekAccessToken()
        val authed = if (token != null && request.header("Authorization") == null) {
            request.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            request
        }
        return chain.proceed(authed)
    }
}

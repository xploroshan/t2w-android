package com.taleson2wheels.app.data.remote.api

import com.taleson2wheels.app.data.remote.dto.AuthSuccess
import com.taleson2wheels.app.data.remote.dto.LoginRequest
import com.taleson2wheels.app.data.remote.dto.LogoutResponse
import com.taleson2wheels.app.data.remote.dto.MeResponse
import com.taleson2wheels.app.data.remote.dto.RefreshRequest
import com.taleson2wheels.app.data.remote.dto.RefreshSuccess
import com.taleson2wheels.app.data.remote.dto.RegisterRequest
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/** `/api/v1/auth/…` — bearer-token auth flows. See T2W docs/openapi-v1.yaml. */
interface AuthApi {

    @POST("api/v1/auth/login")
    suspend fun login(@Body body: LoginRequest): AuthSuccess

    @POST("api/v1/auth/register")
    suspend fun register(@Body body: RegisterRequest): AuthSuccess

    /** Single-use, rotating. Called only by the token authenticator. */
    @POST("api/v1/auth/refresh")
    suspend fun refresh(@Body body: RefreshRequest): RefreshSuccess

    @POST("api/v1/auth/logout")
    suspend fun logout(@Body body: RefreshRequest): LogoutResponse

    @GET("api/v1/auth/me")
    suspend fun me(): MeResponse
}

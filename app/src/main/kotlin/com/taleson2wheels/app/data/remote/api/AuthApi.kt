package com.taleson2wheels.app.data.remote.api

import com.taleson2wheels.app.data.remote.dto.AuthSession
import com.taleson2wheels.app.data.remote.dto.EmailRequest
import com.taleson2wheels.app.data.remote.dto.LoginRequest
import com.taleson2wheels.app.data.remote.dto.MeResponse
import com.taleson2wheels.app.data.remote.dto.RefreshRequest
import com.taleson2wheels.app.data.remote.dto.RegisterRequest
import com.taleson2wheels.app.data.remote.dto.ResetPasswordRequest
import com.taleson2wheels.app.data.remote.dto.TokenPair
import com.taleson2wheels.app.data.remote.dto.VerifyOtpRequest
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/** `/api/v1/auth/…` — bearer-token auth flows. See docs/openapi-v1.yaml. */
interface AuthApi {

    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): AuthSession

    @POST("auth/register")
    suspend fun register(@Body body: RegisterRequest): AuthSession

    /** Single-use, rotating. Called only by the token authenticator. */
    @POST("auth/refresh")
    suspend fun refresh(@Body body: RefreshRequest): TokenPair

    @POST("auth/logout")
    suspend fun logout(@Body body: RefreshRequest)

    @POST("auth/send-otp")
    suspend fun sendOtp(@Body body: EmailRequest)

    @POST("auth/verify-otp")
    suspend fun verifyOtp(@Body body: VerifyOtpRequest)

    @POST("auth/send-reset-otp")
    suspend fun sendResetOtp(@Body body: EmailRequest)

    @POST("auth/verify-reset-otp")
    suspend fun verifyResetOtp(@Body body: VerifyOtpRequest)

    @POST("auth/reset-password")
    suspend fun resetPassword(@Body body: ResetPasswordRequest)

    @GET("auth/me")
    suspend fun me(): MeResponse
}

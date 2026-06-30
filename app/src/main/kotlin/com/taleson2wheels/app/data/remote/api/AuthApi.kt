package com.taleson2wheels.app.data.remote.api

import com.taleson2wheels.app.data.remote.dto.AuthSuccess
import com.taleson2wheels.app.data.remote.dto.ChangePasswordRequest
import com.taleson2wheels.app.data.remote.dto.ChangePasswordSuccess
import com.taleson2wheels.app.data.remote.dto.EmailRequest
import com.taleson2wheels.app.data.remote.dto.LoginRequest
import com.taleson2wheels.app.data.remote.dto.LogoutResponse
import com.taleson2wheels.app.data.remote.dto.MeResponse
import com.taleson2wheels.app.data.remote.dto.ProfileUpdateRequest
import com.taleson2wheels.app.data.remote.dto.RefreshRequest
import com.taleson2wheels.app.data.remote.dto.RefreshSuccess
import com.taleson2wheels.app.data.remote.dto.RegisterRequest
import com.taleson2wheels.app.data.remote.dto.ResetPasswordRequest
import com.taleson2wheels.app.data.remote.dto.SimpleResponse
import com.taleson2wheels.app.data.remote.dto.VerifyOtpRequest
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
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

    @PATCH("api/v1/auth/me")
    suspend fun updateProfile(@Body body: ProfileUpdateRequest): MeResponse

    // ── Account verification / recovery ──────────────────────────────────────

    @POST("api/v1/auth/send-otp")
    suspend fun sendOtp(@Body body: EmailRequest): SimpleResponse

    @POST("api/v1/auth/verify-otp")
    suspend fun verifyOtp(@Body body: VerifyOtpRequest): SimpleResponse

    @POST("api/v1/auth/send-reset-otp")
    suspend fun sendResetOtp(@Body body: EmailRequest): SimpleResponse

    @POST("api/v1/auth/verify-reset-otp")
    suspend fun verifyResetOtp(@Body body: VerifyOtpRequest): SimpleResponse

    @POST("api/v1/auth/reset-password")
    suspend fun resetPassword(@Body body: ResetPasswordRequest): SimpleResponse

    /** Bearer; returns a fresh token pair the client must persist. */
    @POST("api/v1/auth/change-password")
    suspend fun changePassword(@Body body: ChangePasswordRequest): ChangePasswordSuccess
}

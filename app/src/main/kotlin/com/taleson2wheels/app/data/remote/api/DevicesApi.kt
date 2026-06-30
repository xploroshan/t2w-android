package com.taleson2wheels.app.data.remote.api

import com.taleson2wheels.app.data.remote.dto.DeviceRegistration
import com.taleson2wheels.app.data.remote.dto.DeviceResponse
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.POST
import retrofit2.http.Path

/** `/api/v1/devices` — push-token registration for this device (bearer). */
interface DevicesApi {

    @POST("api/v1/devices")
    suspend fun register(@Body body: DeviceRegistration): DeviceResponse

    /** Idempotent deregistration by push token (e.g. on logout). */
    @DELETE("api/v1/devices/{token}")
    suspend fun deregister(@Path("token") token: String)
}

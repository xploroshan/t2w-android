package com.taleson2wheels.app.data.repository

import com.taleson2wheels.app.data.remote.ApiResult
import com.taleson2wheels.app.data.remote.api.DevicesApi
import com.taleson2wheels.app.data.remote.dto.Device
import com.taleson2wheels.app.data.remote.dto.DeviceRegistration
import com.taleson2wheels.app.data.remote.safeApiCall
import kotlinx.serialization.json.Json

/** Registers/deregisters this device's push token with `/api/v1/devices`. */
class DevicesRepository(
    private val devicesApi: DevicesApi,
    private val json: Json,
) {
    suspend fun register(token: String, deviceId: String, appBuild: String? = null): ApiResult<Device> =
        safeApiCall(json) {
            devicesApi.register(
                DeviceRegistration(token = token, platform = "android", deviceId = deviceId, appBuild = appBuild),
            ).device
        }

    suspend fun deregister(token: String): ApiResult<Unit> =
        safeApiCall(json) { devicesApi.deregister(token) }
}

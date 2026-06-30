package com.taleson2wheels.app.data.remote

import com.taleson2wheels.app.data.remote.dto.DeviceRegistration
import com.taleson2wheels.app.data.remote.dto.DeviceResponse
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Locks the push device-registration DTOs to the /api/v1/devices contract. */
class DeviceDtoTest {

    // Mirrors the app's converter Json (see AppContainer).
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        coerceInputValues = true
        encodeDefaults = true
        isLenient = true
    }

    @Test
    fun registration_defaults_platform_android_and_omits_null_appBuild() {
        val body = json.encodeToString(DeviceRegistration(token = "fcm-tok", deviceId = "dev-1"))
        assertTrue(body.contains("\"platform\":\"android\""))
        assertTrue(body.contains("\"token\":\"fcm-tok\""))
        assertTrue(body.contains("\"deviceId\":\"dev-1\""))
        // explicitNulls=false → the unset appBuild is omitted from the wire body.
        assertFalse(body.contains("appBuild"))
    }

    @Test
    fun decodes_device_response_envelope() {
        val res = json.decodeFromString<DeviceResponse>(
            """{ "device": { "id": "d1", "platform": "android", "deviceId": "dev-1", "appBuild": "0.1.0", "createdAt": "2026-06-30T00:00:00Z" } }""",
        )
        assertEquals("d1", res.device.id)
        assertEquals("android", res.device.platform)
        assertEquals("0.1.0", res.device.appBuild)
    }
}

package com.taleson2wheels.app.data.remote

import com.taleson2wheels.app.data.remote.dto.ChangePasswordRequest
import com.taleson2wheels.app.data.remote.dto.ChangePasswordSuccess
import com.taleson2wheels.app.data.remote.dto.SimpleResponse
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Locks the auth-recovery DTOs to the /api/v1 contract (send-otp/reset/change-password). */
class AuthDtoTest {

    // Mirrors the app's converter Json (see AppContainer).
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        coerceInputValues = true
        encodeDefaults = true
        isLenient = true
    }

    @Test
    fun simple_response_tolerates_partial_shapes() {
        // send-otp → { success }
        assertTrue(json.decodeFromString<SimpleResponse>("""{ "success": true }""").success)
        // verify-otp → { success, verified }
        val v = json.decodeFromString<SimpleResponse>("""{ "success": true, "verified": true }""")
        assertTrue(v.verified)
        // send-reset-otp → { success, emailSent }
        val r = json.decodeFromString<SimpleResponse>("""{ "success": true, "emailSent": false }""")
        assertTrue(r.success)
        assertFalse(r.emailSent)
    }

    @Test
    fun decodes_change_password_success_token_pair() {
        val res = json.decodeFromString<ChangePasswordSuccess>(
            """{ "success": true, "accessToken": "a.b.c", "refreshToken": "opaque", "refreshTokenExpiresAt": "2026-09-01T00:00:00Z" }""",
        )
        assertEquals("a.b.c", res.accessToken)
        assertEquals("opaque", res.refreshToken)
        assertEquals("2026-09-01T00:00:00Z", res.refreshTokenExpiresAt)
    }

    @Test
    fun change_password_request_defaults_platform_and_omits_nulls() {
        val body = json.encodeToString(ChangePasswordRequest(currentPassword = "old", newPassword = "newlongenough12"))
        assertTrue(body.contains("\"platform\":\"android\""))
        // explicitNulls=false → the unset deviceId is omitted from the wire body.
        assertFalse(body.contains("deviceId"))
    }
}

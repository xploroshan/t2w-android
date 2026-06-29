package com.taleson2wheels.app.data.remote

import com.taleson2wheels.app.data.remote.dto.AuthSession
import com.taleson2wheels.app.data.remote.dto.Page
import com.taleson2wheels.app.data.remote.dto.RideDto
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure-JVM tests that lock the Kotlin DTOs to the `docs/openapi-v1.yaml`
 * contract: the error envelope, cursor pages, and the auth session shape.
 */
class ContractParsingTest {

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        coerceInputValues = true
        isLenient = true
    }

    @Test
    fun decodes_error_envelope() {
        val body = """
            { "error": { "code": "RIDE_FULL", "message": "This ride is full.", "details": { "maxRiders": 25 } } }
        """.trimIndent()

        val envelope = json.decodeFromString<ErrorEnvelope>(body)

        assertEquals("RIDE_FULL", envelope.error.code)
        assertEquals("This ride is full.", envelope.error.message)
        assertEquals("25", envelope.error.details?.get("maxRiders")?.toString())
    }

    @Test
    fun http_error_exposes_user_message_and_unauthorized_flag() {
        val unauthorized = ApiError.Http(status = 401, code = "UNAUTHENTICATED", serverMessage = "Session expired")
        assertTrue(unauthorized.isUnauthorized)
        assertEquals("Session expired", unauthorized.userMessage)

        val conflict = ApiError.Http(status = 409, code = "RIDE_FULL", serverMessage = "")
        assertFalse(conflict.isUnauthorized)
        // Blank server messages fall back to a status-aware default.
        assertTrue(conflict.userMessage.contains("409"))
    }

    @Test
    fun decodes_cursor_page_and_tolerates_unknown_fields() {
        val body = """
            {
              "items": [
                { "id": "r1", "title": "Coorg Loop", "rideNumber": "T2W-101", "type": "weekend",
                  "status": "upcoming", "startDate": "2026-07-01T06:00:00Z", "endDate": "2026-07-02T18:00:00Z",
                  "distanceKm": 540.5, "maxRiders": 25, "fee": 1500, "futureField": "ignored" }
              ],
              "nextCursor": "eyJpZCI6InIxIn0="
            }
        """.trimIndent()

        val page = json.decodeFromString<Page<RideDto>>(body)

        assertEquals(1, page.items.size)
        assertEquals("Coorg Loop", page.items.first().title)
        assertEquals(540.5, page.items.first().distanceKm, 0.0001)
        assertEquals("eyJpZCI6InIxIn0=", page.nextCursor)
    }

    @Test
    fun last_page_has_null_next_cursor() {
        val page = json.decodeFromString<Page<RideDto>>("""{ "items": [] }""")
        assertTrue(page.items.isEmpty())
        assertNull(page.nextCursor)
    }

    @Test
    fun decodes_auth_session() {
        val body = """
            {
              "accessToken": "header.payload.sig",
              "refreshToken": "opaque-refresh",
              "expiresIn": 900,
              "user": { "id": "u1", "name": "Roshan", "email": "r@example.com", "role": "t2w_rider", "isApproved": true }
            }
        """.trimIndent()

        val session = json.decodeFromString<AuthSession>(body)

        assertEquals("header.payload.sig", session.accessToken)
        assertEquals("opaque-refresh", session.refreshToken)
        assertEquals(900, session.expiresIn)
        assertEquals("t2w_rider", session.user.role)
        assertTrue(session.user.isApproved)
    }
}

package com.taleson2wheels.app.data.remote

import com.taleson2wheels.app.data.remote.dto.AuthSuccess
import com.taleson2wheels.app.data.remote.dto.MeResponse
import com.taleson2wheels.app.data.remote.dto.Page
import com.taleson2wheels.app.data.remote.dto.RideCard
import com.taleson2wheels.app.data.remote.dto.RideDetailResponse
import com.taleson2wheels.app.data.remote.dto.StatsDto
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure-JVM tests that lock the Kotlin DTOs to the *implemented* `/api/v1`
 * contract on T2W `main` (docs/openapi-v1.yaml): the error envelope, cursor
 * pages of `RideCard`, the `AuthSuccess` shape, and the named response wrappers.
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
            { "error": { "code": "INVALID_CREDENTIALS", "message": "Wrong email or password.", "details": { "field": "password" } } }
        """.trimIndent()

        val envelope = json.decodeFromString<ErrorEnvelope>(body)

        assertEquals("INVALID_CREDENTIALS", envelope.error.code)
        assertEquals("Wrong email or password.", envelope.error.message)
        assertEquals("\"password\"", envelope.error.details?.get("field")?.toString())
    }

    @Test
    fun http_error_exposes_user_message_and_unauthorized_flag() {
        val unauthorized = ApiError.Http(status = 401, code = "UNAUTHENTICATED", serverMessage = "Session expired")
        assertTrue(unauthorized.isUnauthorized)
        assertEquals("Session expired", unauthorized.userMessage)

        val conflict = ApiError.Http(status = 409, code = "RIDE_FULL", serverMessage = "")
        assertFalse(conflict.isUnauthorized)
        assertTrue(conflict.userMessage.contains("409"))
    }

    @Test
    fun decodes_cursor_page_of_ride_cards_and_tolerates_unknown_fields() {
        val body = """
            {
              "items": [
                { "id": "r1", "title": "Coorg Loop", "rideNumber": "T2W-101", "type": "weekend",
                  "status": "upcoming", "startDate": "2026-07-01T06:00:00Z", "endDate": "2026-07-02T18:00:00Z",
                  "distanceKm": 540.5, "difficulty": "moderate", "fee": 1500,
                  "registeredRiders": 12, "activeRegistrations": 10, "myRegistrationStatus": "confirmed",
                  "serverOnlyField": "ignored" }
              ],
              "nextCursor": "eyJpZCI6InIxIn0="
            }
        """.trimIndent()

        val page = json.decodeFromString<Page<RideCard>>(body)

        assertEquals(1, page.items.size)
        val card = page.items.first()
        assertEquals("Coorg Loop", card.title)
        assertEquals(540.5, card.distanceKm, 0.0001)
        assertEquals(12, card.registeredRiders)
        assertEquals("confirmed", card.myRegistrationStatus)
        assertEquals("eyJpZCI6InIxIn0=", page.nextCursor)
    }

    @Test
    fun last_page_has_null_next_cursor() {
        val page = json.decodeFromString<Page<RideCard>>("""{ "items": [], "nextCursor": null }""")
        assertTrue(page.items.isEmpty())
        assertNull(page.nextCursor)
    }

    @Test
    fun decodes_auth_success() {
        val body = """
            {
              "accessToken": "header.payload.sig",
              "refreshToken": "opaque-refresh",
              "refreshTokenExpiresAt": "2026-08-28T00:00:00Z",
              "user": { "id": "u1", "name": "Roshan", "email": "r@example.com", "role": "t2w_rider",
                        "isApproved": true, "totalKm": 1234.5, "ridesCompleted": 9 }
            }
        """.trimIndent()

        val auth = json.decodeFromString<AuthSuccess>(body)

        assertEquals("header.payload.sig", auth.accessToken)
        assertEquals("opaque-refresh", auth.refreshToken)
        assertEquals("2026-08-28T00:00:00Z", auth.refreshTokenExpiresAt)
        assertEquals("t2w_rider", auth.user.role)
        assertTrue(auth.user.isApproved)
        assertEquals(9, auth.user.ridesCompleted)
    }

    @Test
    fun decodes_named_wrappers_and_stats_fields() {
        val ride = json.decodeFromString<RideDetailResponse>(
            """{ "ride": { "id": "r1", "title": "Spiti Expedition", "status": "completed",
                  "currentUserRegistered": true, "currentUserApprovalStatus": "confirmed" } }""",
        )
        assertEquals("r1", ride.ride.id)
        assertTrue(ride.ride.currentUserRegistered)

        val me = json.decodeFromString<MeResponse>(
            """{ "user": { "id": "u1", "name": "R", "email": "r@e.com", "role": "rider" } }""",
        )
        assertEquals("u1", me.user.id)

        val stats = json.decodeFromString<StatsDto>(
            """{ "activeRiders": 120, "ridesCompleted": 88, "kmsCovered": 250000, "countriesRidden": 6 }""",
        )
        assertEquals(120, stats.activeRiders)
        assertEquals(250000L, stats.kmsCovered)
        assertEquals(6, stats.countriesRidden)
    }
}

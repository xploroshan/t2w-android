package com.taleson2wheels.app.ui.admin

import com.taleson2wheels.app.data.remote.api.LiveApi
import com.taleson2wheels.app.data.remote.api.MapEditApi
import com.taleson2wheels.app.data.remote.dto.LiveBreak
import com.taleson2wheels.app.data.remote.dto.LiveMetrics
import com.taleson2wheels.app.data.remote.dto.LivePathPoint
import com.taleson2wheels.app.data.remote.dto.LiveRiderPosition
import com.taleson2wheels.app.data.remote.dto.LiveSession
import com.taleson2wheels.app.data.remote.dto.LiveState
import com.taleson2wheels.app.data.remote.dto.MapAddBreakRequest
import com.taleson2wheels.app.data.remote.dto.MapAuditEntry
import com.taleson2wheels.app.data.remote.dto.MapAuditResponse
import com.taleson2wheels.app.data.remote.dto.MapBreakResponse
import com.taleson2wheels.app.data.remote.dto.MapDeleteResponse
import com.taleson2wheels.app.data.remote.dto.MapDeletedResponse
import com.taleson2wheels.app.data.remote.dto.MapGpxPlannedResponse
import com.taleson2wheels.app.data.remote.dto.MapGpxTrackResponse
import com.taleson2wheels.app.data.remote.dto.MapRevertRequest
import com.taleson2wheels.app.data.remote.dto.MapSmoothRequest
import com.taleson2wheels.app.data.remote.dto.MapSmoothResponse
import com.taleson2wheels.app.data.remote.dto.MapStatsResponse
import com.taleson2wheels.app.data.remote.dto.MapStatsSession
import com.taleson2wheels.app.data.remote.dto.SmoothStats
import com.taleson2wheels.app.data.repository.LiveRepository
import com.taleson2wheels.app.data.repository.MapEditRepository
import com.taleson2wheels.app.testutil.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okio.Buffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MapEditorViewModelTest {

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val json = Json { ignoreUnknownKeys = true }

    private class FakeLiveApi : LiveApi {
        var lastViewUserId: String? = null
        override suspend fun state(id: String, since: String?, viewUserId: String?): LiveState {
            lastViewUserId = viewUserId
            val session = LiveSession(
                id = "s1", rideId = id, status = "ended", leadRiderId = "r1",
                plannedRoute = """[{"lat":12.9,"lng":77.5},{"lat":13.0,"lng":77.6}]""",
                breaks = listOf(LiveBreak(id = "b1", startedAt = "2026-06-01T09:00:00Z", endedAt = "2026-06-01T09:10:00Z", reason = "Fuel")),
            )
            val riders = listOf(
                LiveRiderPosition(userId = "r1", userName = "Alice", lat = 12.9, lng = 77.5, isLead = true),
                LiveRiderPosition(userId = "r2", userName = "Bob", lat = 12.95, lng = 77.55),
            )
            val lead = listOf(LivePathPoint(12.9, 77.5), LivePathPoint(12.95, 77.55))
            return if (viewUserId != null) {
                LiveState(session, riders, lead, viewPath = listOf(LivePathPoint(12.9, 77.5), LivePathPoint(12.91, 77.51), LivePathPoint(12.92, 77.52)))
            } else {
                LiveState(session, riders, lead)
            }
        }

        override suspend fun metrics(id: String) =
            LiveMetrics(distanceKm = 120.0, avgSpeedKmh = 45.0, maxSpeedKmh = 90.0, movingMinutes = 160.0)

        // Unused by the map editor.
        override suspend fun control(id: String, body: com.taleson2wheels.app.data.remote.dto.LiveControlRequest) = throw UnsupportedOperationException()
        override suspend fun join(id: String) = throw UnsupportedOperationException()
        override suspend fun uploadLocations(id: String, body: com.taleson2wheels.app.data.remote.dto.LocationBatch) = throw UnsupportedOperationException()
        override suspend fun breakControl(id: String, body: com.taleson2wheels.app.data.remote.dto.LiveBreakRequest) = throw UnsupportedOperationException()
        override suspend fun analytics(id: String) = throw UnsupportedOperationException()
        override suspend fun elevationProfile(id: String, userId: String) = throw UnsupportedOperationException()
    }

    private class FakeMapEditApi : MapEditApi {
        var lastStatsBody: JsonObject? = null
        var deletedBreakId: String? = null
        override suspend fun addBreak(id: String, body: MapAddBreakRequest) = MapBreakResponse(LiveBreak(id = "new"))
        override suspend fun deleteBreak(id: String, breakId: String): MapDeleteResponse {
            deletedBreakId = breakId
            return MapDeleteResponse(success = true, id = breakId)
        }
        override suspend fun updateStats(id: String, body: JsonObject): MapStatsResponse {
            lastStatsBody = body
            return MapStatsResponse(MapStatsSession(id = "s1"))
        }
        override suspend fun smoothTrack(id: String, body: MapSmoothRequest, preview: String?): MapSmoothResponse =
            if (preview == "1") {
                MapSmoothResponse(stats = SmoothStats(movedPercent = 60, gapsFilled = 2), points = JsonArray(List(3) { JsonPrimitive(0) }), preview = true)
            } else {
                MapSmoothResponse(points = JsonPrimitive(10))
            }
        override suspend fun revertSmooth(id: String, body: MapRevertRequest) = MapDeletedResponse(deleted = 5)
        var recordedGpxCalls = 0
        var recordedGpxUserId: String? = null
        override suspend fun importRecordedGpx(id: String, file: MultipartBody.Part, userId: RequestBody): MapGpxTrackResponse {
            recordedGpxCalls++
            recordedGpxUserId = Buffer().also { userId.writeTo(it) }.readUtf8()
            return MapGpxTrackResponse(inserted = 42, distanceKm = 12.3)
        }
        override suspend fun importPlannedGpx(id: String, file: MultipartBody.Part) = MapGpxPlannedResponse(waypointCount = 5)
        override suspend fun audit(id: String) =
            MapAuditResponse(listOf(MapAuditEntry(id = "a1", action = "track_smoothed", editedByName = "Admin", createdAt = "2026-06-01T08:00:00Z")))
    }

    private val live = FakeLiveApi()
    private val edit = FakeMapEditApi()

    private fun vm() = MapEditorViewModel(
        LiveRepository(live, json),
        MapEditRepository(edit, json),
        json,
    )

    @Test
    fun load_populates_session_riders_track_planned_metrics_and_audit() = runTest(mainDispatcher.dispatcher) {
        val m = vm(); m.load("ride-1"); advanceUntilIdle()

        val s = m.uiState
        assertTrue(s.editable)
        assertEquals("r1", s.selectedRiderId)
        assertEquals(2, s.riders.size)
        assertEquals(2, s.plannedRoute.size)            // parsed from session.plannedRoute
        assertEquals(3, s.recordedPath.size)            // the selected rider's viewPath
        assertEquals(120.0, s.metrics?.distanceKm)
        assertEquals(1, s.audit.size)
    }

    @Test
    fun previewSmooth_reports_stats_and_point_count() = runTest(mainDispatcher.dispatcher) {
        val m = vm(); m.load("ride-1"); advanceUntilIdle()

        m.previewSmooth(); advanceUntilIdle()

        assertEquals(60, m.uiState.smoothPreview?.movedPercent)
        assertEquals(3, m.uiState.smoothPreviewPoints)  // array length from the preview response
    }

    @Test
    fun saveStats_sends_only_the_touched_fields() = runTest(mainDispatcher.dispatcher) {
        val m = vm(); m.load("ride-1"); advanceUntilIdle()

        m.onStats { copy(distanceKm = "123.5", dirty = dirty + "distanceKmOverride") }
        m.saveStats(); advanceUntilIdle()

        val body = edit.lastStatsBody!!
        assertEquals(1, body.size)                      // ONLY the touched field
        assertEquals(123.5, body["distanceKmOverride"]!!.jsonPrimitive.double, 0.0)
        assertNull(body["avgSpeedKmhOverride"])         // untouched → omitted (left server-side)
        assertTrue(m.uiState.statsForm.dirty.isEmpty()) // cleared after a successful save
    }

    @Test
    fun saveStats_clears_only_the_sent_keys_so_a_saved_field_is_not_re_sent() = runTest(mainDispatcher.dispatcher) {
        val m = vm(); m.load("ride-1"); advanceUntilIdle()

        m.onStats { copy(distanceKm = "10", dirty = dirty + "distanceKmOverride") }
        m.saveStats(); advanceUntilIdle()
        assertTrue(m.uiState.statsForm.dirty.isEmpty())

        // A later edit + save must send ONLY the new field (distance is no longer dirty).
        m.onStats { copy(avgSpeedKmh = "40", dirty = dirty + "avgSpeedKmhOverride") }
        m.saveStats(); advanceUntilIdle()

        val body = edit.lastStatsBody!!
        assertEquals(1, body.size)
        assertEquals(40.0, body["avgSpeedKmhOverride"]!!.jsonPrimitive.double, 0.0)
        assertNull(body["distanceKmOverride"])
    }

    @Test
    fun saveStats_with_no_changes_does_not_call_the_api() = runTest(mainDispatcher.dispatcher) {
        val m = vm(); m.load("ride-1"); advanceUntilIdle()

        m.saveStats(); advanceUntilIdle()

        assertNull(edit.lastStatsBody)
    }

    @Test
    fun importRecordedGpx_uploads_for_the_selected_rider() = runTest(mainDispatcher.dispatcher) {
        val m = vm(); m.load("ride-1"); advanceUntilIdle()

        m.importRecordedGpx("<gpx/>".toByteArray(), "ride.gpx"); advanceUntilIdle()

        assertEquals(1, edit.recordedGpxCalls)
        assertEquals("r1", edit.recordedGpxUserId)      // the selected rider went in the userId part
        assertTrue(m.uiState.message!!.contains("42")) // "Imported 42 points…"
    }

    @Test
    fun importRecordedGpx_rejects_a_file_over_the_size_cap() = runTest(mainDispatcher.dispatcher) {
        val m = vm(); m.load("ride-1"); advanceUntilIdle()

        m.importRecordedGpx(ByteArray(5 * 1024 * 1024 + 1), "big.gpx"); advanceUntilIdle()

        assertEquals(0, edit.recordedGpxCalls)          // never hit the network
        assertNotNull(m.uiState.actionError)
    }

    @Test
    fun deleteBreak_calls_the_api() = runTest(mainDispatcher.dispatcher) {
        val m = vm(); m.load("ride-1"); advanceUntilIdle()

        m.deleteBreak("b1"); advanceUntilIdle()

        assertEquals("b1", edit.deletedBreakId)
    }
}

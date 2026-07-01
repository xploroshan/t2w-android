package com.taleson2wheels.app.ui.relive

import com.taleson2wheels.app.data.remote.api.LiveApi
import com.taleson2wheels.app.data.remote.dto.LiveBreakRequest
import com.taleson2wheels.app.data.remote.dto.LiveControlRequest
import com.taleson2wheels.app.data.remote.dto.LiveMetrics
import com.taleson2wheels.app.data.remote.dto.LivePathPoint
import com.taleson2wheels.app.data.remote.dto.LiveRiderPosition
import com.taleson2wheels.app.data.remote.dto.LiveSession
import com.taleson2wheels.app.data.remote.dto.LiveState
import com.taleson2wheels.app.data.remote.dto.LocationBatch
import com.taleson2wheels.app.data.repository.LiveRepository
import com.taleson2wheels.app.testutil.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReliveViewModelTest {

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val json = Json { ignoreUnknownKeys = true }

    private class FakeLiveApi(private val json: Json) : LiveApi {
        var fail = false
        var lastViewUserId: String? = null

        // r1: 3 fixes spanning 0–20s (durationMs = 20_000). r2: 2 fixes over 0–10s.
        override suspend fun state(id: String, since: String?, viewUserId: String?): LiveState {
            if (fail) throw RuntimeException("boom")
            lastViewUserId = viewUserId
            val session = LiveSession(
                id = "s1", rideId = id, status = "ended", leadRiderId = "r1",
                plannedRoute = """[{"lat":12.9,"lng":77.5},{"lat":13.0,"lng":77.6}]""",
            )
            val riders = listOf(
                LiveRiderPosition(userId = "r1", userName = "Alice", lat = 12.9, lng = 77.5, isLead = true),
                LiveRiderPosition(userId = "r2", userName = "Bob", lat = 12.95, lng = 77.55),
            )
            val lead = listOf(LivePathPoint(12.9, 77.5), LivePathPoint(12.95, 77.55))
            val viewPath = when (viewUserId) {
                "r1" -> listOf(
                    LivePathPoint(12.90, 77.50, "2026-06-01T09:00:00Z", speed = 10.0),
                    LivePathPoint(12.91, 77.51, "2026-06-01T09:00:10Z", speed = 15.0),
                    LivePathPoint(12.92, 77.52, "2026-06-01T09:00:20Z", speed = 20.0),
                )
                "r2" -> listOf(
                    LivePathPoint(13.00, 78.00, "2026-06-01T10:00:00Z"),
                    LivePathPoint(13.01, 78.01, "2026-06-01T10:00:10Z"),
                )
                else -> emptyList()
            }
            return LiveState(session, riders, lead, viewPath = viewPath)
        }

        override suspend fun metrics(id: String) =
            LiveMetrics(distanceKm = 42.0, avgSpeedKmh = 30.0, maxSpeedKmh = 72.0)

        override suspend fun elevationProfile(id: String, userId: String): JsonObject =
            json.parseToJsonElement(
                """{"profile":[{"altitude":100},{"altitude":150},{"altitude":220}]}""",
            ) as JsonObject

        // Unused by relive.
        override suspend fun control(id: String, body: LiveControlRequest) = throw UnsupportedOperationException()
        override suspend fun join(id: String) = throw UnsupportedOperationException()
        override suspend fun uploadLocations(id: String, body: LocationBatch) = throw UnsupportedOperationException()
        override suspend fun breakControl(id: String, body: LiveBreakRequest) = throw UnsupportedOperationException()
        override suspend fun analytics(id: String) = throw UnsupportedOperationException()
    }

    private val api = FakeLiveApi(json)
    private fun vm() = ReliveViewModel(LiveRepository(api, json), json)

    @Test
    fun load_selects_lead_and_builds_the_timeline() = runTest(mainDispatcher.dispatcher) {
        val m = vm(); m.load("ride-1"); advanceUntilIdle()

        val s = m.uiState
        assertFalse(s.isLoading)
        assertNull(s.loadError)
        assertEquals("r1", s.selectedRiderId)
        assertEquals(2, s.riders.size)
        assertEquals(3, s.track.size)
        assertEquals(20_000L, s.durationMs)          // 0–20s real timestamps
        assertTrue(s.hasTrack)
        assertEquals(2, s.plannedRoute.size)
        assertEquals(42.0, s.metrics?.distanceKm)
        assertEquals(3, s.elevations.size)
        // Positioned at the very start, paused.
        assertEquals(0L, s.playbackMs)
        assertFalse(s.isPlaying)
        assertNotNull(s.sample)
        assertEquals(12.90, s.sample!!.lat, 1e-9)
        assertEquals(100f, s.sample!!.elevationM!!, 1e-3f)
    }

    @Test
    fun load_failure_surfaces_an_error() = runTest(mainDispatcher.dispatcher) {
        api.fail = true
        val m = vm(); m.load("ride-1"); advanceUntilIdle()

        assertFalse(m.uiState.isLoading)
        assertNotNull(m.uiState.loadError)
        assertFalse(m.uiState.hasTrack)
    }

    @Test
    fun play_advances_to_the_end_then_stops() = runTest(mainDispatcher.dispatcher) {
        val m = vm(); m.load("ride-1"); advanceUntilIdle()

        m.play()
        advanceUntilIdle()   // ticker runs until playback reaches the end and self-pauses

        assertFalse(m.uiState.isPlaying)
        assertTrue(m.uiState.atEnd)
        assertEquals(20_000L, m.uiState.playbackMs)
        assertEquals(1f, m.uiState.progress, 1e-6f)
        assertEquals(12.92, m.uiState.sample!!.lat, 1e-9)
    }

    @Test
    fun pause_stops_advancing_playback() = runTest(mainDispatcher.dispatcher) {
        val m = vm(); m.load("ride-1"); advanceUntilIdle()

        m.play()
        advanceTimeBy(2_000L)   // let ~2s of playback elapse
        runCurrent()
        val mid = m.uiState.playbackMs
        assertTrue("expected some progress, was $mid", mid in 1L until 20_000L)

        m.pause()
        advanceTimeBy(5_000L)
        runCurrent()
        assertFalse(m.uiState.isPlaying)
        assertEquals(mid, m.uiState.playbackMs)  // frozen after pause
    }

    @Test
    fun higher_speed_multiplier_advances_playback_faster() = runTest(mainDispatcher.dispatcher) {
        val m = vm(); m.load("ride-1"); advanceUntilIdle()

        m.setSpeed(ReliveSpeed.X8)
        assertEquals(ReliveSpeed.X8, m.uiState.speed)
        m.play()
        advanceTimeBy(1_000L)   // 1s wall-clock at 8× ≈ 8s of track
        runCurrent()

        assertTrue("expected >4s of track, was ${m.uiState.playbackMs}", m.uiState.playbackMs > 4_000L)
    }

    @Test
    fun seekToProgress_jumps_and_pauses() = runTest(mainDispatcher.dispatcher) {
        val m = vm(); m.load("ride-1"); advanceUntilIdle()

        m.seekToProgress(0.5f)
        assertEquals(10_000L, m.uiState.playbackMs)
        assertFalse(m.uiState.isPlaying)
        assertEquals(0.5f, m.uiState.progress, 1e-6f)
        // Halfway between point 0 (12.90) and point 1 (12.91) at t=10s → exactly point 1.
        assertEquals(12.91, m.uiState.sample!!.lat, 1e-9)
    }

    @Test
    fun play_from_the_end_restarts_from_the_beginning() = runTest(mainDispatcher.dispatcher) {
        val m = vm(); m.load("ride-1"); advanceUntilIdle()

        m.seekTo(20_000L)
        assertTrue(m.uiState.atEnd)

        m.play()
        advanceTimeBy(300L)
        runCurrent()
        // Rewound to the start on play and resumed advancing from there.
        assertTrue(m.uiState.isPlaying)
        assertTrue("expected mid-track, was ${m.uiState.playbackMs}", m.uiState.playbackMs in 1L until 20_000L)
    }

    @Test
    fun selectRider_switches_track_and_resets_playback() = runTest(mainDispatcher.dispatcher) {
        val m = vm(); m.load("ride-1"); advanceUntilIdle()
        m.seekToProgress(1f)  // move away from start

        m.selectRider("r2"); advanceUntilIdle()

        assertEquals("r2", m.uiState.selectedRiderId)
        assertEquals("r2", api.lastViewUserId)
        assertEquals(2, m.uiState.track.size)
        assertEquals(10_000L, m.uiState.durationMs)
        assertEquals(0L, m.uiState.playbackMs)   // reset to start on switch
        assertFalse(m.uiState.isPlaying)
        assertFalse(m.uiState.switchingRider)
        assertEquals(13.00, m.uiState.sample!!.lat, 1e-9)
    }

    @Test
    fun selectRider_ignores_reselecting_the_current_rider() = runTest(mainDispatcher.dispatcher) {
        val m = vm(); m.load("ride-1"); advanceUntilIdle()
        m.seekToProgress(0.5f)
        val before = m.uiState.playbackMs

        m.selectRider("r1"); advanceUntilIdle()   // already selected → no-op

        assertEquals(before, m.uiState.playbackMs)
    }
}

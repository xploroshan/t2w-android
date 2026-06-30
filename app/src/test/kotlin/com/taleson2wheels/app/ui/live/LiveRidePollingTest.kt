package com.taleson2wheels.app.ui.live

import com.taleson2wheels.app.data.remote.api.LiveApi
import com.taleson2wheels.app.data.remote.dto.LiveAnalytics
import com.taleson2wheels.app.data.remote.dto.LiveBreakRequest
import com.taleson2wheels.app.data.remote.dto.LiveBreakResponse
import com.taleson2wheels.app.data.remote.dto.LiveControlRequest
import com.taleson2wheels.app.data.remote.dto.LiveControlResponse
import com.taleson2wheels.app.data.remote.dto.LiveJoinResponse
import com.taleson2wheels.app.data.remote.dto.LiveMetrics
import com.taleson2wheels.app.data.remote.dto.LiveState
import com.taleson2wheels.app.data.remote.dto.LocationBatch
import com.taleson2wheels.app.data.remote.dto.LocationUploadResponse
import com.taleson2wheels.app.data.repository.LiveRepository
import com.taleson2wheels.app.testutil.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Verifies the live-ride poll loop is foreground-gated: [LiveRideViewModel.start]
 * polls, [LiveRideViewModel.stop] (driven by the screen's ON_STOP) halts it, and a
 * later start resumes — so the 5s loop doesn't keep hitting the server while the
 * screen is backgrounded or buried under another destination.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LiveRidePollingTest {

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private class FakeLiveApi : LiveApi {
        var stateCalls = 0
        override suspend fun state(id: String, since: String?): LiveState {
            stateCalls++
            return LiveState()
        }
        override suspend fun metrics(id: String): LiveMetrics = LiveMetrics()
        override suspend fun control(id: String, body: LiveControlRequest): LiveControlResponse = error("unused")
        override suspend fun join(id: String): LiveJoinResponse = error("unused")
        override suspend fun uploadLocations(id: String, body: LocationBatch): LocationUploadResponse = error("unused")
        override suspend fun breakControl(id: String, body: LiveBreakRequest): LiveBreakResponse = error("unused")
        override suspend fun analytics(id: String): LiveAnalytics = error("unused")
        override suspend fun elevationProfile(id: String, userId: String): JsonObject = error("unused")
    }

    private fun vm(api: FakeLiveApi) =
        LiveRideViewModel(LiveRepository(api, Json { ignoreUnknownKeys = true }))

    @Test
    fun polling_stops_on_stop_and_resumes_on_start() = runTest(mainDispatcher.dispatcher) {
        val api = FakeLiveApi()
        val model = vm(api)

        model.start("r1")
        runCurrent() // immediate refresh
        advanceTimeBy(12_000) // two more 5s ticks (t=5s, t=10s)
        runCurrent()
        val whileRunning = api.stateCalls
        assertTrue("polling should be issuing requests while started", whileRunning >= 3)

        // Screen leaves the foreground.
        model.stop()
        advanceTimeBy(30_000)
        runCurrent()
        assertEquals("no polling must happen after stop()", whileRunning, api.stateCalls)

        // Screen returns to the foreground.
        model.start("r1")
        runCurrent()
        assertTrue("polling must resume on the next start()", api.stateCalls > whileRunning)

        // Stop so the infinite poll loop doesn't outlive the test (runTest's
        // end-of-test advanceUntilIdle would otherwise spin on it forever).
        model.stop()
    }

    @Test
    fun start_is_idempotent_while_already_polling() = runTest(mainDispatcher.dispatcher) {
        val api = FakeLiveApi()
        val model = vm(api)

        model.start("r1")
        runCurrent()
        val afterFirst = api.stateCalls

        // A second start() while the loop is live must not spin up a second loop.
        model.start("r1")
        runCurrent()
        assertEquals("a redundant start must not double the polling", afterFirst, api.stateCalls)

        model.stop()
    }
}

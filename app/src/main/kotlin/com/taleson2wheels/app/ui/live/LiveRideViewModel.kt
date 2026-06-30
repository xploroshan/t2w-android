package com.taleson2wheels.app.ui.live

import android.location.Location
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taleson2wheels.app.data.location.LocationTracker
import com.taleson2wheels.app.data.remote.ApiResult
import com.taleson2wheels.app.data.remote.dto.LiveMetrics
import com.taleson2wheels.app.data.remote.dto.LiveRiderPosition
import com.taleson2wheels.app.data.remote.dto.LiveState
import com.taleson2wheels.app.data.remote.dto.LocationPoint
import com.taleson2wheels.app.data.repository.LiveRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant

data class LiveUiState(
    val isLoading: Boolean = true,
    val liveState: LiveState? = null,
    val metrics: LiveMetrics? = null,
    val error: String? = null,
    val isJoining: Boolean = false,
    val joined: Boolean = false,
    val isSharing: Boolean = false,
    val bufferedPoints: Int = 0,
    val uploadedPoints: Int = 0,
    val actionInFlight: Boolean = false,
    val actionError: String? = null,
    val actionMessage: String? = null,
) {
    val session get() = liveState?.session
    val status get() = session?.status
    val isLive get() = status == "live"
    val isPaused get() = status == "paused"
    val isEnded get() = status == "ended"
    val noSession get() = session == null
    val activeBreak get() = session?.breaks?.firstOrNull { it.endedAt == null }
    val onBreak get() = activeBreak != null
    val riders: List<LiveRiderPosition> get() = liveState?.riders ?: emptyList()
}

/**
 * Drives the live-ride screen: polls session state + metrics, joins the session,
 * runs organizer controls (start/pause/resume/end + breaks), and streams the
 * device's GPS fixes up in batches while the user is sharing. The screen must
 * obtain a location permission before calling [startSharing].
 */
class LiveRideViewModel(
    private val liveRepository: LiveRepository,
    private val locationTracker: LocationTracker,
) : ViewModel() {

    var uiState by mutableStateOf(LiveUiState())
        private set

    private val buffer = ArrayDeque<LocationPoint>()
    private var pollJob: Job? = null
    private var flushJob: Job? = null

    fun start(rideId: String) {
        if (pollJob != null) return
        viewModelScope.launch { refreshOnce(rideId, firstLoad = true) }
        pollJob = viewModelScope.launch {
            while (true) {
                delay(POLL_MS)
                refreshOnce(rideId, firstLoad = false)
            }
        }
    }

    private suspend fun refreshOnce(rideId: String, firstLoad: Boolean) {
        if (firstLoad) uiState = uiState.copy(isLoading = true, error = null)
        when (val r = liveRepository.state(rideId)) {
            is ApiResult.Success -> uiState = uiState.copy(isLoading = false, liveState = r.data, error = null)
            is ApiResult.Failure -> if (firstLoad) uiState = uiState.copy(isLoading = false, error = r.error.userMessage)
        }
        when (val m = liveRepository.metrics(rideId)) {
            is ApiResult.Success -> uiState = uiState.copy(metrics = m.data)
            is ApiResult.Failure -> Unit
        }
    }

    fun join(rideId: String) {
        viewModelScope.launch {
            uiState = uiState.copy(isJoining = true, actionError = null)
            when (val r = liveRepository.join(rideId)) {
                is ApiResult.Success ->
                    uiState = uiState.copy(isJoining = false, joined = true, actionMessage = "You're on the map")
                is ApiResult.Failure ->
                    uiState = uiState.copy(isJoining = false, actionError = r.error.userMessage)
            }
            refreshOnce(rideId, firstLoad = false)
        }
    }

    fun control(rideId: String, action: String) {
        viewModelScope.launch {
            uiState = uiState.copy(actionInFlight = true, actionError = null)
            when (val r = liveRepository.control(rideId, action)) {
                is ApiResult.Success ->
                    uiState = uiState.copy(actionInFlight = false, actionMessage = "Session ${r.data.action ?: action}")
                is ApiResult.Failure ->
                    uiState = uiState.copy(actionInFlight = false, actionError = r.error.userMessage)
            }
            refreshOnce(rideId, firstLoad = false)
        }
    }

    fun toggleBreak(rideId: String) {
        val action = if (uiState.onBreak) "end" else "start"
        viewModelScope.launch {
            uiState = uiState.copy(actionInFlight = true, actionError = null)
            when (val r = liveRepository.setBreak(rideId, action)) {
                is ApiResult.Success ->
                    uiState = uiState.copy(actionInFlight = false, actionMessage = if (action == "start") "Break started" else "Break ended")
                is ApiResult.Failure ->
                    uiState = uiState.copy(actionInFlight = false, actionError = r.error.userMessage)
            }
            refreshOnce(rideId, firstLoad = false)
        }
    }

    /** Begin streaming this device's GPS up. The caller guarantees permission. */
    fun startSharing(rideId: String) {
        if (uiState.isSharing) return
        uiState = uiState.copy(isSharing = true, actionError = null)
        locationTracker.start { loc -> enqueue(loc) }
        flushJob = viewModelScope.launch {
            while (true) {
                delay(FLUSH_MS)
                flush(rideId)
            }
        }
    }

    fun stopSharing(rideId: String) {
        locationTracker.stop()
        flushJob?.cancel()
        flushJob = null
        viewModelScope.launch { flush(rideId) }
        uiState = uiState.copy(isSharing = false)
    }

    fun dismissActionFeedback() {
        uiState = uiState.copy(actionError = null, actionMessage = null)
    }

    @Synchronized
    private fun enqueue(loc: Location) {
        buffer.addLast(
            LocationPoint(
                lat = loc.latitude,
                lng = loc.longitude,
                speed = if (loc.hasSpeed()) loc.speed.toDouble() else null,
                heading = if (loc.hasBearing()) loc.bearing.toDouble() else null,
                accuracy = if (loc.hasAccuracy()) loc.accuracy.toDouble() else null,
                recordedAt = Instant.ofEpochMilli(loc.time).toString(),
            ),
        )
        uiState = uiState.copy(bufferedPoints = buffer.size)
    }

    @Synchronized
    private fun drain(): List<LocationPoint> {
        if (buffer.isEmpty()) return emptyList()
        val copy = buffer.toList()
        buffer.clear()
        return copy
    }

    private suspend fun flush(rideId: String) {
        val points = drain()
        if (points.isEmpty()) return
        when (val r = liveRepository.uploadLocations(rideId, points)) {
            is ApiResult.Success ->
                uiState = uiState.copy(
                    uploadedPoints = uiState.uploadedPoints + r.data.accepted,
                    bufferedPoints = buffer.size,
                )
            is ApiResult.Failure -> {
                // Re-queue the dropped batch so it isn't lost on a transient failure.
                synchronized(this) { points.asReversed().forEach { buffer.addFirst(it) } }
                uiState = uiState.copy(bufferedPoints = buffer.size, actionError = r.error.userMessage)
            }
        }
    }

    override fun onCleared() {
        locationTracker.stop()
        pollJob?.cancel()
        flushJob?.cancel()
    }

    private companion object {
        const val POLL_MS = 5_000L
        const val FLUSH_MS = 8_000L
    }
}

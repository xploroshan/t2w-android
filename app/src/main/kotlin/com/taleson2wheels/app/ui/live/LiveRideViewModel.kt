package com.taleson2wheels.app.ui.live

import androidx.compose.runtime.Immutable

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taleson2wheels.app.data.remote.ApiResult
import com.taleson2wheels.app.data.remote.dto.LiveMetrics
import com.taleson2wheels.app.data.remote.dto.LiveRiderPosition
import com.taleson2wheels.app.data.remote.dto.LiveState
import com.taleson2wheels.app.data.repository.LiveRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Immutable
data class LiveUiState(
    val isLoading: Boolean = true,
    val liveState: LiveState? = null,
    val metrics: LiveMetrics? = null,
    val error: String? = null,
    val isJoining: Boolean = false,
    val joined: Boolean = false,
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
 * and runs organizer controls (start/pause/resume/end + breaks). GPS sharing
 * itself runs in [com.taleson2wheels.app.data.location.LiveLocationService] (a
 * foreground service) so it survives screen-off; the screen starts/stops it and
 * observes [com.taleson2wheels.app.data.location.LiveShareController].
 */
class LiveRideViewModel(
    private val liveRepository: LiveRepository,
) : ViewModel() {

    var uiState by mutableStateOf(LiveUiState())
        private set

    private var pollJob: Job? = null

    /**
     * Begin (or resume) polling session state + metrics every [POLL_MS]. Driven by
     * the screen's lifecycle (started on ON_START, [stop]ped on ON_STOP), so the
     * loop pauses when the screen is backgrounded or buried under another
     * destination instead of hammering the server for the life of the back-stack
     * entry. Idempotent while a poll is already running. Does an immediate refresh
     * on (re)start — silent once we already have data, so resuming doesn't flash
     * the full-screen loader.
     */
    fun start(rideId: String) {
        if (pollJob?.isActive == true) return
        val firstLoad = uiState.liveState == null
        pollJob = viewModelScope.launch {
            refreshOnce(rideId, firstLoad = firstLoad)
            while (true) {
                delay(POLL_MS)
                refreshOnce(rideId, firstLoad = false)
            }
        }
    }

    /** Pause polling — called when the screen leaves the foreground. */
    fun stop() {
        pollJob?.cancel()
        pollJob = null
    }

    /** Immediate re-fetch for the error screen's Retry button. start() returns
     *  early once polling is running, so the button needs its own entry point. */
    fun retry(rideId: String) {
        viewModelScope.launch { refreshOnce(rideId, firstLoad = true) }
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

    fun dismissActionFeedback() {
        uiState = uiState.copy(actionError = null, actionMessage = null)
    }

    override fun onCleared() {
        pollJob?.cancel()
    }

    private companion object {
        const val POLL_MS = 5_000L
    }
}

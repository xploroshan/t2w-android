package com.taleson2wheels.app.ui.relive

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taleson2wheels.app.data.remote.ApiResult
import com.taleson2wheels.app.data.remote.dto.LiveMetrics
import com.taleson2wheels.app.data.remote.dto.LivePathPoint
import com.taleson2wheels.app.data.remote.dto.LiveRiderPosition
import com.taleson2wheels.app.data.repository.LiveRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

/** One selectable playback speed for the flyover. */
enum class ReliveSpeed(val multiplier: Float, val label: String) {
    X1(1f, "1×"),
    X2(2f, "2×"),
    X4(4f, "4×"),
    X8(8f, "8×"),
}

@Immutable
data class ReliveUiState(
    val rideId: String = "",
    val isLoading: Boolean = true,
    val loadError: String? = null,
    val riders: List<LiveRiderPosition> = emptyList(),
    val selectedRiderId: String? = null,
    val track: List<ReliveTrackPoint> = emptyList(),
    val elevations: List<Float> = emptyList(),
    val plannedRoute: List<LivePathPoint> = emptyList(),
    val metrics: LiveMetrics? = null,
    // Playback
    val isPlaying: Boolean = false,
    val playbackMs: Long = 0L,
    val durationMs: Long = 0L,
    val speed: ReliveSpeed = ReliveSpeed.X1,
    val sample: ReliveSample? = null,
    // Transient
    val switchingRider: Boolean = false,
    val actionError: String? = null,
) {
    val hasTrack: Boolean get() = track.size >= 2
    val atEnd: Boolean get() = durationMs > 0L && playbackMs >= durationMs
    val progress: Float
        get() = if (durationMs > 0L) (playbackMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
    val selectedRiderName: String
        get() = riders.firstOrNull { it.userId == selectedRiderId }?.userName?.ifBlank { null }
            ?: selectedRiderId ?: "—"
}

/**
 * Drives the Relive 3D flyover. Loads a ride's ended/paused session, the chosen
 * rider's recorded track (+ elevation series + headline metrics), builds a
 * time-indexed playback timeline via [RelivePlayback], and runs a fixed-step
 * ticker exposing an interpolated [ReliveSample] the Mapbox screen renders as an
 * animated camera + marker. All playback logic is pure and unit-tested; only the
 * map rendering itself depends on Mapbox.
 */
class ReliveViewModel(
    private val liveRepository: LiveRepository,
    private val json: Json,
) : ViewModel() {

    var uiState by mutableStateOf(ReliveUiState())
        private set

    private var initialized = false
    private var tickJob: Job? = null

    fun load(rideId: String) {
        if (initialized) return
        initialized = true
        uiState = uiState.copy(rideId = rideId, isLoading = true, loadError = null)
        viewModelScope.launch {
            when (val r = liveRepository.state(rideId)) {
                is ApiResult.Failure ->
                    uiState = uiState.copy(isLoading = false, loadError = r.error.userMessage)
                is ApiResult.Success -> {
                    val session = r.data.session
                    val riders = r.data.riders
                    val selected = session?.leadRiderId ?: riders.firstOrNull()?.userId
                    uiState = uiState.copy(
                        isLoading = false,
                        riders = riders,
                        selectedRiderId = selected,
                        plannedRoute = parsePlanned(session?.plannedRoute),
                    )
                    loadMetrics()
                    if (selected != null) loadRiderTrack(selected)
                }
            }
        }
    }

    /** Re-run the initial load (error-state retry). */
    fun reload() {
        initialized = false
        stopTicker()
        uiState = uiState.copy(track = emptyList(), sample = null, playbackMs = 0L, durationMs = 0L, isPlaying = false)
        load(uiState.rideId)
    }

    fun selectRider(userId: String) {
        if (userId == uiState.selectedRiderId) return
        if (uiState.switchingRider) return
        stopTicker()
        uiState = uiState.copy(
            selectedRiderId = userId,
            switchingRider = true,
            isPlaying = false,
            actionError = null,
        )
        viewModelScope.launch { loadRiderTrack(userId) }
    }

    // ── Playback controls ────────────────────────────────────────────────────

    fun togglePlay() { if (uiState.isPlaying) pause() else play() }

    fun play() {
        if (!uiState.hasTrack) return
        // Restart from the beginning if we're sitting at the end.
        if (uiState.atEnd) setPlayback(0L)
        if (tickJob?.isActive == true) return
        uiState = uiState.copy(isPlaying = true)
        tickJob = viewModelScope.launch {
            while (uiState.isPlaying) {
                delay(TICK_MS)
                val advance = (TICK_MS * uiState.speed.multiplier).toLong()
                val next = uiState.playbackMs + advance
                if (next >= uiState.durationMs) {
                    setPlayback(uiState.durationMs)
                    uiState = uiState.copy(isPlaying = false)
                } else {
                    setPlayback(next)
                }
            }
        }
    }

    fun pause() {
        stopTicker()
        uiState = uiState.copy(isPlaying = false)
    }

    /** Scrub to an absolute offset (clamped). Pauses so scrubbing feels precise. */
    fun seekTo(ms: Long) {
        pause()
        setPlayback(ms.coerceIn(0L, uiState.durationMs))
    }

    /** Scrub by fraction of the track, 0..1. */
    fun seekToProgress(fraction: Float) {
        seekTo((fraction.coerceIn(0f, 1f) * uiState.durationMs).toLong())
    }

    fun setSpeed(speed: ReliveSpeed) {
        uiState = uiState.copy(speed = speed)
    }

    fun dismissError() { uiState = uiState.copy(actionError = null) }

    // ── Internals ────────────────────────────────────────────────────────────

    private suspend fun loadRiderTrack(userId: String) {
        val path = when (val r = liveRepository.state(uiState.rideId, viewUserId = userId)) {
            is ApiResult.Success -> r.data.viewPath.ifEmpty { r.data.leadPath }
            is ApiResult.Failure -> {
                uiState = uiState.copy(switchingRider = false, actionError = r.error.userMessage)
                return
            }
        }
        // Elevation is a nice-to-have HUD series; its absence just hides the readout.
        val elevations = when (val e = liveRepository.elevationAltitudes(uiState.rideId, userId)) {
            is ApiResult.Success -> e.data
            is ApiResult.Failure -> emptyList()
        }
        val track = RelivePlayback.buildTimeline(path)
        uiState = uiState.copy(
            track = track,
            elevations = elevations,
            durationMs = RelivePlayback.durationMs(track),
            playbackMs = 0L,
            isPlaying = false,
            switchingRider = false,
            sample = RelivePlayback.sample(track, elevations, 0L),
        )
    }

    private suspend fun loadMetrics() {
        when (val r = liveRepository.metrics(uiState.rideId)) {
            is ApiResult.Success -> uiState = uiState.copy(metrics = r.data)
            is ApiResult.Failure -> Unit // non-fatal; the HUD just omits headline metrics
        }
    }

    private fun setPlayback(ms: Long) {
        uiState = uiState.copy(
            playbackMs = ms,
            sample = RelivePlayback.sample(uiState.track, uiState.elevations, ms),
        )
    }

    private fun stopTicker() {
        tickJob?.cancel()
        tickJob = null
    }

    private fun parsePlanned(raw: String?): List<LivePathPoint> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching { json.decodeFromString<List<LivePathPoint>>(raw) }.getOrDefault(emptyList())
    }

    override fun onCleared() {
        stopTicker()
    }

    private companion object {
        // Fixed playback step: ~30 fps of wall-clock advance at 1× speed.
        const val TICK_MS = 33L
    }
}

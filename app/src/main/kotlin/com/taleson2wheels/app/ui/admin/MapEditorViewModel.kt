package com.taleson2wheels.app.ui.admin

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
import com.taleson2wheels.app.data.remote.dto.LiveSession
import com.taleson2wheels.app.data.remote.dto.MapAuditEntry
import com.taleson2wheels.app.data.remote.dto.SmoothStats
import com.taleson2wheels.app.data.repository.LiveRepository
import com.taleson2wheels.app.data.repository.MapEditRepository
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull

/** The six stat-override text fields, plus which ones the admin has touched. */
@Immutable
data class StatsForm(
    val distanceKm: String = "",
    val avgSpeedKmh: String = "",
    val maxSpeedKmh: String = "",
    val movingMinutes: String = "",
    val elevationGainM: String = "",
    val elevationLossM: String = "",
    val dirty: Set<String> = emptySet(),
)

@Immutable
data class MapEditorUiState(
    val rideId: String = "",
    val isLoading: Boolean = true,
    val loadError: String? = null,
    val session: LiveSession? = null,
    val riders: List<LiveRiderPosition> = emptyList(),
    val selectedRiderId: String? = null,
    val recordedPath: List<LivePathPoint> = emptyList(),
    val plannedRoute: List<LivePathPoint> = emptyList(),
    val metrics: LiveMetrics? = null,
    val statsForm: StatsForm = StatsForm(),
    val audit: List<MapAuditEntry> = emptyList(),
    // Transient action state
    val busy: Boolean = false,
    val actionError: String? = null,
    val message: String? = null,
    val smoothPreview: SmoothStats? = null,
    val smoothPreviewPoints: Int? = null,
) {
    /** The editor only operates on a non-live session (paused/ended). */
    val editable: Boolean get() = session != null && session.status != "live"
    val selectedRiderName: String
        get() = riders.firstOrNull { it.userId == selectedRiderId }?.userName?.ifBlank { null }
            ?: selectedRiderId ?: "—"
}

/**
 * Post-ride map editor. Loads a ride's (paused/ended) session, a chosen rider's
 * recorded track + the planned overlay, then exposes the safe map-edit ops:
 * smooth-&-fill (preview/apply/revert), manual breaks, and headline-stat
 * overrides — each writing through [MapEditRepository] and refreshing on success.
 */
class MapEditorViewModel(
    private val liveRepository: LiveRepository,
    private val mapEditRepository: MapEditRepository,
    private val json: Json,
) : ViewModel() {

    var uiState by mutableStateOf(MapEditorUiState())
        private set

    private var initialized = false

    fun load(rideId: String) {
        if (initialized) return
        initialized = true
        uiState = uiState.copy(rideId = rideId, isLoading = true, loadError = null)
        viewModelScope.launch {
            when (val r = liveRepository.state(rideId)) {
                is ApiResult.Failure -> {
                    uiState = uiState.copy(isLoading = false, loadError = r.error.userMessage)
                    return@launch
                }
                is ApiResult.Success -> {
                    val session = r.data.session
                    val riders = r.data.riders
                    val selected = session?.leadRiderId ?: riders.firstOrNull()?.userId
                    uiState = uiState.copy(
                        isLoading = false,
                        session = session,
                        riders = riders,
                        selectedRiderId = selected,
                        recordedPath = r.data.leadPath,
                        plannedRoute = parsePlanned(session?.plannedRoute),
                    )
                    if (selected != null) fetchRiderPath(selected)
                    loadMetrics()
                    loadAudit()
                }
            }
        }
    }

    /** Re-run the initial load (used by the error-state retry). */
    fun reload() {
        initialized = false
        load(uiState.rideId)
    }

    fun selectRider(userId: String) {
        if (userId == uiState.selectedRiderId) return
        uiState = uiState.copy(selectedRiderId = userId, smoothPreview = null, smoothPreviewPoints = null)
        viewModelScope.launch { fetchRiderPath(userId) }
    }

    private suspend fun fetchRiderPath(userId: String) {
        when (val r = liveRepository.state(uiState.rideId, viewUserId = userId)) {
            is ApiResult.Success ->
                uiState = uiState.copy(
                    recordedPath = r.data.viewPath.ifEmpty { r.data.leadPath },
                )
            is ApiResult.Failure -> uiState = uiState.copy(actionError = r.error.userMessage)
        }
    }

    private suspend fun loadMetrics() {
        when (val r = liveRepository.metrics(uiState.rideId)) {
            is ApiResult.Success -> uiState = uiState.copy(metrics = r.data)
            is ApiResult.Failure -> Unit // non-fatal; the overrides form just starts blank
        }
    }

    private suspend fun loadAudit() {
        when (val r = mapEditRepository.audit(uiState.rideId)) {
            is ApiResult.Success -> uiState = uiState.copy(audit = r.data)
            is ApiResult.Failure -> Unit // non-fatal
        }
    }

    // ── Smooth & fill ────────────────────────────────────────────────────────

    fun previewSmooth() = withRider { rider ->
        runAction(refresh = false) {
            when (val r = mapEditRepository.smoothPreview(uiState.rideId, rider)) {
                is ApiResult.Success -> {
                    uiState = uiState.copy(
                        smoothPreview = r.data.stats,
                        smoothPreviewPoints = pointCount(r.data.points),
                        message = "Preview ready — review, then apply.",
                    )
                    null
                }
                is ApiResult.Failure -> r.error.userMessage
            }
        }
    }

    fun applySmooth() = withRider { rider ->
        runAction {
            when (val r = mapEditRepository.smoothApply(uiState.rideId, rider)) {
                is ApiResult.Success -> {
                    uiState = uiState.copy(
                        smoothPreview = null,
                        smoothPreviewPoints = null,
                        message = "Track smoothed (${pointCount(r.data.points)} points).",
                    )
                    null
                }
                is ApiResult.Failure -> r.error.userMessage
            }
        }
    }

    fun revertSmooth() = withRider { rider ->
        runAction {
            when (val r = mapEditRepository.revertSmooth(uiState.rideId, rider)) {
                is ApiResult.Success -> {
                    uiState = uiState.copy(
                        smoothPreview = null,
                        smoothPreviewPoints = null,
                        message = "Reverted to the raw track.",
                    )
                    null
                }
                is ApiResult.Failure -> r.error.userMessage
            }
        }
    }

    // ── Breaks (list comes from the session; add needs a datetime picker — v2) ──

    fun deleteBreak(breakId: String) = runAction {
        when (val r = mapEditRepository.deleteBreak(uiState.rideId, breakId)) {
            is ApiResult.Success -> { uiState = uiState.copy(message = "Break removed."); null }
            is ApiResult.Failure -> r.error.userMessage
        }
    }

    // ── Stat overrides ─────────────────────────────────────────────────────────

    fun onStats(update: StatsForm.() -> StatsForm) {
        uiState = uiState.copy(statsForm = uiState.statsForm.update(), actionError = null)
    }

    fun saveStats() {
        val f = uiState.statsForm
        if (f.dirty.isEmpty()) {
            uiState = uiState.copy(message = "No stat changes to save.")
            return
        }
        val body: JsonObject
        try {
            body = buildStatsBody(f)
        } catch (e: NumberFormatException) {
            uiState = uiState.copy(actionError = e.message ?: "Enter valid numbers")
            return
        }
        runAction {
            when (val r = mapEditRepository.updateStats(uiState.rideId, body)) {
                is ApiResult.Success -> {
                    uiState = uiState.copy(statsForm = f.copy(dirty = emptySet()), message = "Stats updated.")
                    null
                }
                is ApiResult.Failure -> r.error.userMessage
            }
        }
    }

    fun dismissMessage() { uiState = uiState.copy(message = null) }
    fun dismissError() { uiState = uiState.copy(actionError = null) }

    // ── Internals ──────────────────────────────────────────────────────────────

    private inline fun withRider(block: (String) -> Unit) {
        val rider = uiState.selectedRiderId
        if (rider == null) uiState = uiState.copy(actionError = "Select a rider first")
        else block(rider)
    }

    /**
     * Run a mutating action: guard against re-entry, clear prior errors, await the
     * [block] (which returns an error string or null), then on success refresh the
     * derived state. Never leaves `busy` stuck.
     */
    private fun runAction(refresh: Boolean = true, block: suspend () -> String?) {
        if (uiState.busy) return
        uiState = uiState.copy(busy = true, actionError = null)
        viewModelScope.launch {
            val err = try {
                block()
            } catch (e: Exception) {
                e.message ?: "Something went wrong"
            }
            uiState = uiState.copy(busy = false, actionError = err)
            if (err == null && refresh) refreshAfterMutation()
        }
    }

    /** Re-pull the session (breaks/planned), the selected rider's path, metrics + audit. */
    private suspend fun refreshAfterMutation() {
        when (val r = liveRepository.state(uiState.rideId)) {
            is ApiResult.Success -> uiState = uiState.copy(
                session = r.data.session,
                riders = r.data.riders,
                plannedRoute = parsePlanned(r.data.session?.plannedRoute),
            )
            is ApiResult.Failure -> Unit
        }
        uiState.selectedRiderId?.let { fetchRiderPath(it) }
        loadMetrics()
        loadAudit()
    }

    private fun parsePlanned(raw: String?): List<LivePathPoint> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching { json.decodeFromString<List<LivePathPoint>>(raw) }.getOrDefault(emptyList())
    }

    private fun pointCount(el: JsonElement?): Int = when (el) {
        is JsonArray -> el.size
        is JsonPrimitive -> el.intOrNull ?: 0
        else -> 0
    }

    /** Build the PATCH body from ONLY the touched fields (blank → clear, value → set). */
    private fun buildStatsBody(f: StatsForm): JsonObject = buildJsonObject {
        fun emit(key: String, raw: String, integer: Boolean) {
            if (key !in f.dirty) return
            val t = raw.trim()
            if (t.isEmpty()) {
                put(key, JsonNull)
            } else {
                val n = if (integer) {
                    JsonPrimitive(t.toIntOrNull() ?: throw NumberFormatException("“$t” is not a whole number"))
                } else {
                    JsonPrimitive(t.toDoubleOrNull() ?: throw NumberFormatException("“$t” is not a number"))
                }
                put(key, n)
            }
        }
        emit("distanceKmOverride", f.distanceKm, integer = false)
        emit("avgSpeedKmhOverride", f.avgSpeedKmh, integer = false)
        emit("maxSpeedKmhOverride", f.maxSpeedKmh, integer = false)
        emit("movingMinutesOverride", f.movingMinutes, integer = true)
        emit("elevationGainM", f.elevationGainM, integer = true)
        emit("elevationLossM", f.elevationLossM, integer = true)
    }
}

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
import com.taleson2wheels.app.data.remote.dto.MapWaypoint
import com.taleson2wheels.app.data.remote.dto.SmoothStats
import com.taleson2wheels.app.data.repository.LiveRepository
import com.taleson2wheels.app.data.repository.MapEditRepository
import java.time.Instant
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

/**
 * The in-progress planned-route edit: a working copy of the overlay's waypoints,
 * which one (if any) is selected, and whether anything changed since [startPlannedEdit].
 * Null on [MapEditorUiState] means "not editing" (the map is read-only).
 */
@Immutable
data class PlannedEditState(
    val waypoints: List<MapWaypoint> = emptyList(),
    val selectedIndex: Int? = null,
    val dirty: Boolean = false,
)

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
    // Non-null while the planned route is being edited on the map.
    val plannedEdit: PlannedEditState? = null,
) {
    /** The editor only operates on a non-live session (paused/ended). */
    val editable: Boolean get() = session != null && session.status != "live"
    val isEditingPlanned: Boolean get() = plannedEdit != null
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
        // Don't switch riders mid-action: a smooth/revert in flight is attributed to
        // the rider selected when it started, and refreshAfterMutation reads the
        // current selection — switching now would mismatch the two and race the path.
        if (uiState.busy) return
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

    // ── GPX import ───────────────────────────────────────────────────────────

    /** Replace the selected rider's recorded track from a chosen .gpx file. */
    fun importRecordedGpx(bytes: ByteArray, filename: String) {
        if (tooLarge(bytes)) return
        withRider { rider ->
            runAction {
                when (val r = mapEditRepository.importRecordedGpx(uiState.rideId, rider, filename, bytes)) {
                    is ApiResult.Success -> {
                        uiState = uiState.copy(message = "Imported ${r.data.inserted} points from $filename.")
                        null
                    }
                    is ApiResult.Failure -> r.error.userMessage
                }
            }
        }
    }

    /** Replace the planned-route overlay from a chosen .gpx file. */
    fun importPlannedGpx(bytes: ByteArray, filename: String) {
        if (tooLarge(bytes)) return
        runAction {
            when (val r = mapEditRepository.importPlannedGpx(uiState.rideId, filename, bytes)) {
                is ApiResult.Success -> {
                    uiState = uiState.copy(message = "Planned route imported (${r.data.waypointCount} points).")
                    null
                }
                is ApiResult.Failure -> r.error.userMessage
            }
        }
    }

    /** Surface a file-read failure from the SAF picker (the screen reads the bytes). */
    fun reportError(message: String) {
        uiState = uiState.copy(actionError = message)
    }

    private fun tooLarge(bytes: ByteArray): Boolean {
        val over = bytes.size > MAX_GPX_BYTES
        if (over) uiState = uiState.copy(actionError = "GPX file too large (max 5 MB)")
        return over
    }

    // ── Breaks (list from the session; add via the screen's datetime pickers) ────

    /** Add a manual break. [endMillis] null = an open-ended break. Times are epoch ms. */
    fun addBreak(startMillis: Long, endMillis: Long?, reason: String?) {
        if (endMillis != null && endMillis <= startMillis) {
            uiState = uiState.copy(actionError = "Break end must be after its start")
            return
        }
        val startedAt = isoOf(startMillis)
        val endedAt = endMillis?.let { isoOf(it) }
        val trimmedReason = reason?.trim()?.takeIf { it.isNotEmpty() }
        runAction {
            when (val r = mapEditRepository.addBreak(uiState.rideId, startedAt, endedAt, trimmedReason)) {
                is ApiResult.Success -> { uiState = uiState.copy(message = "Break added."); null }
                is ApiResult.Failure -> r.error.userMessage
            }
        }
    }

    fun deleteBreak(breakId: String) = runAction {
        when (val r = mapEditRepository.deleteBreak(uiState.rideId, breakId)) {
            is ApiResult.Success -> { uiState = uiState.copy(message = "Break removed."); null }
            is ApiResult.Failure -> r.error.userMessage
        }
    }

    // ── Trim recorded track by time range ───────────────────────────────────────

    /**
     * Remove the selected rider's fixes in a window: `after < recordedAt < before`.
     * Only [afterMillis] → trim the tail; only [beforeMillis] → trim the head.
     */
    fun trimTrackPoints(afterMillis: Long?, beforeMillis: Long?) = withRider { rider ->
        if (afterMillis == null && beforeMillis == null) {
            uiState = uiState.copy(actionError = "Pick a start and/or end time to trim")
            return@withRider
        }
        if (afterMillis != null && beforeMillis != null && afterMillis >= beforeMillis) {
            uiState = uiState.copy(actionError = "End time must be after start time")
            return@withRider
        }
        val after = afterMillis?.let { isoOf(it) }
        val before = beforeMillis?.let { isoOf(it) }
        runAction {
            when (val r = mapEditRepository.trimTrackPoints(uiState.rideId, rider, after, before)) {
                is ApiResult.Success -> {
                    uiState = uiState.copy(
                        message = if (r.data > 0) "Removed ${r.data} track points." else "No points in that range.",
                    )
                    null
                }
                is ApiResult.Failure -> r.error.userMessage
            }
        }
    }

    // ── Planned-route edit (tap the map: move the selected pin, or add one) ──────

    fun startPlannedEdit() {
        if (uiState.busy || uiState.isEditingPlanned) return
        val wps = uiState.plannedRoute.map { MapWaypoint(it.lat, it.lng) }
        if (wps.size > MAX_EDITABLE_WAYPOINTS) {
            uiState = uiState.copy(
                actionError = "This planned route has too many points to edit on mobile " +
                    "(max $MAX_EDITABLE_WAYPOINTS) — replace it with a GPX import instead.",
            )
            return
        }
        uiState = uiState.copy(plannedEdit = PlannedEditState(waypoints = wps))
    }

    fun cancelPlannedEdit() {
        if (uiState.busy) return
        uiState = uiState.copy(plannedEdit = null)
    }

    fun selectPlannedWaypoint(index: Int?) {
        val e = uiState.plannedEdit ?: return
        val sel = when {
            index == null || index !in e.waypoints.indices -> null
            index == e.selectedIndex -> null // tapping the selected pin deselects it (so a map tap adds a new one)
            else -> index
        }
        uiState = uiState.copy(plannedEdit = e.copy(selectedIndex = sel))
    }

    /** A map tap moves the selected waypoint here, or appends a new one if none is selected. */
    fun onPlannedMapTap(lat: Double, lng: Double) {
        if (uiState.busy) return
        val e = uiState.plannedEdit ?: return
        val sel = e.selectedIndex
        val next = if (sel != null && sel in e.waypoints.indices) {
            val moved = e.waypoints.toMutableList().also { it[sel] = MapWaypoint(lat, lng) }
            e.copy(waypoints = moved, dirty = true)
        } else {
            if (e.waypoints.size >= MAX_EDITABLE_WAYPOINTS) {
                uiState = uiState.copy(actionError = "Too many waypoints (max $MAX_EDITABLE_WAYPOINTS)")
                return
            }
            e.copy(waypoints = e.waypoints + MapWaypoint(lat, lng), selectedIndex = e.waypoints.size, dirty = true)
        }
        uiState = uiState.copy(plannedEdit = next)
    }

    fun deleteSelectedWaypoint() {
        if (uiState.busy) return
        val e = uiState.plannedEdit ?: return
        val sel = e.selectedIndex ?: return
        if (sel !in e.waypoints.indices) return
        val remaining = e.waypoints.toMutableList().also { it.removeAt(sel) }
        uiState = uiState.copy(plannedEdit = e.copy(waypoints = remaining, selectedIndex = null, dirty = true))
    }

    fun savePlanned() {
        val e = uiState.plannedEdit ?: return
        if (!e.dirty) {
            uiState = uiState.copy(message = "No route changes to save.")
            return
        }
        val wps = e.waypoints
        runAction {
            when (val r = mapEditRepository.setPlannedRoute(uiState.rideId, wps)) {
                is ApiResult.Success -> {
                    uiState = uiState.copy(
                        plannedRoute = wps.map { LivePathPoint(it.lat, it.lng) },
                        plannedEdit = null,
                        message = "Planned route saved (${wps.size} points).",
                    )
                    null
                }
                is ApiResult.Failure -> r.error.userMessage
            }
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
        val sentKeys = f.dirty
        runAction {
            when (val r = mapEditRepository.updateStats(uiState.rideId, body)) {
                is ApiResult.Success -> {
                    // Clear only the keys we actually sent, against the CURRENT form —
                    // so a field the admin edited WHILE the save was in flight keeps its
                    // value and dirty flag instead of being clobbered by a stale snapshot.
                    uiState = uiState.copy(
                        statsForm = uiState.statsForm.copy(dirty = uiState.statsForm.dirty - sentKeys),
                        message = "Stats updated.",
                    )
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

    /** Epoch millis → ISO-8601 UTC (what the backend's date parsers accept). */
    private fun isoOf(millis: Long): String = Instant.ofEpochMilli(millis).toString()

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

    private companion object {
        // Matches the backend's MAX_GPX_BYTES so we reject early with a clear message.
        const val MAX_GPX_BYTES = 5 * 1024 * 1024
        // Cap for tap-to-edit on a phone — a denser route should be replaced via GPX.
        const val MAX_EDITABLE_WAYPOINTS = 100
    }
}

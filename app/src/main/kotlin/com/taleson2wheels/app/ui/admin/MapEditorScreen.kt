package com.taleson2wheels.app.ui.admin

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.taleson2wheels.app.data.remote.dto.LiveBreak
import com.taleson2wheels.app.data.remote.dto.MapAuditEntry
import com.taleson2wheels.app.data.remote.dto.SmoothStats
import com.taleson2wheels.app.ui.AppViewModelFactory
import com.taleson2wheels.app.ui.common.ErrorView
import com.taleson2wheels.app.ui.common.LoadingView
import com.taleson2wheels.app.ui.components.BrandBackground
import com.taleson2wheels.app.ui.components.BrandCard
import com.taleson2wheels.app.ui.components.GradientButton
import com.taleson2wheels.app.ui.components.SecondaryButton
import com.taleson2wheels.app.ui.components.SectionHeader
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.util.Calendar
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapEditorScreen(
    rideId: String,
    factory: AppViewModelFactory,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MapEditorViewModel = viewModel(factory = factory),
) {
    val state = viewModel.uiState
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(rideId) { viewModel.load(rideId) }
    LaunchedEffect(state.message) {
        state.message?.let { snackbar.showSnackbar(it); viewModel.dismissMessage() }
    }
    LaunchedEffect(state.actionError) {
        state.actionError?.let { snackbar.showSnackbar(it); viewModel.dismissError() }
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("Map editor") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                ),
            )
        },
    ) { inner ->
        BrandBackground(Modifier.padding(inner)) {
            when {
                state.isLoading -> LoadingView()
                state.loadError != null -> ErrorView(state.loadError, onRetry = { viewModel.reload() })
                else -> MapEditorContent(state, viewModel)
            }
        }
    }
}

@Composable
private fun MapEditorContent(state: MapEditorUiState, viewModel: MapEditorViewModel) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())

        if (state.session == null) {
            BrandCard {
                Text("No live session for this ride yet — start one before editing its map.",
                    style = MaterialTheme.typography.bodyMedium)
            }
            return@Column
        }
        if (!state.editable) {
            BrandCard {
                Text(
                    "This ride is still live. Pause or end it before editing the recorded map.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }

        val pe = state.plannedEdit
        MapEditorMapCard(
            recordedPath = state.recordedPath,
            plannedRoute = state.plannedRoute,
            editWaypoints = pe?.waypoints,
            selectedWaypoint = pe?.selectedIndex,
            onWaypointTap = { viewModel.selectPlannedWaypoint(it) },
            onMapTap = { lat, lng -> viewModel.onPlannedMapTap(lat, lng) },
        )

        RiderSection(state, viewModel)
        PlannedRouteSection(state, viewModel)
        SmoothSection(state, viewModel)
        GpxSection(state, viewModel)
        TrimSection(state, viewModel)
        StatsSection(state, viewModel)
        BreaksSection(state, viewModel)
        AuditSection(state.audit)
    }
}

// ── Planned-route edit ───────────────────────────────────────────────────────

@Composable
private fun PlannedRouteSection(state: MapEditorUiState, viewModel: MapEditorViewModel) {
    val pe = state.plannedEdit
    BrandCard {
        SectionHeader("Planned route")
        if (pe == null) {
            Text(
                "${state.plannedRoute.size} waypoints. Tap “Edit route”, then tap a pin to select and tap the map to move it; tap empty map to add a pin at the end.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SecondaryButton(
                "Edit route on map",
                onClick = viewModel::startPlannedEdit,
                enabled = state.editable && !state.busy,
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            )
        } else {
            val selText = pe.selectedIndex?.let { "Waypoint ${it + 1} selected — tap the map to move it." }
                ?: "Tap a pin to select it, or tap the map to add a waypoint."
            Text(
                "Editing ${pe.waypoints.size} waypoint(s). $selText",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SecondaryButton(
                    "Delete pin",
                    onClick = viewModel::deleteSelectedWaypoint,
                    enabled = pe.selectedIndex != null && !state.busy,
                    modifier = Modifier.weight(1f),
                )
                SecondaryButton(
                    "Cancel",
                    onClick = viewModel::cancelPlannedEdit,
                    enabled = !state.busy,
                    modifier = Modifier.weight(1f),
                )
            }
            GradientButton(
                "Save route",
                onClick = viewModel::savePlanned,
                enabled = pe.dirty && !state.busy,
                loading = state.busy,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
        }
    }
}

// ── Trim recorded track by time range ────────────────────────────────────────

@Composable
private fun TrimSection(state: MapEditorUiState, viewModel: MapEditorViewModel) {
    val context = LocalContext.current
    var after by remember { mutableStateOf<Long?>(null) }
    var before by remember { mutableStateOf<Long?>(null) }
    val enabled = state.editable && !state.busy && state.selectedRiderId != null
    BrandCard {
        SectionHeader("Trim recorded track")
        Text(
            "Remove ${state.selectedRiderName}'s fixes in a time window. Set a start to trim the tail, an end to trim the head, or both to cut the middle. This can't be undone.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        DateTimeField("After (start)", after, enabled, onClear = { after = null }) {
            pickDateTime(context, after) { after = it }
        }
        DateTimeField("Before (end)", before, enabled, onClear = { before = null }) {
            pickDateTime(context, before) { before = it }
        }
        GradientButton(
            "Trim points",
            onClick = { viewModel.trimTrackPoints(after, before) },
            enabled = enabled && (after != null || before != null),
            loading = state.busy,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )
    }
}

/** A read-only value + Set/Clear buttons that drive an epoch-millis picker. */
@Composable
private fun DateTimeField(
    label: String,
    millis: Long?,
    enabled: Boolean,
    onClear: () -> Unit,
    onPick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(top = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                millis?.let { fmtMillis(it) } ?: "Not set",
                style = MaterialTheme.typography.bodyMedium,
                color = if (millis != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        TextButton(onClick = onPick, enabled = enabled) { Text("Set") }
        if (millis != null) {
            TextButton(onClick = onClear, enabled = enabled) { Text("Clear") }
        }
    }
}

// ── Rider selector ───────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RiderSection(state: MapEditorUiState, viewModel: MapEditorViewModel) {
    BrandCard {
        SectionHeader("Rider track")
        if (state.riders.isEmpty()) {
            Text("No recorded riders on this session.", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            return@BrandCard
        }
        var expanded by remember { mutableStateOf(false) }
        // Don't allow switching riders mid-action (the VM also ignores it while busy).
        val canPick = !state.busy
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { if (canPick) expanded = it }) {
            OutlinedTextField(
                value = state.selectedRiderName,
                onValueChange = {},
                readOnly = true,
                enabled = canPick,
                label = { Text("Rider") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                state.riders.forEach { r ->
                    DropdownMenuItem(
                        text = { Text(r.userName.ifBlank { r.userId }) },
                        onClick = { viewModel.selectRider(r.userId); expanded = false },
                    )
                }
            }
        }
        Text("${state.recordedPath.size} recorded points", style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
    }
}

// ── Smooth & fill ────────────────────────────────────────────────────────────

@Composable
private fun SmoothSection(state: MapEditorUiState, viewModel: MapEditorViewModel) {
    BrandCard {
        SectionHeader("Smooth & fill gaps")
        Text(
            "Snaps ${state.selectedRiderName}'s track to roads and fills GPS gaps. Preview first, then apply — or revert to the raw track.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        val enabled = state.editable && !state.busy && state.selectedRiderId != null
        Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SecondaryButton("Preview", onClick = viewModel::previewSmooth, enabled = enabled, modifier = Modifier.weight(1f))
            GradientButton("Apply", onClick = viewModel::applySmooth, enabled = enabled, loading = state.busy, modifier = Modifier.weight(1f))
        }
        SecondaryButton("Revert to raw", onClick = viewModel::revertSmooth, enabled = enabled,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
        state.smoothPreview?.let { SmoothPreviewStats(it, state.smoothPreviewPoints) }
    }
}

@Composable
private fun SmoothPreviewStats(stats: SmoothStats, points: Int?) {
    Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text("Preview", style = MaterialTheme.typography.labelLarge)
        StatLine("Proposed points", (points ?: 0).toString())
        StatLine("Snapped to roads", "${stats.movedPercent}%")
        StatLine("Gaps filled", stats.gapsFilled.toString())
        StatLine("Gaps skipped (too long)", stats.gapsSkipped.toString())
    }
}

@Composable
private fun StatLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall)
    }
}

// ── GPX import (SAF file picker + multipart upload) ──────────────────────────

private val GPX_MIME_FILTER = arrayOf(
    "application/gpx+xml", "application/xml", "text/xml", "application/octet-stream", "*/*",
)

@Composable
private fun GpxSection(state: MapEditorUiState, viewModel: MapEditorViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val enabled = state.editable && !state.busy

    fun pick(uri: Uri?, onBytes: (ByteArray, String) -> Unit) {
        if (uri == null) return
        scope.launch {
            val file = readGpxFile(context, uri)
            if (file == null) viewModel.reportError("Couldn't read that file")
            else onBytes(file.second, file.first)
        }
    }
    val recordedPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        pick(uri) { bytes, name -> viewModel.importRecordedGpx(bytes, name) }
    }
    val plannedPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        pick(uri) { bytes, name -> viewModel.importPlannedGpx(bytes, name) }
    }

    BrandCard {
        SectionHeader("Import GPX")
        Text(
            "Replace ${state.selectedRiderName}'s recorded track, or the planned overlay, from a .gpx file (max 5 MB). Non-destructive — a smoothed/recorded track can be reverted.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SecondaryButton(
                "Recorded track…",
                onClick = { recordedPicker.launch(GPX_MIME_FILTER) },
                enabled = enabled && state.selectedRiderId != null,
                modifier = Modifier.weight(1f),
            )
            SecondaryButton(
                "Planned route…",
                onClick = { plannedPicker.launch(GPX_MIME_FILTER) },
                enabled = enabled,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** Read a SAF document's display name + bytes off the main thread; null on failure. */
private suspend fun readGpxFile(context: Context, uri: Uri): Pair<String, ByteArray>? =
    withContext(Dispatchers.IO) {
        runCatching {
            val name = queryDisplayName(context, uri) ?: "upload.gpx"
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: return@runCatching null
            name to bytes
        }.getOrNull()
    }

private fun queryDisplayName(context: Context, uri: Uri): String? =
    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
        ?.use { c -> if (c.moveToFirst()) c.getString(0) else null }

// ── Stat overrides ────────────────────────────────────────────────────────────

@Composable
private fun StatsSection(state: MapEditorUiState, viewModel: MapEditorViewModel) {
    val f = state.statsForm
    val enabled = state.editable && !state.busy
    BrandCard {
        SectionHeader("Override headline stats")
        Text(
            "Enter a value to override a computed metric; leave a field blank (after editing) to clear it. Untouched fields are left as-is.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        state.metrics?.let {
            Text(
                "Computed now — ${fmtNum(it.distanceKm)} km · avg ${fmtNum(it.avgSpeedKmh)} · max ${fmtNum(it.maxSpeedKmh)} km/h · moving ${it.movingMinutes.toInt()} min",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            NumField("Distance (km)", f.distanceKm, enabled) { v -> viewModel.onStats { copy(distanceKm = v, dirty = dirty + "distanceKmOverride") } }
            NumField("Avg speed (km/h)", f.avgSpeedKmh, enabled) { v -> viewModel.onStats { copy(avgSpeedKmh = v, dirty = dirty + "avgSpeedKmhOverride") } }
            NumField("Max speed (km/h)", f.maxSpeedKmh, enabled) { v -> viewModel.onStats { copy(maxSpeedKmh = v, dirty = dirty + "maxSpeedKmhOverride") } }
            NumField("Moving time (min)", f.movingMinutes, enabled) { v -> viewModel.onStats { copy(movingMinutes = v, dirty = dirty + "movingMinutesOverride") } }
            NumField("Elevation gain (m)", f.elevationGainM, enabled) { v -> viewModel.onStats { copy(elevationGainM = v, dirty = dirty + "elevationGainM") } }
            NumField("Elevation loss (m)", f.elevationLossM, enabled) { v -> viewModel.onStats { copy(elevationLossM = v, dirty = dirty + "elevationLossM") } }
            GradientButton("Save stats", onClick = viewModel::saveStats, enabled = enabled && f.dirty.isNotEmpty(),
                loading = state.busy, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun NumField(label: String, value: String, enabled: Boolean, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        enabled = enabled,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
    )
}

// ── Breaks (list + delete) ────────────────────────────────────────────────────

@Composable
private fun BreaksSection(state: MapEditorUiState, viewModel: MapEditorViewModel) {
    val breaks = state.session?.breaks.orEmpty()
    val enabled = state.editable && !state.busy
    BrandCard {
        SectionHeader("Breaks")
        if (breaks.isEmpty()) {
            Text("No breaks recorded.", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            breaks.forEachIndexed { i, b ->
                if (i > 0) HorizontalDivider(Modifier.padding(vertical = 4.dp))
                BreakRow(b, enabled = enabled) { viewModel.deleteBreak(b.id) }
            }
        }
        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        AddBreakForm(enabled = enabled) { start, end, reason -> viewModel.addBreak(start, end, reason) }
    }
}

@Composable
private fun AddBreakForm(enabled: Boolean, onAdd: (Long, Long?, String?) -> Unit) {
    val context = LocalContext.current
    var start by remember { mutableStateOf<Long?>(null) }
    var end by remember { mutableStateOf<Long?>(null) }
    var reason by remember { mutableStateOf("") }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Add a break", style = MaterialTheme.typography.labelLarge)
        DateTimeField("Start", start, enabled, onClear = { start = null }) {
            pickDateTime(context, start) { start = it }
        }
        DateTimeField("End (optional — leave unset for an open break)", end, enabled, onClear = { end = null }) {
            pickDateTime(context, end) { end = it }
        }
        OutlinedTextField(
            value = reason,
            onValueChange = { reason = it },
            enabled = enabled,
            label = { Text("Reason (optional)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        )
        GradientButton(
            "Add break",
            onClick = { start?.let { onAdd(it, end, reason.ifBlank { null }) } },
            enabled = enabled && start != null,
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        )
    }
}

@Composable
private fun BreakRow(b: LiveBreak, enabled: Boolean, onDelete: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Column(Modifier.weight(1f)) {
            Text("${fmt(b.startedAt)} → ${b.endedAt?.let { fmt(it) } ?: "open"}",
                style = MaterialTheme.typography.bodyMedium)
            b.reason?.takeIf { it.isNotBlank() }?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        IconButton(onClick = onDelete, enabled = enabled) {
            Icon(Icons.Filled.Delete, contentDescription = "Delete break",
                tint = MaterialTheme.colorScheme.error)
        }
    }
}

// ── Audit ────────────────────────────────────────────────────────────────────

@Composable
private fun AuditSection(audit: List<MapAuditEntry>) {
    BrandCard {
        SectionHeader("Recent edits")
        if (audit.isEmpty()) {
            Text("No edits yet.", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            return@BrandCard
        }
        audit.take(10).forEachIndexed { i, e ->
            if (i > 0) HorizontalDivider(Modifier.padding(vertical = 4.dp))
            Column(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                Text(e.action.replace('_', ' ').replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.bodyMedium)
                Text("${e.editedByName.ifBlank { "—" }} · ${fmt(e.createdAt)}",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// ── Formatting helpers ───────────────────────────────────────────────────────

private val DISPLAY_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d, HH:mm")

private fun fmt(iso: String?): String {
    if (iso.isNullOrBlank()) return "—"
    return runCatching { OffsetDateTime.parse(iso).format(DISPLAY_FMT) }.getOrDefault(iso)
}

private fun fmtMillis(millis: Long): String =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(DISPLAY_FMT)

private fun fmtNum(v: Double): String = if (v == v.toLong().toDouble()) v.toLong().toString() else "%.1f".format(v)

/** Chain the platform date + time dialogs, yielding the chosen local instant as epoch millis. */
private fun pickDateTime(context: Context, initialMillis: Long?, onPicked: (Long) -> Unit) {
    val seed = Calendar.getInstance().apply { initialMillis?.let { timeInMillis = it } }
    DatePickerDialog(
        context,
        { _, year, month, day ->
            TimePickerDialog(
                context,
                { _, hour, minute ->
                    val c = Calendar.getInstance().apply {
                        set(year, month, day, hour, minute, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    onPicked(c.timeInMillis)
                },
                seed.get(Calendar.HOUR_OF_DAY), seed.get(Calendar.MINUTE), true,
            ).show()
        },
        seed.get(Calendar.YEAR), seed.get(Calendar.MONTH), seed.get(Calendar.DAY_OF_MONTH),
    ).show()
}

package com.taleson2wheels.app.ui.live

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import kotlinx.coroutines.launch
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.taleson2wheels.app.data.location.LiveLocationService
import com.taleson2wheels.app.data.location.LiveShareController
import com.taleson2wheels.app.data.remote.dto.LiveMetrics
import com.taleson2wheels.app.data.remote.dto.LiveRiderPosition
import com.taleson2wheels.app.ui.AppViewModelFactory
import com.taleson2wheels.app.ui.common.Avatar
import com.taleson2wheels.app.ui.common.ErrorView
import com.taleson2wheels.app.ui.common.LoadingView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveRideScreen(
    rideId: String,
    factory: AppViewModelFactory,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LiveRideViewModel = viewModel(factory = factory),
) {
    // Poll only while the screen is in the foreground: start on ON_START, stop on
    // ON_STOP/dispose. This pauses the 5s loop when the app is backgrounded or the
    // user navigates deeper, instead of polling for the whole back-stack lifetime.
    LifecycleStartEffect(rideId) {
        viewModel.start(rideId)
        onStopOrDispose { viewModel.stop() }
    }
    val state = viewModel.uiState
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val activeShareRide by LiveShareController.activeRideId.collectAsState()
    val uploaded by LiveShareController.uploaded.collectAsState()
    val isSharing = activeShareRide == rideId

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { grants ->
        val locationGranted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (locationGranted) {
            LiveLocationService.start(context, rideId)
            // A denied POST_NOTIFICATIONS doesn't stop a location FGS from
            // starting on Android 13+, but it suppresses the ongoing "sharing
            // your location" indicator — so warn the user that sharing is
            // running without a visible badge rather than letting it run
            // invisibly (a transparency + Play-policy concern).
            val notifOk = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                grants[Manifest.permission.POST_NOTIFICATIONS] == true
            if (!notifOk) {
                scope.launch {
                    snackbar.showSnackbar(
                        "Sharing started — turn on notifications to keep the ongoing 'sharing your location' indicator visible.",
                    )
                }
            }
        }
    }

    fun requestShare() {
        val locationGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        // On Android 13+ the foreground service needs POST_NOTIFICATIONS for its
        // ongoing "sharing your location" notification to show. Request it even
        // when location is already granted — otherwise background sharing runs
        // with no visible indicator (a privacy/transparency + Play-policy issue).
        val notificationNeeded = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        if (locationGranted && !notificationNeeded) {
            LiveLocationService.start(context, rideId)
        } else {
            val perms = buildList {
                add(Manifest.permission.ACCESS_FINE_LOCATION)
                add(Manifest.permission.ACCESS_COARSE_LOCATION)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) add(Manifest.permission.POST_NOTIFICATIONS)
            }
            permissionLauncher.launch(perms.toTypedArray())
        }
    }

    LaunchedEffect(state.actionMessage, state.actionError) {
        (state.actionError ?: state.actionMessage)?.let {
            snackbar.showSnackbar(it)
            viewModel.dismissActionFeedback()
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("Live ride") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        },
    ) { innerPadding ->
        when {
            state.isLoading -> LoadingView(Modifier.padding(innerPadding))
            state.error != null && state.liveState == null ->
                ErrorView(state.error, { viewModel.retry(rideId) }, Modifier.padding(innerPadding))
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item { StatusBanner(state) }
                if (state.riders.isNotEmpty()) {
                    item { LiveMapCard(state.riders, state.liveState?.leadPath ?: emptyList()) }
                }
                state.metrics?.let { item { MetricsCard(it) } }
                item { ShareCard(isSharing, uploaded, ::requestShare, onStop = { LiveLocationService.stop(context) }) }
                if (state.isLive && !state.joined) {
                    item {
                        Button(
                            onClick = { viewModel.join(rideId) },
                            enabled = !state.isJoining,
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text(if (state.isJoining) "Joining…" else "Join the live ride") }
                    }
                }
                item { OrganizerControls(state, viewModel, rideId) }
                if (state.riders.isNotEmpty()) {
                    item { SectionHeader("Riders (${state.riders.size})") }
                    items(state.riders, key = { it.userId }) { RiderRow(it) }
                }
            }
        }
    }
}

@Composable
private fun StatusBanner(state: LiveUiState) {
    val (label, color) = when {
        state.noSession -> "No live session yet" to MaterialTheme.colorScheme.onSurfaceVariant
        state.isLive && state.onBreak -> "On break" to MaterialTheme.colorScheme.tertiary
        state.isLive -> "LIVE" to MaterialTheme.colorScheme.primary
        state.isPaused -> "Paused" to MaterialTheme.colorScheme.tertiary
        state.isEnded -> "Ride ended" to MaterialTheme.colorScheme.onSurfaceVariant
        else -> (state.status ?: "Waiting") to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.titleLarge, color = color, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
internal fun MetricsCard(m: LiveMetrics) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Metric("Distance", "${"%.1f".format(m.distanceKm)} km", Modifier.weight(1f))
                Metric("Avg", "${m.avgSpeedKmh.toInt()} km/h", Modifier.weight(1f))
                Metric("Max", "${m.maxSpeedKmh.toInt()} km/h", Modifier.weight(1f))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Metric("Riders", "${m.riderCount}", Modifier.weight(1f))
                Metric("Elapsed", "${m.elapsedMinutes.toInt()} min", Modifier.weight(1f))
                Metric("Moving", "${m.movingMinutes.toInt()} min", Modifier.weight(1f))
            }
            if (m.breakCount > 0) {
                Text(
                    "${m.breakCount} break(s) · ${m.breakMinutes.toInt()} min",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun Metric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(value, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun ShareCard(isSharing: Boolean, uploaded: Int, onShare: () -> Unit, onStop: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Share my location", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
            if (isSharing) {
                Text(
                    "Sharing live · $uploaded sent · keeps running in the background",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                OutlinedButton(onClick = onStop, modifier = Modifier.fillMaxWidth()) { Text("Stop sharing") }
            } else {
                Text(
                    "Broadcast your position to the group while the ride is live. Tracking continues even with the screen off.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                FilledTonalButton(onClick = onShare, modifier = Modifier.fillMaxWidth()) { Text("Start sharing") }
            }
        }
    }
}

@Composable
private fun OrganizerControls(state: LiveUiState, viewModel: LiveRideViewModel, rideId: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Organizer controls", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
            Text(
                "Only ride leads can start or control the session.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                when {
                    state.noSession || state.isEnded ->
                        ControlButton("Start", !state.actionInFlight, Modifier.weight(1f)) { viewModel.control(rideId, "start") }
                    state.isPaused ->
                        ControlButton("Resume", !state.actionInFlight, Modifier.weight(1f)) { viewModel.control(rideId, "resume") }
                    else ->
                        ControlButton("Pause", !state.actionInFlight, Modifier.weight(1f)) { viewModel.control(rideId, "pause") }
                }
                if (state.isLive || state.isPaused) {
                    ControlButton("End", !state.actionInFlight, Modifier.weight(1f)) { viewModel.control(rideId, "end") }
                }
            }
            if (state.isLive) {
                OutlinedButton(
                    onClick = { viewModel.toggleBreak(rideId) },
                    enabled = !state.actionInFlight,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(if (state.onBreak) "End break" else "Start break") }
            }
        }
    }
}

@Composable
private fun ControlButton(label: String, enabled: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(onClick = onClick, enabled = enabled, modifier = modifier) { Text(label) }
}

@Composable
internal fun RiderRow(rider: LiveRiderPosition) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Avatar(url = rider.userAvatar, name = rider.userName, size = 36.dp)
            Column(Modifier.weight(1f)) {
                Text(rider.userName.ifBlank { "Rider" }, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                val tags = buildList {
                    if (rider.isLead) add("Lead")
                    if (rider.isSweep) add("Sweep")
                    if (rider.isDeviated) add("Off-route")
                    rider.speed?.let { add("${(it * 3.6).toInt()} km/h") }
                }
                if (tags.isNotEmpty()) {
                    Text(
                        tags.joinToString(" · "),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (rider.isDeviated) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(text, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.SemiBold)
}

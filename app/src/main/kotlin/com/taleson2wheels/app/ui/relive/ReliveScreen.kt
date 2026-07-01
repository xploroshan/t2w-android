package com.taleson2wheels.app.ui.relive

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.taleson2wheels.app.BuildConfig
import com.taleson2wheels.app.data.remote.dto.LiveRiderPosition
import com.taleson2wheels.app.ui.AppViewModelFactory
import com.taleson2wheels.app.ui.common.ErrorView
import com.taleson2wheels.app.ui.common.LoadingView
import com.taleson2wheels.app.ui.components.BrandBackground
import com.taleson2wheels.app.ui.components.BrandCard
import com.taleson2wheels.app.ui.theme.T2WAccent
import com.taleson2wheels.app.ui.theme.T2WGold
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReliveScreen(
    rideId: String,
    factory: AppViewModelFactory,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ReliveViewModel = viewModel(factory = factory),
) {
    LaunchedEffect(rideId) { viewModel.load(rideId) }
    val state = viewModel.uiState
    val token = BuildConfig.MAPBOX_ACCESS_TOKEN

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Relive · ${state.selectedRiderName}") },
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
    ) { innerPadding ->
        BrandBackground(Modifier.padding(innerPadding)) {
            when {
                state.isLoading -> LoadingView()
                state.loadError != null ->
                    ErrorView(state.loadError, onRetry = { viewModel.reload() })
                else -> ReliveBody(state, token, viewModel)
            }
        }
    }
}

@Composable
private fun ReliveBody(
    state: ReliveUiState,
    token: String,
    viewModel: ReliveViewModel,
) {
    Column(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            when {
                token.isBlank() -> TokenPlaceholder(Modifier.fillMaxSize())
                !state.hasTrack -> NoTrackPlaceholder(Modifier.fillMaxSize())
                else -> ReliveMap(
                    token = token,
                    recorded = state.track,
                    planned = state.plannedRoute,
                    sample = state.sample,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            // HUD overlays the map (only meaningful when a track is playing).
            if (token.isNotBlank() && state.hasTrack) {
                HudRow(state, Modifier.align(Alignment.TopStart).padding(12.dp))
            }
        }

        ControlsCard(state, viewModel)
    }
}

@Composable
private fun HudRow(state: ReliveUiState, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        val s = state.sample
        HudPill("Speed", s?.speedKmh?.let { "${it.roundToInt()} km/h" } ?: "—", T2WAccent)
        HudPill("Elevation", s?.elevationM?.let { "${it.roundToInt()} m" } ?: "—", T2WGold)
        HudPill("Progress", "${(state.progress * 100).roundToInt()}%", MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun HudPill(label: String, value: String, valueColor: Color) {
    Box(
        Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color.Black.copy(alpha = 0.55f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Column(horizontalAlignment = Alignment.Start) {
            Text(value, style = MaterialTheme.typography.titleMedium, color = valueColor, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ControlsCard(state: ReliveUiState, viewModel: ReliveViewModel) {
    BrandCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (state.riders.size > 1) {
                RiderSelector(state, onSelect = viewModel::selectRider)
            }

            // Scrubber with elapsed / total clock labels.
            Slider(
                value = state.progress,
                onValueChange = { viewModel.seekToProgress(it) },
                enabled = state.hasTrack,
                colors = SliderDefaults.colors(
                    thumbColor = T2WAccent,
                    activeTrackColor = T2WAccent,
                ),
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    RelivePlayback.formatClock(state.playbackMs),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    RelivePlayback.formatClock(state.durationMs),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                PlayButton(playing = state.isPlaying, enabled = state.hasTrack, onClick = viewModel::togglePlay)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ReliveSpeed.entries.forEach { speed ->
                        SpeedChip(
                            speed = speed,
                            selected = state.speed == speed,
                            onClick = { viewModel.setSpeed(speed) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayButton(playing: Boolean, enabled: Boolean, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .size(56.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(T2WAccent.copy(alpha = if (enabled) 1f else 0.4f)),
    ) {
        Icon(
            imageVector = if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
            contentDescription = if (playing) "Pause" else "Play",
            tint = Color.White,
            modifier = Modifier.size(30.dp),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SpeedChip(speed: ReliveSpeed, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(speed.label) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = T2WAccent,
            selectedLabelColor = Color.White,
        ),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RiderSelector(state: ReliveUiState, onSelect: (String) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        state.riders.forEach { rider: LiveRiderPosition ->
            FilterChip(
                selected = state.selectedRiderId == rider.userId,
                onClick = { onSelect(rider.userId) },
                enabled = !state.switchingRider,
                label = { Text(rider.userName.ifBlank { "Rider" }) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = T2WAccent,
                    selectedLabelColor = Color.White,
                ),
            )
        }
    }
}

@Composable
private fun TokenPlaceholder(modifier: Modifier = Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Column(
            Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                "3D flyover needs a Mapbox token",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "Add a Mapbox public token (MAPBOX_ACCESS_TOKEN) in secrets.properties to enable the terrain flyover. See docs/SETUP_SECRETS.md.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun NoTrackPlaceholder(modifier: Modifier = Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Text(
            "No recorded track for this rider yet.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(24.dp),
        )
    }
}

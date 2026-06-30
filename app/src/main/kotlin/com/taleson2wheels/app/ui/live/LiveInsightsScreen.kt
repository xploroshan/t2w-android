package com.taleson2wheels.app.ui.live

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.taleson2wheels.app.data.remote.dto.AnalyticsRider
import com.taleson2wheels.app.data.remote.dto.LiveMetrics
import com.taleson2wheels.app.ui.AppViewModelFactory
import com.taleson2wheels.app.ui.common.Avatar
import com.taleson2wheels.app.ui.common.ErrorView
import com.taleson2wheels.app.ui.common.LoadingView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveInsightsScreen(
    rideId: String,
    factory: AppViewModelFactory,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LiveInsightsViewModel = viewModel(factory = factory),
) {
    LaunchedEffect(rideId) { viewModel.load(rideId) }
    val state = viewModel.uiState

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Ride insights") },
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
            state.isEmpty -> ErrorView(
                state.error ?: "No insights available for this ride yet.",
                { viewModel.load(rideId) },
                Modifier.padding(innerPadding),
            )
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                state.metrics?.let { item { SummaryCard(it) } }
                if (state.elevation.size >= 2) {
                    item { ElevationCard(state.elevation) }
                }
                val board = state.analytics?.leaderboard ?: emptyList()
                if (board.isNotEmpty()) {
                    item { SectionHeader("Distance leaderboard") }
                    itemsIndexed(board, key = { _, r -> r.userId.ifBlank { r.name } }) { i, rider ->
                        LeaderRow(rank = i + 1, rider = rider)
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(m: LiveMetrics) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Summary", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Stat("Distance", "${"%.1f".format(m.distanceKm)} km", Modifier.weight(1f))
                Stat("Avg", "${m.avgSpeedKmh.toInt()} km/h", Modifier.weight(1f))
                Stat("Max", "${m.maxSpeedKmh.toInt()} km/h", Modifier.weight(1f))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Stat("Moving", "${m.movingMinutes.toInt()} min", Modifier.weight(1f))
                Stat("Elapsed", "${m.elapsedMinutes.toInt()} min", Modifier.weight(1f))
                Stat("Breaks", "${m.breakCount}", Modifier.weight(1f))
            }
            if (m.elevationGainM != null || m.elevationLossM != null) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Stat("Climb", "${(m.elevationGainM ?: 0.0).toInt()} m", Modifier.weight(1f))
                    Stat("Descent", "${(m.elevationLossM ?: 0.0).toInt()} m", Modifier.weight(1f))
                    Stat("Riders", "${m.riderCount}", Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(value, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun ElevationCard(altitudes: List<Float>) {
    val primary = MaterialTheme.colorScheme.primary
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Elevation profile", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
            val min = altitudes.min()
            val max = altitudes.max()
            Canvas(modifier = Modifier.fillMaxWidth().height(120.dp)) {
                val range = (max - min).takeIf { it > 0f } ?: 1f
                val dx = if (altitudes.size > 1) size.width / (altitudes.size - 1) else size.width
                val path = Path()
                altitudes.forEachIndexed { i, alt ->
                    val x = dx * i
                    val y = size.height - ((alt - min) / range) * size.height
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                drawPath(path, color = primary, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f))
                // Baseline fill anchor points
                path.lineTo(size.width, size.height)
                path.lineTo(0f, size.height)
                path.close()
                drawPath(path, color = primary.copy(alpha = 0.12f))
                drawCircle(primary, radius = 3f, center = Offset(0f, size.height - ((altitudes.first() - min) / range) * size.height))
            }
            Text(
                "${min.toInt()} m – ${max.toInt()} m  ·  ${altitudes.size} samples (lead rider)",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun LeaderRow(rank: Int, rider: AnalyticsRider) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("$rank", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, modifier = Modifier.width(24.dp))
            Avatar(url = rider.avatar, name = rider.name)
            Column(Modifier.weight(1f)) {
                Text(rider.name.ifBlank { "Rider" }, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    "${rider.movingMinutes.toInt()} min · ${rider.avgSpeedKmh.toInt()} km/h avg",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text("${"%.1f".format(rider.distanceKm)} km", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(text, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.SemiBold)
}

package com.taleson2wheels.app.ui.riders

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.taleson2wheels.app.data.remote.dto.RiderDto
import com.taleson2wheels.app.ui.AppViewModelFactory
import com.taleson2wheels.app.ui.common.Avatar
import com.taleson2wheels.app.ui.common.ErrorView
import com.taleson2wheels.app.ui.common.LoadingView
import com.taleson2wheels.app.ui.components.BrandBackground
import com.taleson2wheels.app.ui.components.BrandCard
import com.taleson2wheels.app.ui.theme.T2WBronze
import com.taleson2wheels.app.ui.theme.T2WGold
import com.taleson2wheels.app.ui.theme.T2WSilver

/** Medal color for a 1-based rank (gold/silver/bronze for the top three). */
private fun rankColor(rank: Int): Color? = when (rank) {
    1 -> T2WGold
    2 -> T2WSilver
    3 -> T2WBronze
    else -> null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardScreen(
    factory: AppViewModelFactory,
    onRiderClick: (String) -> Unit,
    onOpenAchievements: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LeaderboardViewModel = viewModel(factory = factory),
) {
    val state = viewModel.uiState
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Leaderboard") },
                actions = {
                    IconButton(onClick = onOpenAchievements) {
                        Icon(Icons.Filled.MilitaryTech, contentDescription = "Achievements")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    actionIconContentColor = MaterialTheme.colorScheme.primary,
                ),
            )
        },
    ) { innerPadding ->
        BrandBackground(Modifier.padding(innerPadding)) {
            Column(modifier = Modifier.fillMaxSize()) {
                OutlinedTextField(
                    value = state.query,
                    onValueChange = viewModel::onQueryChange,
                    label = { Text("Search riders") },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    trailingIcon = {
                        if (state.query.isNotEmpty()) {
                            IconButton(onClick = viewModel::clearQuery) {
                                Icon(Icons.Filled.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    shape = MaterialTheme.shapes.small,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Search),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                )

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    LeaderboardPeriod.entries.forEach { period ->
                        FilterChip(
                            selected = state.period == period,
                            onClick = { viewModel.setPeriod(period) },
                            label = { Text(period.label) },
                        )
                    }
                }

                // Podium only on the unfiltered leaderboard with a full top three.
                val showPodium = state.query.isBlank() && state.riders.size >= 3
                when {
                    state.isLoading && state.riders.isEmpty() -> LoadingView()
                    state.error != null && state.riders.isEmpty() ->
                        ErrorView(state.error, viewModel::refresh)
                    state.riders.isEmpty() ->
                        ErrorView(
                            if (state.query.isNotBlank()) "No riders match \"${state.query}\"." else "No riders yet.",
                            viewModel::refresh,
                        )
                    else -> LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        val rows = if (showPodium) {
                            item(key = "podium") { Podium(state.riders.take(3), onRiderClick) }
                            state.riders.drop(3) to 4
                        } else {
                            state.riders to 1
                        }
                        itemsIndexed(rows.first, key = { _, r -> r.id }) { index, rider ->
                            RiderRow(rank = index + rows.second, rider = rider, onClick = { onRiderClick(rider.id) })
                        }
                        if (state.canLoadMore || state.loadMoreError != null) {
                            item(key = "load-more") {
                                if (state.loadMoreError != null) {
                                    Text(
                                        text = "Couldn't load more — tap to retry",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.primary,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { viewModel.loadMore() }
                                            .padding(16.dp),
                                    )
                                } else {
                                    LaunchedEffect(state.nextCursor) { viewModel.loadMore() }
                                    LoadingView(Modifier.fillMaxWidth().padding(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Top-three podium: 2nd · 1st (raised) · 3rd, each ringed in its medal color. */
@Composable
internal fun Podium(top3: List<RiderDto>, onRiderClick: (String) -> Unit) {
    BrandCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            top3.getOrNull(1)?.let { PodiumSpot(it, 2, big = false, Modifier.weight(1f), onRiderClick) }
            top3.getOrNull(0)?.let { PodiumSpot(it, 1, big = true, Modifier.weight(1f), onRiderClick) }
            top3.getOrNull(2)?.let { PodiumSpot(it, 3, big = false, Modifier.weight(1f), onRiderClick) }
        }
    }
}

@Composable
private fun PodiumSpot(rider: RiderDto, rank: Int, big: Boolean, modifier: Modifier = Modifier, onRiderClick: (String) -> Unit) {
    val color = rankColor(rank) ?: MaterialTheme.colorScheme.primary
    Column(
        modifier = modifier.clickable { onRiderClick(rider.id) },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            Modifier.border(2.dp, color, CircleShape).padding(3.dp),
            contentAlignment = Alignment.Center,
        ) {
            Avatar(url = rider.avatarUrl, name = rider.name, size = if (big) 72.dp else 56.dp)
        }
        Text("#$rank", style = MaterialTheme.typography.labelLarge, color = color, fontWeight = FontWeight.Bold)
        Text(
            text = rider.name,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = "${rider.totalPoints.toInt()} pts",
            style = MaterialTheme.typography.labelMedium,
            color = color,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
internal fun RiderRow(rank: Int, rider: RiderDto, onClick: () -> Unit) {
    val medal = rankColor(rank)
    BrandCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "$rank",
                style = MaterialTheme.typography.titleLarge,
                color = medal ?: MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(28.dp),
            )
            Avatar(url = rider.avatarUrl, name = rider.name)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = rider.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${rider.ridesCompleted} rides · ${rider.totalKm.toInt()} km",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = "${rider.totalPoints.toInt()} pts",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

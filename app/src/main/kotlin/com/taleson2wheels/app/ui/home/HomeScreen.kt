package com.taleson2wheels.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.taleson2wheels.app.data.remote.dto.NotificationDto
import com.taleson2wheels.app.data.remote.dto.StatsDto
import com.taleson2wheels.app.ui.AppViewModelFactory
import com.taleson2wheels.app.ui.common.ErrorView
import com.taleson2wheels.app.ui.common.LoadingView
import com.taleson2wheels.app.ui.components.BrandBackground
import com.taleson2wheels.app.ui.components.BrandCard
import com.taleson2wheels.app.ui.components.BrandWordmark
import com.taleson2wheels.app.ui.components.SectionHeader
import com.taleson2wheels.app.ui.rides.RideCardItem
import com.taleson2wheels.app.ui.theme.HeroGradient
import com.taleson2wheels.app.ui.theme.T2WAccent
import com.taleson2wheels.app.ui.theme.T2WGold

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    factory: AppViewModelFactory,
    onOpenNotifications: () -> Unit,
    onRideClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel(factory = factory),
) {
    val state = viewModel.uiState
    val unread = state.notifications.count { !it.isRead }
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            // Minimal, transparent bar — the brand wordmark lives in the hero
            // below; the bar only carries the notifications affordance.
            TopAppBar(
                title = {},
                actions = {
                    IconButton(onClick = onOpenNotifications) {
                        BadgedBox(badge = { if (unread > 0) Badge { Text("$unread") } }) {
                            Icon(Icons.Filled.Notifications, contentDescription = "Notifications")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    actionIconContentColor = MaterialTheme.colorScheme.onBackground,
                ),
            )
        },
    ) { innerPadding ->
        when {
            state.isLoading -> LoadingView(Modifier.padding(innerPadding))
            state.error != null && state.stats == null ->
                ErrorView(state.error, viewModel::refresh, Modifier.padding(innerPadding))
            else -> BrandBackground(Modifier.padding(innerPadding)) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    item { Hero(userName = state.userName) }

                    state.stats?.let { stats -> item { StatsGrid(stats) } }

                    if (state.upcomingRides.isNotEmpty()) {
                        item { SectionHeader("Upcoming rides") }
                        items(state.upcomingRides, key = { it.id }) { ride ->
                            RideCardItem(ride = ride, onClick = { onRideClick(ride.id) })
                        }
                    }

                    if (state.notifications.isNotEmpty()) {
                        item { SectionHeader("What's new") }
                        items(state.notifications, key = { it.id }) { NotificationCard(it) }
                    }
                }
            }
        }
    }
}

/** Branded hero band — gradient backdrop, Courgette wordmark, greeting + tagline. */
@Composable
internal fun Hero(userName: String?) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(HeroGradient)
            .padding(24.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            BrandWordmark(style = MaterialTheme.typography.headlineMedium)
            Text(
                text = userName?.let { "Welcome back, $it" } ?: "Welcome to the crew",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Ride. Capture. Share.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
internal fun StatsGrid(stats: StatsDto) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            StatCard("Active riders", stats.activeRiders.toString(), T2WAccent, Modifier.weight(1f))
            StatCard("Rides done", stats.ridesCompleted.toString(), T2WGold, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            StatCard("Kms covered", stats.kmsCovered.toString(), T2WAccent, Modifier.weight(1f))
            StatCard("Countries", stats.countriesRidden.toString(), T2WGold, Modifier.weight(1f))
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, valueColor: Color, modifier: Modifier = Modifier) {
    BrandCard(modifier = modifier) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            color = valueColor,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
internal fun NotificationCard(notification: NotificationDto) {
    BrandCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = notification.title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = notification.message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

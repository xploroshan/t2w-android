package com.taleson2wheels.app.ui.rides

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.taleson2wheels.app.R
import com.taleson2wheels.app.data.remote.dto.RideCard
import com.taleson2wheels.app.ui.AppViewModelFactory
import com.taleson2wheels.app.ui.common.ErrorView
import com.taleson2wheels.app.ui.common.LoadingView
import com.taleson2wheels.app.ui.components.BrandBackground
import com.taleson2wheels.app.ui.components.BrandCard
import com.taleson2wheels.app.ui.components.TagChip

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RidesScreen(
    factory: AppViewModelFactory,
    onRideClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RidesViewModel = viewModel(factory = factory),
) {
    val state = viewModel.uiState
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.rides_title)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                ),
            )
        },
    ) { innerPadding ->
        BrandBackground(Modifier.padding(innerPadding)) {
            when {
                state.isLoading && state.rides.isEmpty() -> LoadingView()

                state.error != null && state.rides.isEmpty() ->
                    ErrorView(message = state.error, onRetry = viewModel::refresh)

                state.rides.isEmpty() ->
                    ErrorView(message = stringResource(R.string.rides_empty), onRetry = viewModel::refresh)

                else -> PullToRefreshBox(
                    isRefreshing = state.isLoading,
                    onRefresh = viewModel::refresh,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(state.rides, key = { it.id }) { ride ->
                            RideCardItem(ride = ride, onClick = { onRideClick(ride.id) })
                        }
                        if (state.canLoadMore || state.loadMoreError != null) {
                            item(key = "load-more") {
                                if (state.loadMoreError != null) {
                                    // A failed page must not silently spin — let the user retry.
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

@Composable
internal fun RideCardItem(ride: RideCard, onClick: () -> Unit) {
    BrandCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = ride.title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            ride.status?.let { status -> TagChip(status.replaceFirstChar { it.uppercase() }) }
        }
        Text(
            text = buildString {
                ride.rideNumber?.let { append("#").append(it).append("  ·  ") }
                append(ride.distanceKm.toInt()).append(" km")
                ride.difficulty?.let { append("  ·  ").append(it) }
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp),
        )
        val registered = ride.registeredRiders
        if (registered > 0 || ride.myRegistrationStatus != null) {
            Text(
                text = buildString {
                    append(registered).append(" riders")
                    ride.myRegistrationStatus?.let { append("  ·  you: ").append(it) }
                },
                style = MaterialTheme.typography.bodyMedium,
                color = if (ride.myRegistrationStatus != null) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                fontWeight = if (ride.myRegistrationStatus != null) FontWeight.SemiBold else FontWeight.Normal,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

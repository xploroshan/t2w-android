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
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.taleson2wheels.app.R
import com.taleson2wheels.app.data.remote.dto.RideCard
import com.taleson2wheels.app.ui.AppViewModelFactory
import com.taleson2wheels.app.ui.common.ErrorView
import com.taleson2wheels.app.ui.common.LoadingView

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
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.rides_title)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        },
    ) { innerPadding ->
        when {
            state.isLoading && state.rides.isEmpty() ->
                LoadingView(Modifier.padding(innerPadding))

            state.error != null && state.rides.isEmpty() ->
                ErrorView(
                    message = state.error,
                    onRetry = viewModel::refresh,
                    modifier = Modifier.padding(innerPadding),
                )

            state.rides.isEmpty() ->
                ErrorView(
                    message = stringResource(R.string.rides_empty),
                    onRetry = viewModel::refresh,
                    modifier = Modifier.padding(innerPadding),
                )

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(state.rides, key = { it.id }) { ride ->
                    RideCardItem(ride = ride, onClick = { onRideClick(ride.id) })
                }
                if (state.canLoadMore) {
                    item(key = "load-more") {
                        // Trigger the next page when the footer scrolls into view.
                        LaunchedEffect(state.nextCursor) { viewModel.loadMore() }
                        LoadingView(Modifier.fillMaxWidth().padding(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun RideCardItem(ride: RideCard, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = ride.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                ride.status?.let { status ->
                    Text(
                        text = status.replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            Text(
                text = buildString {
                    ride.rideNumber?.let { append("#").append(it).append("  ·  ") }
                    append(ride.distanceKm.toInt()).append(" km")
                    ride.difficulty?.let { append("  ·  ").append(it) }
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 4.dp),
            )
            val registered = ride.registeredRiders
            if (registered > 0 || ride.myRegistrationStatus != null) {
                Text(
                    text = buildString {
                        append(registered).append(" riders")
                        ride.myRegistrationStatus?.let { append("  ·  you: ").append(it) }
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }
}

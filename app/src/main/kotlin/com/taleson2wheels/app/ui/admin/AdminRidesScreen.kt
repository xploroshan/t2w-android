package com.taleson2wheels.app.ui.admin

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.taleson2wheels.app.data.remote.dto.RideCard
import com.taleson2wheels.app.ui.AppViewModelFactory
import com.taleson2wheels.app.ui.common.ErrorView
import com.taleson2wheels.app.ui.common.LoadingView
import com.taleson2wheels.app.ui.components.BrandBackground
import com.taleson2wheels.app.ui.components.BrandCard
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminRidesScreen(
    factory: AppViewModelFactory,
    onBack: () -> Unit,
    onCreateRide: () -> Unit,
    onEditRide: (String) -> Unit,
    onOpenParticipation: (rideId: String, rideTitle: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AdminRidesViewModel = viewModel(factory = factory),
) {
    val state = viewModel.uiState
    val snackbarHostState = remember { SnackbarHostState() }
    var deleteTarget by remember { mutableStateOf<RideCard?>(null) }

    // Re-fetch when returning from the editor so a create/edit shows up.
    LaunchedEffect(Unit) { viewModel.refresh() }
    LaunchedEffect(state.actionError) {
        state.actionError?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearActionError()
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreateRide,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("New ride") },
            )
        },
        topBar = {
            TopAppBar(
                title = { Text("Rides") },
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
                state.isLoading && state.items.isEmpty() -> LoadingView()
                state.error != null && state.items.isEmpty() -> ErrorView(state.error, viewModel::refresh)
                state.items.isEmpty() -> ErrorView("No rides yet.", viewModel::refresh)
                else -> PullToRefreshBox(
                    isRefreshing = state.isLoading,
                    onRefresh = viewModel::refresh,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(state.items, key = { it.id }) { ride ->
                            RideRow(
                                ride = ride,
                                busy = ride.id in state.pendingActionIds,
                                onEdit = { onEditRide(ride.id) },
                                onParticipation = { onOpenParticipation(ride.id, ride.title) },
                                onDelete = { deleteTarget = ride },
                            )
                        }
                        if (state.canLoadMore || state.loadMoreError != null) {
                            item(key = "load-more") {
                                if (state.loadMoreError != null) {
                                    Text(
                                        text = "Couldn't load more — tap to retry",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.primary,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth().clickable { viewModel.loadMore() }.padding(16.dp),
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

    deleteTarget?.let { ride ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete ride?") },
            text = { Text("“${ride.title}” and its registrations, participation and posts will be permanently removed. This can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    deleteTarget = null
                    viewModel.deleteRide(ride.id)
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun RideRow(
    ride: RideCard,
    busy: Boolean,
    onEdit: () -> Unit,
    onParticipation: () -> Unit,
    onDelete: () -> Unit,
) {
    BrandCard(modifier = Modifier.fillMaxWidth().clickable(enabled = !busy, onClick = onEdit)) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        ride.title.ifBlank { "Untitled ride" },
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                    )
                    val meta = listOfNotNull(
                        ride.rideNumber?.takeIf { it.isNotBlank() },
                        formatRideDate(ride.startDate),
                    ).joinToString(" · ")
                    if (meta.isNotBlank()) {
                        Text(meta, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                RideOverflowMenu(enabled = !busy, onParticipation = onParticipation, onDelete = onDelete)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ride.status?.takeIf { it.isNotBlank() }?.let { StatusChip(it) }
                if (ride.registeredRiders > 0) StatusChip("${ride.registeredRiders} riders")
            }
        }
    }
}

@Composable
private fun RideOverflowMenu(enabled: Boolean, onParticipation: () -> Unit, onDelete: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    IconButton(onClick = { expanded = true }, enabled = enabled) {
        Icon(Icons.Default.MoreVert, contentDescription = "Actions", tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        DropdownMenuItem(
            text = { Text("Participation") },
            onClick = { expanded = false; onParticipation() },
        )
        DropdownMenuItem(
            text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
            onClick = { expanded = false; onDelete() },
        )
    }
}

@Composable
private fun StatusChip(text: String) {
    AssistChip(
        onClick = {},
        enabled = false,
        label = { Text(text, style = MaterialTheme.typography.labelSmall) },
        colors = AssistChipDefaults.assistChipColors(
            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    )
}

private val rideDateFormatter = DateTimeFormatter.ofPattern("d MMM yyyy")

private fun formatRideDate(iso: String?): String? {
    val ms = parseIsoToMillis(iso) ?: return null
    return Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).format(rideDateFormatter)
}

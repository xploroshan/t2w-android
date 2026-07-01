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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.taleson2wheels.app.data.remote.dto.ParticipationRow
import com.taleson2wheels.app.ui.AppViewModelFactory
import com.taleson2wheels.app.ui.common.Avatar
import com.taleson2wheels.app.ui.common.ErrorView
import com.taleson2wheels.app.ui.common.LoadingView
import com.taleson2wheels.app.ui.components.BrandBackground
import com.taleson2wheels.app.ui.components.BrandCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminParticipationScreen(
    rideId: String,
    rideTitle: String,
    factory: AppViewModelFactory,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AdminParticipationViewModel = viewModel(factory = factory),
) {
    val state = viewModel.uiState
    val snackbarHostState = remember { SnackbarHostState() }
    var pointsTarget by remember { mutableStateOf<ParticipationRow?>(null) }

    LaunchedEffect(rideId) { viewModel.load(rideId) }
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
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Participation")
                        rideTitle.takeIf { it.isNotBlank() }?.let {
                            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
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
                state.items.isEmpty() -> ErrorView("No participants yet. Add riders from the web admin.", viewModel::refresh)
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
                        items(state.items, key = { it.riderProfileId }) { row ->
                            ParticipantCard(
                                row = row,
                                busy = row.riderProfileId in state.pendingActionIds,
                                onEditPoints = { pointsTarget = row },
                                onToggleDroppedOut = { viewModel.setDroppedOut(row.riderProfileId, !row.droppedOut) },
                                onRemove = { viewModel.remove(row.riderProfileId) },
                            )
                        }
                    }
                }
            }
        }
    }

    pointsTarget?.let { row ->
        PointsDialog(
            row = row,
            onDismiss = { pointsTarget = null },
            onSave = { points ->
                pointsTarget = null
                viewModel.setPoints(row.riderProfileId, points)
            },
        )
    }
}

@Composable
private fun ParticipantCard(
    row: ParticipationRow,
    busy: Boolean,
    onEditPoints: () -> Unit,
    onToggleDroppedOut: () -> Unit,
    onRemove: () -> Unit,
) {
    BrandCard(modifier = Modifier.fillMaxWidth().clickable(enabled = !busy, onClick = onEditPoints)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Avatar(url = row.avatarUrl, name = row.riderName, size = 44.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    row.riderName.ifBlank { "Rider" },
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PointsChip("${formatPoints(row.points)} pts")
                    if (row.droppedOut) PointsChip("Dropped out")
                }
            }
            ParticipantOverflowMenu(
                droppedOut = row.droppedOut,
                enabled = !busy,
                onToggleDroppedOut = onToggleDroppedOut,
                onRemove = onRemove,
            )
        }
    }
}

@Composable
private fun ParticipantOverflowMenu(
    droppedOut: Boolean,
    enabled: Boolean,
    onToggleDroppedOut: () -> Unit,
    onRemove: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    IconButton(onClick = { expanded = true }, enabled = enabled) {
        Icon(Icons.Default.MoreVert, contentDescription = "Actions", tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        DropdownMenuItem(
            text = { Text(if (droppedOut) "Restore" else "Mark dropped out") },
            onClick = { expanded = false; onToggleDroppedOut() },
        )
        DropdownMenuItem(
            text = { Text("Remove", color = MaterialTheme.colorScheme.error) },
            onClick = { expanded = false; onRemove() },
        )
    }
}

@Composable
private fun PointsDialog(row: ParticipationRow, onDismiss: () -> Unit, onSave: (Double) -> Unit) {
    var text by remember { mutableStateOf(formatPoints(row.points)) }
    val parsed = text.toDoubleOrNull()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Points for ${row.riderName.ifBlank { "rider" }}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Points") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                Text(
                    "Setting points to 0 removes this rider from the ride.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(enabled = parsed != null, onClick = { parsed?.let(onSave) }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun PointsChip(text: String) {
    AssistChip(
        onClick = {},
        enabled = false,
        label = { Text(text, style = MaterialTheme.typography.labelSmall) },
        colors = AssistChipDefaults.assistChipColors(
            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    )
}

/** "12.0" → "12", "12.5" → "12.5". */
private fun formatPoints(v: Double): String =
    if (v == v.toLong().toDouble()) v.toLong().toString() else v.toString()

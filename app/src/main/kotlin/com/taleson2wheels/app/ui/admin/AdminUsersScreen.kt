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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import com.taleson2wheels.app.data.remote.dto.AdminUser
import com.taleson2wheels.app.ui.AppViewModelFactory
import com.taleson2wheels.app.ui.common.ErrorView
import com.taleson2wheels.app.ui.common.LoadingView
import com.taleson2wheels.app.ui.common.OnBottomReached
import com.taleson2wheels.app.ui.components.BrandBackground
import com.taleson2wheels.app.ui.components.BrandCard

private data class StatusFilter(val label: String, val value: String?)

private val STATUS_FILTERS = listOf(
    StatusFilter("Pending", "pending"),
    StatusFilter("Active", "active"),
    StatusFilter("All", null),
)

private val ASSIGNABLE_ROLES = listOf("rider", "t2w_rider", "core_member", "superadmin")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminUsersScreen(
    factory: AppViewModelFactory,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AdminUsersViewModel = viewModel(factory = factory),
) {
    val state = viewModel.uiState
    val snackbarHostState = remember { SnackbarHostState() }
    var roleDialogFor by remember { mutableStateOf<AdminUser?>(null) }

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
                title = { Text("Users") },
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
            Column(Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    STATUS_FILTERS.forEach { f ->
                        FilterChip(
                            selected = state.status == f.value,
                            onClick = { viewModel.setStatus(f.value) },
                            label = { Text(f.label) },
                        )
                    }
                }
                when {
                    state.isLoading && state.items.isEmpty() -> LoadingView()
                    state.error != null && state.items.isEmpty() -> ErrorView(state.error, viewModel::refresh)
                    state.items.isEmpty() -> ErrorView("No users here.", viewModel::refresh)
                    else -> {
                        val listState = rememberLazyListState()
                        // Fetch the next page from the actual scroll position, not the
                        // footer's composition, so a tall viewport doesn't auto-chain pages.
                        listState.OnBottomReached { viewModel.loadMore() }
                        PullToRefreshBox(
                        isRefreshing = state.isLoading,
                        onRefresh = viewModel::refresh,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            items(state.items, key = { it.id }) { user ->
                                UserCard(
                                    user = user,
                                    busy = user.id in state.pendingActionIds,
                                    onApprove = { viewModel.approve(user.id) },
                                    onReject = { viewModel.reject(user.id) },
                                    onToggleBlock = { viewModel.setBlocked(user.id, !user.blocked) },
                                    onChangeRole = { roleDialogFor = user },
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
    }

    roleDialogFor?.let { user ->
        RoleDialog(
            user = user,
            onDismiss = { roleDialogFor = null },
            onPick = { role ->
                roleDialogFor = null
                if (role != user.role) viewModel.setRole(user.id, role)
            },
        )
    }
}

@Composable
private fun UserCard(
    user: AdminUser,
    busy: Boolean,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    onToggleBlock: () -> Unit,
    onChangeRole: () -> Unit,
) {
    BrandCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        user.name.ifBlank { "Rider" },
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(user.email, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                OverflowMenu(user = user, enabled = !busy, onToggleBlock = onToggleBlock, onChangeRole = onChangeRole)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Badge(roleLabel(user.role))
                if (!user.isApproved) Badge("Pending")
                if (user.blocked) Badge("Blocked")
            }
            if (!user.isApproved) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    TextButton(onClick = onApprove, enabled = !busy) { Text("Approve") }
                    TextButton(onClick = onReject, enabled = !busy) { Text("Reject") }
                }
            }
        }
    }
}

@Composable
private fun OverflowMenu(
    user: AdminUser,
    enabled: Boolean,
    onToggleBlock: () -> Unit,
    onChangeRole: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    IconButton(onClick = { expanded = true }, enabled = enabled) {
        Icon(Icons.Default.MoreVert, contentDescription = "Actions", tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        DropdownMenuItem(
            text = { Text(if (user.blocked) "Unblock" else "Block") },
            onClick = { expanded = false; onToggleBlock() },
        )
        DropdownMenuItem(
            text = { Text("Change role") },
            onClick = { expanded = false; onChangeRole() },
        )
    }
}

@Composable
private fun RoleDialog(user: AdminUser, onDismiss: () -> Unit, onPick: (String) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Change role") },
        text = {
            Column {
                ASSIGNABLE_ROLES.forEach { role ->
                    DropdownMenuItem(
                        text = { Text(roleLabel(role) + if (role == user.role) "  (current)" else "") },
                        onClick = { onPick(role) },
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun Badge(text: String) {
    AssistChip(
        onClick = {},
        enabled = false,
        label = { Text(text, style = MaterialTheme.typography.labelSmall) },
        colors = AssistChipDefaults.assistChipColors(
            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    )
}

private fun roleLabel(role: String): String = when (role) {
    "superadmin" -> "Super admin"
    "core_member" -> "Core member"
    "t2w_rider" -> "T2W rider"
    "rider" -> "Rider"
    else -> role
}

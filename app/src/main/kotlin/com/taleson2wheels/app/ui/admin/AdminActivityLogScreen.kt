package com.taleson2wheels.app.ui.admin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.taleson2wheels.app.data.remote.dto.ActivityLogEntry
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
fun AdminActivityLogScreen(
    factory: AppViewModelFactory,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AdminActivityLogViewModel = viewModel(factory = factory),
) {
    val state = viewModel.uiState

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Activity log") },
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
                state.items.isEmpty() -> ErrorView("No activity yet.", viewModel::refresh)
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
                        items(state.items, key = { it.id }) { entry -> ActivityRow(entry) }
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
}

@Composable
private fun ActivityRow(entry: ActivityLogEntry) {
    BrandCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = actionLabel(entry.action),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
            )
            val who = entry.performedByName.ifBlank { "Someone" }
            val what = entry.targetName.takeIf { it.isNotBlank() }
            Text(
                text = if (what != null) "$who → $what" else who,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            entry.details?.takeIf { it.isNotBlank() }?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            formatTimestamp(entry.timestamp)?.let {
                Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** "user.block" → "User block" — a readable label for the raw action key. */
private fun actionLabel(action: String): String =
    action.replace('_', ' ').replace('.', ' ').trim().replaceFirstChar { it.uppercase() }

private val activityFormatter = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm")

private fun formatTimestamp(iso: String?): String? {
    val ms = parseIsoToMillis(iso) ?: return null
    return Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).format(activityFormatter)
}

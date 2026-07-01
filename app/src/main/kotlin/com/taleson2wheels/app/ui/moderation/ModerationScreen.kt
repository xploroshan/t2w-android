package com.taleson2wheels.app.ui.moderation

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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.taleson2wheels.app.data.remote.dto.RegistrationModeration
import com.taleson2wheels.app.ui.AppViewModelFactory
import com.taleson2wheels.app.ui.common.ErrorView
import com.taleson2wheels.app.ui.common.LoadingView
import com.taleson2wheels.app.ui.components.BrandBackground
import com.taleson2wheels.app.ui.components.BrandCard
import com.taleson2wheels.app.ui.components.GradientButton
import com.taleson2wheels.app.ui.components.SecondaryButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModerationScreen(
    factory: AppViewModelFactory,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ModerationViewModel = viewModel(factory = factory),
) {
    val state = viewModel.uiState
    val snackbarHostState = remember { SnackbarHostState() }
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
                title = { Text("Registrations") },
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
                state.items.isEmpty() -> ErrorView("No pending registrations. All caught up!", viewModel::refresh)
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
                        items(state.items, key = { it.id }) { reg ->
                            RegistrationCard(
                                reg = reg,
                                onApprove = { viewModel.moderate(reg.id, approve = true) },
                                onReject = { viewModel.moderate(reg.id, approve = false) },
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
private fun RegistrationCard(
    reg: RegistrationModeration,
    onApprove: () -> Unit,
    onReject: () -> Unit,
) {
    BrandCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                reg.riderName.ifBlank { "Rider" },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
            )
            val contact = listOfNotNull(reg.email?.ifBlank { null }, reg.phone?.ifBlank { null }).joinToString(" · ")
            if (contact.isNotBlank()) {
                Text(contact, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            val meta = listOfNotNull(
                reg.accommodationType?.ifBlank { null }?.let { "Stay: $it" },
                reg.registeredAt?.take(10),
            ).joinToString(" · ")
            if (meta.isNotBlank()) {
                Text(meta, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                GradientButton(text = "Approve", onClick = onApprove, modifier = Modifier.weight(1f))
                SecondaryButton(text = "Reject", onClick = onReject, modifier = Modifier.weight(1f))
            }
        }
    }
}

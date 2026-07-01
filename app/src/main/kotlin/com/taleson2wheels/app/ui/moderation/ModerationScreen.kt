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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.taleson2wheels.app.data.remote.dto.BlogCard
import com.taleson2wheels.app.data.remote.dto.RegistrationModeration
import com.taleson2wheels.app.data.remote.dto.RidePost
import com.taleson2wheels.app.ui.AppViewModelFactory
import com.taleson2wheels.app.ui.common.ErrorView
import com.taleson2wheels.app.ui.common.LoadingView
import com.taleson2wheels.app.ui.common.OnBottomReached
import com.taleson2wheels.app.ui.components.BrandBackground
import com.taleson2wheels.app.ui.components.BrandCard
import com.taleson2wheels.app.ui.components.GradientButton
import com.taleson2wheels.app.ui.components.SecondaryButton

private val MODERATION_TABS = listOf("Registrations", "Blogs", "Ride Tales")

/**
 * The core-member moderation surface: a tabbed host over three queues —
 * registrations, blog posts, and ride tales. Each tab is an independent
 * cursor-paginated queue with optimistic approve/reject; per-tab role toggles
 * are enforced server-side (a tab whose toggle is off simply shows a 403 error).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModerationScreen(
    factory: AppViewModelFactory,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    registrationVm: ModerationViewModel = viewModel(factory = factory),
    blogVm: BlogModerationViewModel = viewModel(factory = factory),
    ridePostVm: RidePostModerationViewModel = viewModel(factory = factory),
) {
    var selectedTab by rememberSaveable { mutableStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Surface (and clear) the *active* tab's transient action error.
    val activeActionError = when (selectedTab) {
        1 -> blogVm.uiState.actionError
        2 -> ridePostVm.uiState.actionError
        else -> registrationVm.uiState.actionError
    }
    LaunchedEffect(activeActionError, selectedTab) {
        activeActionError?.let {
            snackbarHostState.showSnackbar(it)
            when (selectedTab) {
                1 -> blogVm.clearActionError()
                2 -> ridePostVm.clearActionError()
                else -> registrationVm.clearActionError()
            }
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Moderation") },
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
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.primary,
                ) {
                    MODERATION_TABS.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title) },
                            selectedContentColor = MaterialTheme.colorScheme.primary,
                            unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                when (selectedTab) {
                    0 -> ModerationQueueList(
                        state = registrationVm.uiState,
                        keyOf = { it.id },
                        emptyMessage = "No pending registrations. All caught up!",
                        onRefresh = registrationVm::refresh,
                        onLoadMore = registrationVm::loadMore,
                    ) { reg ->
                        RegistrationCard(
                            reg = reg,
                            onApprove = { registrationVm.moderate(reg.id, approve = true) },
                            onReject = { registrationVm.moderate(reg.id, approve = false) },
                        )
                    }

                    1 -> ModerationQueueList(
                        state = blogVm.uiState,
                        keyOf = { it.id },
                        emptyMessage = "No blogs awaiting review.",
                        onRefresh = blogVm::refresh,
                        onLoadMore = blogVm::loadMore,
                    ) { blog ->
                        BlogModerationCard(
                            blog = blog,
                            onApprove = { blogVm.moderate(blog.id, approve = true) },
                            onReject = { blogVm.moderate(blog.id, approve = false) },
                        )
                    }

                    else -> ModerationQueueList(
                        state = ridePostVm.uiState,
                        keyOf = { it.id },
                        emptyMessage = "No ride tales awaiting review.",
                        onRefresh = ridePostVm::refresh,
                        onLoadMore = ridePostVm::loadMore,
                    ) { post ->
                        RidePostModerationCard(
                            post = post,
                            onApprove = { ridePostVm.moderate(post.id, approve = true) },
                            onReject = { ridePostVm.moderate(post.id, approve = false) },
                        )
                    }
                }
            }
        }
    }
}

/**
 * Generic list body shared by all three queues: handles the loading / error /
 * empty / paginated-content states and renders each item via [card].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> ModerationQueueList(
    state: ModerationQueueState<T>,
    keyOf: (T) -> String,
    emptyMessage: String,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    card: @Composable (T) -> Unit,
) {
    when {
        state.isLoading && state.items.isEmpty() -> LoadingView()
        state.error != null && state.items.isEmpty() -> ErrorView(state.error, onRefresh)
        state.items.isEmpty() -> ErrorView(emptyMessage, onRefresh)
        else -> {
            val listState = rememberLazyListState()
            // Fetch the next page from the actual scroll position, not the footer's
            // composition, so a tall viewport doesn't auto-chain pages.
            listState.OnBottomReached { onLoadMore() }
            PullToRefreshBox(
            isRefreshing = state.isLoading,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize(),
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(state.items, key = keyOf) { item -> card(item) }
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
                                    .clickable { onLoadMore() }
                                    .padding(16.dp),
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

@Composable
private fun RegistrationCard(
    reg: RegistrationModeration,
    onApprove: () -> Unit,
    onReject: () -> Unit,
) {
    ModerationCardShell(onApprove = onApprove, onReject = onReject) {
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
    }
}

@Composable
private fun BlogModerationCard(
    blog: BlogCard,
    onApprove: () -> Unit,
    onReject: () -> Unit,
) {
    ModerationCardShell(onApprove = onApprove, onReject = onReject) {
        Text(
            blog.title.ifBlank { "Untitled" },
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
        )
        val byline = listOfNotNull(
            blog.authorName.ifBlank { null },
            if (blog.isVlog) "Vlog" else blog.type?.ifBlank { null }?.replaceFirstChar { it.uppercase() },
            blog.publishDate?.take(10),
        ).joinToString(" · ")
        if (byline.isNotBlank()) {
            Text(byline, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        val summary = blog.excerpt?.ifBlank { null } ?: blog.content?.ifBlank { null }
        if (summary != null) {
            Text(
                summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
            )
        }
    }
}

@Composable
private fun RidePostModerationCard(
    post: RidePost,
    onApprove: () -> Unit,
    onReject: () -> Unit,
) {
    ModerationCardShell(onApprove = onApprove, onReject = onReject) {
        Text(
            post.authorName.ifBlank { "Rider" },
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
        )
        val meta = listOfNotNull(
            if (post.images.isNotEmpty()) "${post.images.size} photo${if (post.images.size == 1) "" else "s"}" else null,
            post.createdAt?.take(10),
        ).joinToString(" · ")
        if (meta.isNotBlank()) {
            Text(meta, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (post.content.isNotBlank()) {
            Text(
                post.content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 4,
            )
        }
    }
}

/** Shared card chrome: the brand card + the approve/reject action row. */
@Composable
private fun ModerationCardShell(
    onApprove: () -> Unit,
    onReject: () -> Unit,
    content: @Composable () -> Unit,
) {
    BrandCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            content()
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

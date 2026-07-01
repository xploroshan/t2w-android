package com.taleson2wheels.app.ui.blogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.SubcomposeAsyncImage
import com.taleson2wheels.app.data.remote.dto.BlogCard
import com.taleson2wheels.app.ui.AppViewModelFactory
import com.taleson2wheels.app.ui.common.ErrorView
import com.taleson2wheels.app.ui.common.LoadingView
import com.taleson2wheels.app.ui.common.OnBottomReached
import com.taleson2wheels.app.ui.components.BrandBackground
import com.taleson2wheels.app.ui.components.BrandCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlogsScreen(
    factory: AppViewModelFactory,
    onBlogClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    onCompose: () -> Unit = {},
    viewModel: BlogsViewModel = viewModel(factory = factory),
) {
    val state = viewModel.uiState
    val snackbarHostState = remember { SnackbarHostState() }
    // Surface a failed like (or the "sign in to like" hint) as a transient snackbar.
    LaunchedEffect(state.likeError) {
        state.likeError?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearLikeError()
        }
    }
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Stories") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                ),
            )
        },
        floatingActionButton = {
            if (state.canCompose) {
                ExtendedFloatingActionButton(
                    onClick = onCompose,
                    icon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                    text = { Text("Write") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                )
            }
        },
    ) { innerPadding ->
        BrandBackground(Modifier.padding(innerPadding)) {
            Column(Modifier.fillMaxSize()) {
                BlogFilterRow(selected = state.filter, onSelect = viewModel::setFilter)
                when {
                    state.isLoading && state.blogs.isEmpty() -> LoadingView()
                    state.error != null && state.blogs.isEmpty() ->
                        ErrorView(state.error, viewModel::refresh)
                    state.blogs.isEmpty() ->
                        ErrorView(emptyMessage(state.filter), viewModel::refresh)
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
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            items(state.blogs, key = { it.id }) { blog ->
                                BlogCardItem(
                                    blog = blog,
                                    onClick = { onBlogClick(blog.id) },
                                    onToggleLike = { viewModel.toggleLike(blog.id) },
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
}

private fun emptyMessage(filter: BlogFilter): String = when (filter) {
    BlogFilter.ALL -> "No stories yet. Check back soon."
    BlogFilter.VLOGS -> "No vlogs yet. Check back soon."
    else -> "No ${filter.label.lowercase()} stories yet."
}

@Composable
private fun BlogFilterRow(selected: BlogFilter, onSelect: (BlogFilter) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BlogFilter.entries.forEach { filter ->
            FilterChip(
                selected = selected == filter,
                onClick = { onSelect(filter) },
                label = { Text(filter.label) },
            )
        }
    }
}

@Composable
internal fun BlogCardItem(blog: BlogCard, onClick: () -> Unit, onToggleLike: () -> Unit = {}) {
    // contentPadding=0 so the cover image runs edge-to-edge to the card's rounded
    // corners; the text block carries its own padding.
    BrandCard(modifier = Modifier.fillMaxWidth(), onClick = onClick, contentPadding = PaddingValues(0.dp)) {
        if (!blog.coverImage.isNullOrBlank()) {
            SubcomposeAsyncImage(
                model = blog.coverImage,
                contentDescription = blog.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
                loading = {},
                error = {},
            )
        }
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (blog.isVlog) {
                Text(
                    "▶ VLOG",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
            }
            Text(
                text = blog.title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (!blog.excerpt.isNullOrBlank()) {
                Text(
                    text = blog.excerpt,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = blog.authorName,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (blog.readTime > 0) {
                    Text(
                        text = "· ${blog.readTime} min read",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.weight(1f))
                LikeButton(liked = blog.likedByMe, count = blog.likes, onClick = onToggleLike)
            }
        }
    }
}

/** A heart toggle + count. Filled/accent when liked, outline/muted otherwise. */
@Composable
internal fun LikeButton(liked: Boolean, count: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val tint = if (liked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = modifier
            .clickable(onClick = onClick)
            .semantics { contentDescription = if (liked) "Unlike, $count likes" else "Like, $count likes" }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (liked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelMedium,
            color = tint,
        )
    }
}

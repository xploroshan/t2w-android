package com.taleson2wheels.app.ui.blogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.taleson2wheels.app.ui.common.ZoomableAsyncImage
import com.taleson2wheels.app.data.remote.dto.BlogCard
import com.taleson2wheels.app.ui.AppViewModelFactory
import com.taleson2wheels.app.ui.common.Avatar
import com.taleson2wheels.app.ui.common.ErrorView
import com.taleson2wheels.app.ui.common.LoadingView
import com.taleson2wheels.app.ui.components.BrandBackground
import com.taleson2wheels.app.ui.components.SecondaryButton
import com.taleson2wheels.app.ui.components.TagChip

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlogDetailScreen(
    blogId: String,
    factory: AppViewModelFactory,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BlogDetailViewModel = viewModel(factory = factory),
) {
    LaunchedEffect(blogId) { viewModel.load(blogId) }
    val state = viewModel.uiState

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(state.blog?.title ?: "Story", maxLines = 1) },
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
                state.isLoading -> LoadingView()
                state.error != null -> ErrorView(state.error, { viewModel.load(blogId) })
                state.blog != null -> BlogDetailBody(state.blog)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BlogDetailBody(blog: BlogCard, modifier: Modifier = Modifier) {
    val uriHandler = LocalUriHandler.current
    val embedUrl = blog.videoUrl?.takeIf { blog.isVlog && it.isNotBlank() }?.let { youTubeEmbedUrl(it) }
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        // A playable YouTube vlog leads with the embed; otherwise the cover image.
        when {
            embedUrl != null -> VlogPlayer(embedUrl)
            !blog.coverImage.isNullOrBlank() -> ZoomableAsyncImage(
                url = blog.coverImage,
                contentDescription = blog.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
            )
        }
        Column(
            Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
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
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold,
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Avatar(url = blog.authorAvatar, name = blog.authorName, size = 36.dp)
                Column {
                    Text(blog.authorName, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.SemiBold)
                    val meta = buildString {
                        blog.publishDate?.take(10)?.let { append(it) }
                        if (blog.readTime > 0) {
                            if (isNotEmpty()) append("  ·  ")
                            append("${blog.readTime} min read")
                        }
                    }
                    if (meta.isNotBlank()) {
                        Text(meta, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            // A vlog whose link isn't an embeddable YouTube URL still gets a way out.
            if (blog.isVlog && embedUrl == null && !blog.videoUrl.isNullOrBlank()) {
                SecondaryButton(
                    text = "Watch video",
                    onClick = { uriHandler.openUri(blog.videoUrl) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (blog.tags.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    blog.tags.forEach { tag -> TagChip(tag) }
                }
            }
            val body = blog.content?.ifBlank { null } ?: blog.excerpt
            if (!body.isNullOrBlank()) {
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
        }
    }
}

package com.taleson2wheels.app.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage

/**
 * Circular avatar: loads [url] with Coil, falling back to the person's initial
 * on a brand-tinted circle while loading, on error, or when [url] is null.
 */
@Composable
fun Avatar(
    url: String?,
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
) {
    val shape = CircleShape
    val initial = name.trim().firstOrNull()?.uppercase() ?: "?"

    val fallback: @Composable () -> Unit = {
        Box(
            modifier = Modifier
                .size(size)
                .clip(shape)
                .background(MaterialTheme.colorScheme.primaryContainer)
                // Announce the person's name, not the bare initial glyph.
                .semantics(mergeDescendants = true) { contentDescription = name },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = initial,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }

    if (url.isNullOrBlank()) {
        fallback()
    } else {
        SubcomposeAsyncImage(
            model = url,
            contentDescription = name,
            contentScale = ContentScale.Crop,
            modifier = modifier.size(size).clip(shape),
            loading = { fallback() },
            error = { fallback() },
        )
    }
}

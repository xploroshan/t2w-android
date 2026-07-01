package com.taleson2wheels.app.ui.common

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

/**
 * Fires [onLoadMore] once when the list is scrolled within [buffer] items of the
 * end, driven off the ACTUAL scroll position rather than a footer's composition.
 *
 * The previous `LaunchedEffect(nextCursor) { loadMore() }`-in-the-footer pattern
 * re-fired on every cursor change while the footer stayed composed, so a tall
 * viewport or short rows could auto-chain several pages with no user scroll. Here
 * the trigger only re-arms after the bottom edge moves away (a new page pushes it
 * down) and the user scrolls back near it, so pages load lazily as intended.
 * [onLoadMore] should still no-op when there's nothing to fetch (the ViewModels
 * guard on cursor/in-flight), so this can be called unconditionally.
 */
@Composable
fun LazyListState.OnBottomReached(buffer: Int = 3, onLoadMore: () -> Unit) {
    val shouldLoadMore = remember(this) {
        derivedStateOf {
            val info = layoutInfo
            val total = info.totalItemsCount
            if (total == 0) return@derivedStateOf false
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: return@derivedStateOf false
            lastVisible >= total - 1 - buffer
        }
    }
    LaunchedEffect(this) {
        snapshotFlow { shouldLoadMore.value }
            .distinctUntilChanged()
            .filter { it }
            .collect { onLoadMore() }
    }
}

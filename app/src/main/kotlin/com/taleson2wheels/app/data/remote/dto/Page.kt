package com.taleson2wheels.app.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * Generic cursor-paginated envelope used by every list endpoint:
 *
 *   { "items": [ ... ], "nextCursor": "…" | null }
 *
 * `nextCursor == null` means the last page has been reached.
 */
@Serializable
data class Page<T>(
    val items: List<T> = emptyList(),
    val nextCursor: String? = null,
)

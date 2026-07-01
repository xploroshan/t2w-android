package com.taleson2wheels.app.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * A blog post (`GET /blogs` → `Page<BlogCard>`, `GET /blogs/{id}` → `{ blog }`).
 * Both the list and detail endpoints return this same shape; `content` is the
 * full body (present on detail, may be blank on cards).
 */
@Serializable
data class BlogCard(
    val id: String,
    val title: String,
    val excerpt: String? = null,
    val content: String? = null,
    val authorId: String? = null,
    val authorName: String = "",
    val authorAvatar: String? = null,
    val publishDate: String? = null,
    val coverImage: String? = null,
    val tags: List<String> = emptyList(),
    val type: String? = null,
    val isVlog: Boolean = false,
    val videoUrl: String? = null,
    val readTime: Int = 0,
    val likes: Int = 0,
    /** Whether the signed-in caller has liked this post; always false when anonymous. */
    val likedByMe: Boolean = false,
    val approvalStatus: String? = null,
)

/** `/blogs/{id}` wraps the post in `{ "blog": ... }`. */
@Serializable
data class BlogResponse(val blog: BlogCard)

/**
 * Response of `POST`/`DELETE /api/v1/blogs/{id}/like` — the fresh, server-authoritative
 * like count and this caller's like flag. Mirrors the `LikeState` schema in the OpenAPI spec.
 */
@Serializable
data class LikeState(val likes: Int, val likedByMe: Boolean)

/**
 * Request body for `POST /api/v1/blogs` (privileged roles only). Mirrors the
 * `BlogInput` schema in `docs/openapi-v1.yaml`; only `title` is required and the
 * server fills `authorId`/`authorName`/`approvalStatus` from the bearer token.
 */
@Serializable
data class BlogInput(
    val title: String,
    val excerpt: String? = null,
    val content: String? = null,
    val coverImage: String? = null,
    val tags: List<String> = emptyList(),
    val type: String = "personal",
    val isVlog: Boolean = false,
    val videoUrl: String? = null,
)

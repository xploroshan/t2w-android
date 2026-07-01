package com.taleson2wheels.app.data.remote.dto

import kotlinx.serialization.Serializable

/** Body for `POST /api/v1/contact` (public, rate-limited). */
@Serializable
data class ContactRequest(
    val name: String,
    val email: String,
    val subject: String,
    val message: String,
)

@Serializable
data class ContactResponse(val success: Boolean = false)

package com.taleson2wheels.app.data.repository

import com.taleson2wheels.app.data.remote.ApiResult
import com.taleson2wheels.app.data.remote.dto.UserDto
import com.taleson2wheels.app.data.session.Tokens
import kotlinx.coroutines.flow.StateFlow

/**
 * The slice of the auth session the content screens depend on: the reactive token
 * state (to gate like / compose affordances on being signed in) and a lookup of the
 * current user (for role-gated actions). [AuthRepository] is the production
 * implementation; unit tests provide a lightweight fake without an Android context.
 */
interface AuthSession {
    /** Reactive session — null when signed out. */
    val tokens: StateFlow<Tokens?>

    /** The current signed-in user, or a failure when anonymous / offline. */
    suspend fun currentUser(): ApiResult<UserDto>
}

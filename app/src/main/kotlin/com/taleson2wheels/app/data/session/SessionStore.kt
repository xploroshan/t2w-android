package com.taleson2wheels.app.data.session

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first

/** The pair of tokens that defines a signed-in session. */
data class Tokens(val accessToken: String, val refreshToken: String)

private val Context.sessionDataStore: DataStore<Preferences> by preferencesDataStore(name = "t2w_session")

/**
 * Persists the bearer/refresh token pair and exposes it both reactively (for
 * the UI auth gate) and synchronously (for the OkHttp interceptor/authenticator,
 * which run off the main thread and cannot suspend).
 *
 * The in-memory [StateFlow] is the source of truth at runtime; [hydrate] loads
 * it from disk once at startup. Refresh tokens here are stored in the app's
 * private DataStore and excluded from backups (see res/xml/backup_rules.xml).
 * Hardening to the Android Keystore is a follow-up — see README.
 */
class SessionStore(private val context: Context) {

    private val accessKey = stringPreferencesKey("access_token")
    private val refreshKey = stringPreferencesKey("refresh_token")

    private val _tokens = MutableStateFlow<Tokens?>(null)
    val tokens: StateFlow<Tokens?> = _tokens.asStateFlow()

    private val _ready = MutableStateFlow(false)
    /** False until [hydrate] has read persisted tokens; the UI shows a splash. */
    val ready: StateFlow<Boolean> = _ready.asStateFlow()

    val isLoggedIn: Boolean get() = _tokens.value != null

    /** Load persisted tokens into memory. Call once on app start. */
    suspend fun hydrate() {
        val prefs = context.sessionDataStore.data.first()
        val access = prefs[accessKey]
        val refresh = prefs[refreshKey]
        _tokens.value = if (access != null && refresh != null) Tokens(access, refresh) else null
        _ready.value = true
    }

    suspend fun save(tokens: Tokens) {
        context.sessionDataStore.edit { prefs ->
            prefs[accessKey] = tokens.accessToken
            prefs[refreshKey] = tokens.refreshToken
        }
        _tokens.value = tokens
    }

    suspend fun clear() {
        context.sessionDataStore.edit { it.clear() }
        _tokens.value = null
    }

    /** Synchronous read for the OkHttp auth interceptor. */
    fun peekAccessToken(): String? = _tokens.value?.accessToken

    /** Synchronous read for the token authenticator. */
    fun peekRefreshToken(): String? = _tokens.value?.refreshToken
}

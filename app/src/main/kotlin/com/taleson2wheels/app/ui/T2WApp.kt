package com.taleson2wheels.app.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.taleson2wheels.app.di.AppContainer
import com.taleson2wheels.app.ui.auth.LoginScreen
import com.taleson2wheels.app.ui.common.LoadingView
import com.taleson2wheels.app.ui.theme.T2WTheme

/**
 * Root composable + auth gate. The session lives in [AppContainer.session]; this
 * observes it to switch between the login screen and the signed-in app shell.
 * Clearing the session anywhere (logout, refresh failure) automatically returns
 * the user to login — no manual navigation required.
 */
@Composable
fun T2WApp(container: AppContainer) {
    T2WTheme {
        val factory = remember(container) { AppViewModelFactory(container) }
        val ready by container.session.ready.collectAsStateWithLifecycle()
        val tokens by container.session.tokens.collectAsStateWithLifecycle()

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            when {
                !ready -> LoadingView()
                tokens == null -> LoginScreen(factory = factory)
                else -> MainScreen(factory = factory)
            }
        }
    }
}

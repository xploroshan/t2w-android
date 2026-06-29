package com.taleson2wheels.app

import android.app.Application
import com.taleson2wheels.app.di.AppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Process-wide entry point. Owns the [AppContainer] and hydrates the session. */
class T2WApplication : Application() {

    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        // Load persisted tokens into memory before the UI evaluates the auth gate.
        appScope.launch { container.session.hydrate() }
    }
}

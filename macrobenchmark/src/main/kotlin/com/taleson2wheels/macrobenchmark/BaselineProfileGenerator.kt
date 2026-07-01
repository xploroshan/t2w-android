package com.taleson2wheels.macrobenchmark

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Generates a Baseline Profile — the list of hot startup/first-frame code paths
 * that ART pre-compiles (AOT) so the app's cold start and first screen render
 * faster. The :app module consumes the generated profile and bundles it
 * (installed at first run by androidx.profileinstaller).
 *
 * Runs on a device / Gradle Managed Device via the perf CI, not in a JVM unit run:
 *
 *   ./gradlew :app:generateBaselineProfile
 *
 * The critical journey exercised here is cold launch to the first screen (the
 * signed-out login screen, reachable with no backend). Extend the block with
 * post-login scrolls once a seeded backend is wired into the perf runner.
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {

    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun startup() = rule.collect(packageName = TARGET_PACKAGE) {
        pressHome()
        startActivityAndWait()
    }

    private companion object {
        const val TARGET_PACKAGE = "com.taleson2wheels.app"
    }
}

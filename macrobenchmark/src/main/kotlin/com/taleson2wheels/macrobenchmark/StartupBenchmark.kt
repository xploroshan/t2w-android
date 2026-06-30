package com.taleson2wheels.macrobenchmark

import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Measures app startup time (the most important perf signal) for the release-like
 * `benchmark` build of the app. Runs on a device/emulator via the perf CI:
 *
 *   ./gradlew :macrobenchmark:connectedBenchmarkAndroidTest
 *
 * Cold = process freshly created; Warm = process alive, activity recreated.
 */
@RunWith(AndroidJUnit4::class)
class StartupBenchmark {

    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun startupCold() = measure(StartupMode.COLD)

    @Test
    fun startupWarm() = measure(StartupMode.WARM)

    private fun measure(mode: StartupMode) = benchmarkRule.measureRepeated(
        packageName = TARGET_PACKAGE,
        metrics = listOf(StartupTimingMetric()),
        iterations = 5,
        startupMode = mode,
    ) {
        pressHome()
        startActivityAndWait()
    }

    private companion object {
        const val TARGET_PACKAGE = "com.taleson2wheels.app"
    }
}

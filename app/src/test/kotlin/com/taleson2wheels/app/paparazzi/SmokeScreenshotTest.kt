package com.taleson2wheels.app.paparazzi

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.taleson2wheels.app.ui.auth.AuthPrimaryButton
import com.taleson2wheels.app.ui.theme.T2WTheme
import org.junit.Rule
import org.junit.Test

/**
 * Smoke test: proves Paparazzi can render the app's Material3 theme + a real
 * component to a PNG on the JVM (no emulator/device). Once this is green the
 * per-screen snapshots in [ScreenshotTest] follow the same pattern.
 */
class SmokeScreenshotTest {

    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5)

    @Test
    fun theme_and_primary_button() {
        paparazzi.snapshot {
            T2WTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Text("Tales on 2 Wheels", style = MaterialTheme.typography.headlineMedium)
                        Text("Paparazzi renders the real theme", style = MaterialTheme.typography.bodyLarge)
                        AuthPrimaryButton(
                            text = "Sign in",
                            enabled = true,
                            loading = false,
                            onClick = {},
                        )
                    }
                }
            }
        }
    }
}

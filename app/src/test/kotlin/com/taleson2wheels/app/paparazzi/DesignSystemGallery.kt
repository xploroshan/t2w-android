package com.taleson2wheels.app.paparazzi

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.taleson2wheels.app.ui.common.EmptyState
import com.taleson2wheels.app.ui.components.BadgeTierChip
import com.taleson2wheels.app.ui.components.BrandCard
import com.taleson2wheels.app.ui.components.BrandWordmark
import com.taleson2wheels.app.ui.components.GlassCard
import com.taleson2wheels.app.ui.components.GradientButton
import com.taleson2wheels.app.ui.components.GradientText
import com.taleson2wheels.app.ui.components.InputField
import com.taleson2wheels.app.ui.components.SecondaryButton
import com.taleson2wheels.app.ui.components.SkeletonCard
import com.taleson2wheels.app.ui.components.TagChip
import com.taleson2wheels.app.ui.theme.T2WTheme
import org.junit.Rule
import org.junit.Test

/**
 * Visual gallery for the design-system components — renders the brand kit on the
 * dark theme so a reviewer can eyeball every primitive in one place and so
 * regressions to the look are caught by `verifyPaparazziDebug`.
 */
class DesignSystemGallery {

    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5)

    private fun gallery(content: @Composable () -> Unit) = paparazzi.snapshot {
        T2WTheme {
            Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) { content() }
            }
        }
    }

    @Test
    fun controls() = gallery {
        BrandWordmark()
        GradientText("Ride. Capture. Share.", style = MaterialTheme.typography.headlineSmall)
        GradientButton(text = "Register for ride", onClick = {}, modifier = Modifier.fillMaxWidth())
        GradientButton(text = "Submitting…", onClick = {}, loading = true, modifier = Modifier.fillMaxWidth())
        SecondaryButton(text = "Share", onClick = {}, modifier = Modifier.fillMaxWidth())
        InputField(value = "rider@taleson2wheels.com", onValueChange = {}, label = "Email")
        InputField(value = "wrong", onValueChange = {}, label = "Password", isPassword = true, error = "Invalid email or password")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BadgeTierChip(tier = "gold")
            BadgeTierChip(tier = "diamond")
            TagChip("Monsoon")
        }
    }

    @Test
    fun surfaces() = gallery {
        BrandCard(modifier = Modifier.fillMaxWidth()) {
            Text("Coorg Monsoon Run", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
            Text("320 km · Moderate · 18 riders", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Text("LIVE · 12 riders on route", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        }
        SkeletonCard(Modifier.fillMaxWidth())
        Box(Modifier.fillMaxWidth().height(200.dp)) {
            EmptyState(title = "No rides yet", message = "Upcoming rides will appear here.")
        }
    }
}

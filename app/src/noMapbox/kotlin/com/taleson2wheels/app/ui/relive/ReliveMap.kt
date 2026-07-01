package com.taleson2wheels.app.ui.relive

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.taleson2wheels.app.data.remote.dto.LivePathPoint
import kotlin.math.roundToInt

/**
 * Placeholder Relive map for builds compiled WITHOUT the Mapbox SDK
 * (`-Pt2wMapbox=false`, the default — see app/build.gradle.kts). Keeps the exact
 * signature of the real [ReliveMap] in `src/mapbox`, so the rest of the Relive
 * feature (playback engine, HUD, transport controls) compiles and runs unchanged;
 * only the terrain rendering is absent. It still surfaces the current sample so the
 * screen isn't dead — the animated 3D flyover simply needs a Mapbox build.
 */
@Composable
fun ReliveMap(
    token: String,
    recorded: List<ReliveTrackPoint>,
    planned: List<LivePathPoint>,
    sample: ReliveSample?,
    modifier: Modifier = Modifier,
) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Column(
            Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                "3D flyover unavailable in this build",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "Compiled without the Mapbox Maps SDK (-Pt2wMapbox=false). The playback " +
                    "controls below still work; build with -Pt2wMapbox=true for the terrain map.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            sample?.let { s ->
                Text(
                    "Position ${"%.4f".format(s.lat)}, ${"%.4f".format(s.lng)} · " +
                        (s.speedKmh?.let { "${it.roundToInt()} km/h" } ?: "—"),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

package com.taleson2wheels.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// The T2W brand is dark-only (matching the website), so there is a single
// scheme. `onSurfaceVariant` is lifted to a lighter gray than the raw web
// `muted` token so secondary text/icons keep adequate contrast on the near-black
// surfaces; the literal #6b7280 muted is still available as `T2WMuted` for spots
// the web uses it deliberately.
private val TextMutedOnDark = Color(0xFF9AA0AA)

private val T2WColors = darkColorScheme(
    primary = T2WAccent,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF3A1620), // deep accent-tinted surface
    onPrimaryContainer = T2WAccentHover,
    secondary = T2WGold,
    onSecondary = Color(0xFF1A1206),
    secondaryContainer = Color(0xFF332608),
    onSecondaryContainer = T2WGold,
    tertiary = T2WDiamond,
    onTertiary = Color(0xFF06222A),
    background = T2WBackground,
    onBackground = T2WForeground,
    surface = T2WSurface,
    onSurface = T2WForeground,
    surfaceVariant = T2WSurfaceLight,
    onSurfaceVariant = TextMutedOnDark,
    surfaceContainerLowest = T2WBackground,
    surfaceContainerLow = T2WSurface,
    surfaceContainer = T2WSurface,
    surfaceContainerHigh = T2WSurfaceLight,
    surfaceContainerHighest = Color(0xFF202020),
    outline = T2WBorder,
    outlineVariant = Color(0xFF1F1F1F),
    error = T2WError,
    onError = Color.White,
    inverseSurface = T2WForeground,
    inverseOnSurface = T2WBackground,
)

@Composable
fun T2WTheme(
    // Kept for source-compat with existing call sites; the brand is dark-only so
    // the value is intentionally ignored.
    darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = T2WColors,
        typography = T2WTypography,
        shapes = T2WShapes,
        content = content,
    )
}

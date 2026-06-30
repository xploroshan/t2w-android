package com.taleson2wheels.app.ui.theme

import androidx.compose.foundation.background
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// ─────────────────────────────────────────────────────────────────────────────
// Brand brushes & helpers — the gradients/glows that give the website its look,
// expressed as reusable Compose Brushes so every screen draws them consistently.
// ─────────────────────────────────────────────────────────────────────────────

/** Accent→red CTA gradient, matching the site's `.btn-primary`. */
val AccentButtonGradient = Brush.horizontalGradient(listOf(T2WAccent, Color(0xFFD93A4F)))

/** Hero/section backdrop: `linear-gradient(135deg, #0f0f0f, #1a1a2e, #0f0f0f)`. */
val HeroGradient = Brush.linearGradient(listOf(T2WPrimaryDark, T2WNavy, T2WPrimaryDark))

/** `.gradient-text` — accent → gold → accent, for the wordmark/headlines. */
val GradientTextBrush = Brush.linearGradient(listOf(T2WAccent, T2WGold, T2WAccent))

/** Subtle accent glow used behind hero art / focal cards (`.glow-accent`). */
val AccentGlow = Brush.radialGradient(
    colors = listOf(T2WAccent.copy(alpha = 0.12f), Color.Transparent),
)

/** Page background brush: near-black with a faint navy lift toward the top. */
val AppBackgroundGradient = Brush.verticalGradient(
    colors = listOf(T2WPrimaryDark, T2WBackground),
)

/** Paints the standard app background (used by [com.taleson2wheels.app.ui.components.BrandBackground]). */
fun Modifier.appBackground(): Modifier = this.background(AppBackgroundGradient)

/**
 * Badge-tier accent color, keyed off the tier name the API returns
 * (iron/bronze/silver/gold/platinum/diamond). Falls back to the accent.
 */
fun badgeTierColor(tier: String?): Color = when (tier?.trim()?.lowercase()) {
    "iron" -> T2WIron
    "bronze" -> T2WBronze
    "silver" -> T2WSilver
    "gold" -> T2WGold
    "platinum" -> T2WPlatinum
    "diamond", "conqueror" -> T2WDiamond
    else -> T2WAccent
}

package com.taleson2wheels.app.ui.theme

import androidx.compose.ui.graphics.Color

// ─────────────────────────────────────────────────────────────────────────────
// T2W brand palette — ported 1:1 from the website's Tailwind theme + globals.css
// so the app reads as the same brand. The site is dark-only; the app follows.
//
//   web token            value      role
//   t2w.dark / bg        #0a0a0a    app background (darkest)
//   t2w.primary          #0f0f0f    hero gradient stop
//   t2w.secondary        #1a1a2e    hero gradient mid (navy)
//   t2w.surface          #111111    card / surface base
//   t2w.surface-light    #1a1a1a    lifted surface / inputs
//   t2w.border           #2a2a2a    dividers / outlines
//   foreground           #fafafa    primary text
//   t2w.muted            #6b7280    secondary text
//   t2w.accent           #e94560    brand red — CTAs, highlights
//   accent-hover         #ff6b81    accent pressed/hover
//   t2w.gold             #f5a623    gold accent / premium
//   t2w.silver           #c0c0c0    badge tier
//   t2w.platinum         #e5e4e2    badge tier
//   t2w.diamond          #b9f2ff    badge tier / secondary accent
// ─────────────────────────────────────────────────────────────────────────────

val T2WBackground = Color(0xFF0A0A0A)
val T2WPrimaryDark = Color(0xFF0F0F0F)
val T2WNavy = Color(0xFF1A1A2E)
val T2WSurface = Color(0xFF111111)
val T2WSurfaceLight = Color(0xFF1A1A1A)
val T2WBorder = Color(0xFF2A2A2A)
val T2WForeground = Color(0xFFFAFAFA)
val T2WMuted = Color(0xFF6B7280)

val T2WAccent = Color(0xFFE94560)
val T2WAccentHover = Color(0xFFFF6B81)
val T2WGold = Color(0xFFF5A623)

// Metallic badge-tier accents.
val T2WIron = Color(0xFF8A8A8A)
val T2WBronze = Color(0xFFCD7F32)
val T2WSilver = Color(0xFFC0C0C0)
val T2WPlatinum = Color(0xFFE5E4E2)
val T2WDiamond = Color(0xFFB9F2FF)

// Status colors tuned for the dark surface.
val T2WSuccess = Color(0xFF22C55E)
val T2WWarning = Color(0xFFF5A623)
val T2WError = Color(0xFFEF4444)

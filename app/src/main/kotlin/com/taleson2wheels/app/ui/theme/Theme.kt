package com.taleson2wheels.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = T2WOrange,
    onPrimary = Color.White,
    primaryContainer = T2WOrangeLight,
    onPrimaryContainer = T2WInk,
    secondary = T2WOrangeDark,
    onSecondary = Color.White,
    background = T2WSand,
    onBackground = T2WInk,
    surface = Color.White,
    onSurface = T2WInk,
)

private val DarkColors = darkColorScheme(
    primary = T2WOrange,
    onPrimary = T2WInk,
    primaryContainer = T2WOrangeDark,
    onPrimaryContainer = Color.White,
    secondary = T2WOrangeLight,
    onSecondary = T2WInk,
    background = T2WInk,
    onBackground = Color.White,
    surface = T2WSurfaceDark,
    onSurface = Color.White,
)

@Composable
fun T2WTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = T2WTypography,
        content = content,
    )
}

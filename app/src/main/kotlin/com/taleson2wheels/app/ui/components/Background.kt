package com.taleson2wheels.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.taleson2wheels.app.ui.theme.appBackground

/**
 * Fills the available space with the app's branded background gradient and hosts
 * [content]. Use as the root of a screen body so every surface sits on the same
 * near-black canvas the website uses.
 */
@Composable
fun BrandBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(modifier = modifier.fillMaxSize().appBackground(), content = content)
}

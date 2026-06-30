package com.taleson2wheels.app.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/** Clickable with no Material ripple — for controls that animate their own press. */
fun Modifier.clickableNoRipple(
    interactionSource: MutableInteractionSource,
    enabled: Boolean = true,
    onClick: () -> Unit,
): Modifier = this.clickable(
    interactionSource = interactionSource,
    indication = null,
    enabled = enabled,
    onClick = onClick,
)

/**
 * Animated shimmer sweep for skeleton placeholders — a diagonal highlight band
 * that loops across the element. Apply to a surface-colored Box sized like the
 * content it stands in for.
 */
fun Modifier.shimmer(): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val x by transition.animateFloat(
        initialValue = -2f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Restart),
        label = "shimmer-x",
    )
    val highlight = Color.White.copy(alpha = 0.06f)
    val base = Color.White.copy(alpha = 0.02f)
    drawShimmer(x, base, highlight)
}

private fun Modifier.drawShimmer(progress: Float, base: Color, highlight: Color): Modifier =
    drawWithContent {
        drawContent()
        val w = size.width
        val start = Offset(w * progress, 0f)
        val end = Offset(w * (progress + 1f), size.height)
        val brush = Brush.linearGradient(
            colors = listOf(base, highlight, base),
            start = start,
            end = end,
        )
        drawRect(brush)
    }

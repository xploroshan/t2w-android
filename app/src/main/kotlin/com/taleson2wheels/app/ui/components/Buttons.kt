package com.taleson2wheels.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.taleson2wheels.app.ui.theme.AccentButtonGradient
import com.taleson2wheels.app.ui.theme.T2WBorder

private val ButtonShape = RoundedCornerShape(12.dp)
private val ButtonHeight = 52.dp

/**
 * Primary CTA — the website's `.btn-primary`: accent→red gradient pill, bold
 * white label, presses down slightly. Shows a spinner and blocks taps while
 * [loading]; dims when disabled.
 */
@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    leadingIcon: ImageVector? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f, label = "btn-scale")
    val active = enabled && !loading

    Box(
        modifier = modifier
            .scale(scale)
            .clip(ButtonShape)
            .background(AccentButtonGradient)
            .alpha(if (enabled) 1f else 0.45f)
            .heightIn(min = ButtonHeight)
            .clickableNoRipple(interaction, enabled = active, onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(LocalContentColor provides Color.White) {
            if (loading) {
                CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
            } else {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (leadingIcon != null) Icon(leadingIcon, contentDescription = null)
                    Text(text, style = MaterialTheme.typography.labelLarge, color = Color.White)
                }
            }
        }
    }
}

/** Secondary action — the site's `.btn-secondary`: outlined surface pill. */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f, label = "btn2-scale")

    Box(
        modifier = modifier
            .scale(scale)
            .clip(ButtonShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(BorderStroke(1.dp, T2WBorder), ButtonShape)
            .alpha(if (enabled) 1f else 0.45f)
            .heightIn(min = ButtonHeight)
            .clickableNoRipple(interaction, enabled = enabled, onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (leadingIcon != null) Icon(leadingIcon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
            Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

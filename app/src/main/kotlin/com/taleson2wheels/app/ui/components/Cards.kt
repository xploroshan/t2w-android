package com.taleson2wheels.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.taleson2wheels.app.ui.theme.T2WAccent
import com.taleson2wheels.app.ui.theme.T2WBorder

private val CardShape = RoundedCornerShape(16.dp)

/**
 * The website's `.card` — rounded-2xl surface with a hairline border and a soft
 * accent-tinted shadow. When [onClick] is supplied it becomes `.card-interactive`:
 * it presses down on tap.
 */
@Composable
fun BrandCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    contentPadding: androidx.compose.foundation.layout.PaddingValues = androidx.compose.foundation.layout.PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (onClick != null && pressed) 0.985f else 1f, label = "card-scale")

    val base = Modifier
        .scale(scale)
        .shadow(elevation = 10.dp, shape = CardShape, ambientColor = T2WAccent.copy(alpha = 0.10f), spotColor = T2WAccent.copy(alpha = 0.10f))
        .clip(CardShape)
        .background(androidx.compose.material3.MaterialTheme.colorScheme.surface)
        .border(BorderStroke(1.dp, T2WBorder), CardShape)

    val clickable = if (onClick != null) base.clickableNoRipple(interaction, onClick = onClick) else base

    Column(modifier = clickable.then(modifier).padding(contentPadding), content = content)
}

/** Translucent `.glass` panel — for overlays and floating bars. */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    contentPadding: androidx.compose.foundation.layout.PaddingValues = androidx.compose.foundation.layout.PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .clip(CardShape)
            .background(Color(0xFF111111).copy(alpha = 0.85f))
            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.06f)), CardShape)
            .padding(contentPadding),
        content = content,
    )
}

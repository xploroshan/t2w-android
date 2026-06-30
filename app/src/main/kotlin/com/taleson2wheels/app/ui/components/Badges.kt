package com.taleson2wheels.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.taleson2wheels.app.ui.theme.badgeTierColor

private val ChipShape = RoundedCornerShape(50)

/**
 * A pill chip tinted by badge tier (iron…diamond) — a translucent fill of the
 * tier color with a matching outline and label. Used on rider cards, the
 * leaderboard, and profiles.
 */
@Composable
fun BadgeTierChip(
    tier: String,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
) {
    val color = badgeTierColor(tier)
    Row(
        modifier = modifier
            .clip(ChipShape)
            .background(color.copy(alpha = 0.14f))
            .border(BorderStroke(1.dp, color.copy(alpha = 0.5f)), ChipShape)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (leading != null) leading()
        Text(
            text = tier.replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = color,
        )
    }
}

/** Neutral pill chip (e.g. blog tags, ride status) in the surface-light style. */
@Composable
fun TagChip(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier
            .clip(ChipShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

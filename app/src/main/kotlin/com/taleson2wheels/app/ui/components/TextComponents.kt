package com.taleson2wheels.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.taleson2wheels.app.ui.theme.AccentButtonGradient
import com.taleson2wheels.app.ui.theme.Courgette
import com.taleson2wheels.app.ui.theme.GradientTextBrush
import com.taleson2wheels.app.ui.theme.T2WAccent

/**
 * `.gradient-text` — text painted with the accent→gold→accent brush. Used for the
 * wordmark and hero headlines. Falls back gracefully (the brush is applied via
 * the style's `brush`, which Compose renders on the glyphs).
 */
@Composable
fun GradientText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.headlineMedium,
    maxLines: Int = Int.MAX_VALUE,
) {
    Text(
        text = text,
        modifier = modifier,
        style = style.copy(brush = GradientTextBrush),
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
    )
}

/** The "Tales on 2 Wheels" wordmark in the brand (Courgette) face. */
@Composable
fun BrandWordmark(
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.headlineMedium,
) {
    Text(
        text = "Tales on 2 Wheels",
        modifier = modifier,
        style = style.copy(fontFamily = Courgette, brush = GradientTextBrush),
    )
}

/** Section heading with a short accent rule, matching the site's section titles. */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            Modifier
                .size(width = 4.dp, height = 18.dp)
                .background(AccentButtonGradient, CircleShape),
        )
        Text(
            title,
            // Mark as a heading so TalkBack users can jump section-to-section.
            modifier = Modifier.weight(1f, fill = false).semantics { heading() },
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onBackground,
        )
        if (trailing != null) trailing()
    }
}

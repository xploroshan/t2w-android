package com.taleson2wheels.app.ui.content

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.taleson2wheels.app.ui.auth.AuthScaffold
import com.taleson2wheels.app.ui.components.BrandCard
import com.taleson2wheels.app.ui.components.BrandWordmark
import com.taleson2wheels.app.ui.components.GradientButton
import com.taleson2wheels.app.ui.components.SecondaryButton
import com.taleson2wheels.app.ui.components.SectionHeader

private data class Social(val label: String, val url: String)

private val socials = listOf(
    Social("Instagram", "https://www.instagram.com/Tales.On.2.Wheels"),
    Social("YouTube", "https://www.youtube.com/@TalesOn2Wheels"),
    Social("Facebook", "https://www.facebook.com/TalesOn2Wheels"),
    Social("X (Twitter)", "https://x.com/TalesOn2Wheels"),
)

@Composable
fun AboutScreen(
    onBack: () -> Unit,
    onOpenCrew: () -> Unit,
    onOpenGuidelines: () -> Unit,
    onOpenContact: () -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    AuthScaffold(title = "About", onBack = onBack) { m ->
        Column(
            modifier = m.padding(top = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            BrandWordmark(style = MaterialTheme.typography.headlineMedium)
            Text(
                "A community of riders chasing tarmac, trails, and the stories in between.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }

        SectionHeader("Follow the ride", modifier = m.padding(top = 8.dp))
        BrandCard(modifier = m, contentPadding = androidx.compose.foundation.layout.PaddingValues(4.dp)) {
            socials.forEach { social ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { uriHandler.openUri(social.url) }
                        .padding(horizontal = 12.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        social.label,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Icon(
                        Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = "Open ${social.label}",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }

        SectionHeader("More", modifier = m.padding(top = 8.dp))
        SecondaryButton(text = "Meet the crew", onClick = onOpenCrew, modifier = m)
        SecondaryButton(text = "Ride guidelines", onClick = onOpenGuidelines, modifier = m)
        GradientButton(text = "Contact us", onClick = onOpenContact, modifier = m)
    }
}

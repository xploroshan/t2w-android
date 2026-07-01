package com.taleson2wheels.app.ui.blogs

import android.annotation.SuppressLint
import android.graphics.Color
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Pull a YouTube video id from a watch / short / embed / youtu.be URL and build
 * the privacy-enhanced embed URL, or return null if [raw] isn't a YouTube link
 * (in which case the caller should fall back to opening the URL externally). Ids
 * are exactly 11 chars, so a non-YouTube `videoUrl` can never produce an embed —
 * this doubles as sanitisation for the WebView below.
 */
internal fun youTubeEmbedUrl(raw: String): String? {
    val id = Regex("""(?:youtu\.be/|[?&]v=|/embed/|/shorts/)([A-Za-z0-9_-]{11})""")
        .find(raw)?.groupValues?.get(1)
    return id?.let { "https://www.youtube-nocookie.com/embed/$it?rel=0" }
}

/**
 * Inline YouTube embed for a vlog, mirroring the website's iframe. Uses the
 * platform WebView (no extra dependency; ExoPlayer can't play YouTube) restricted
 * to a single trusted embed URL, JS-on for the player, file/content access off,
 * and no autoplay (the user taps play).
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun VlogPlayer(embedUrl: String, modifier: Modifier = Modifier) {
    val html = """
        <html><body style="margin:0;background:#000">
        <iframe width="100%" height="100%" src="$embedUrl" frameborder="0"
         allow="accelerometer; encrypted-media; gyroscope; picture-in-picture" allowfullscreen></iframe>
        </body></html>
    """.trimIndent()
    AndroidView(
        modifier = modifier.fillMaxWidth().aspectRatio(16f / 9f),
        factory = { ctx ->
            WebView(ctx).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.mediaPlaybackRequiresUserGesture = true
                settings.allowFileAccess = false
                settings.allowContentAccess = false
                settings.cacheMode = WebSettings.LOAD_DEFAULT
                setBackgroundColor(Color.BLACK)
                loadDataWithBaseURL("https://www.youtube-nocookie.com", html, "text/html", "utf-8", null)
            }
        },
        onRelease = { it.destroy() },
    )
}

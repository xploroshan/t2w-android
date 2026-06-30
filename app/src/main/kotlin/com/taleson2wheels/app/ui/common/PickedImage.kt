package com.taleson2wheels.app.ui.common

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Bytes + mime of an image chosen via the system picker. */
class PickedImage(val bytes: ByteArray, val mime: String) {
    /** A filename from [prefix] + the mime subtype, e.g. fileName("avatar") -> "avatar.jpeg". */
    fun fileName(prefix: String): String = "$prefix.${mime.substringAfter('/')}"
}

/**
 * Read a picked image's bytes (+mime) on [Dispatchers.IO].
 *
 * The `GetContent` result callback runs on the main thread, so reading the file
 * there — `contentResolver.openInputStream(uri).readBytes()` — blocks the UI for
 * the whole read and buffers the entire (possibly multi-MB) image in a main-thread
 * allocation, causing jank and risking OOM on constrained devices. Doing it here,
 * off the main thread, keeps the UI responsive. Returns null on any failure or an
 * empty stream.
 */
suspend fun Context.readPickedImage(uri: Uri): PickedImage? = withContext(Dispatchers.IO) {
    runCatching {
        val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return@runCatching null
        val mime = contentResolver.getType(uri) ?: "image/jpeg"
        PickedImage(bytes, mime)
    }.getOrNull()
}

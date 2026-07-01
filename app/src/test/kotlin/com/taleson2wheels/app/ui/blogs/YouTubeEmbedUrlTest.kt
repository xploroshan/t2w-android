package com.taleson2wheels.app.ui.blogs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class YouTubeEmbedUrlTest {

    private val expected = "https://www.youtube-nocookie.com/embed/dQw4w9WgXcQ?rel=0"

    @Test
    fun `parses watch, short, embed and youtu_be forms to a nocookie embed`() {
        assertEquals(expected, youTubeEmbedUrl("https://www.youtube.com/watch?v=dQw4w9WgXcQ"))
        assertEquals(expected, youTubeEmbedUrl("https://youtu.be/dQw4w9WgXcQ"))
        assertEquals(expected, youTubeEmbedUrl("https://www.youtube.com/embed/dQw4w9WgXcQ"))
        assertEquals(expected, youTubeEmbedUrl("https://www.youtube.com/shorts/dQw4w9WgXcQ"))
        // Extra query params around the id are tolerated.
        assertEquals(expected, youTubeEmbedUrl("https://youtube.com/watch?list=PL123&v=dQw4w9WgXcQ&t=30s"))
    }

    @Test
    fun `returns null for non-youtube or unparseable urls`() {
        assertNull(youTubeEmbedUrl("https://vimeo.com/123456789"))
        assertNull(youTubeEmbedUrl("https://example.com/video.mp4"))
        assertNull(youTubeEmbedUrl("just some text"))
        assertNull(youTubeEmbedUrl(""))
    }
}

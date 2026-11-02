package io.github.lokij07.vyntra.data

import org.junit.Assert.*
import org.junit.Test

class VideoLinksTest {
    @Test fun extractsWatchLink() {
        assertEquals("https://www.youtube.com/watch?v=dQw4w9WgXcQ",
            VideoLinks.selectedVideoUrl("https://m.youtube.com/watch?v=dQw4w9WgXcQ&t=5"))
    }
    @Test fun extractsShortsAndShortUrl() {
        assertEquals("https://www.youtube.com/watch?v=dQw4w9WgXcQ",
            VideoLinks.selectedVideoUrl("https://www.youtube.com/shorts/dQw4w9WgXcQ"))
        assertEquals("https://www.youtube.com/watch?v=dQw4w9WgXcQ",
            VideoLinks.selectedVideoUrl("https://youtu.be/dQw4w9WgXcQ"))
    }
    @Test fun refusesSearchAndImposterLinks() {
        assertNull(VideoLinks.selectedVideoUrl("https://youtube.com/results?search_query=sb19"))
        assertNull(VideoLinks.selectedVideoUrl("https://youtube.com.evil.tld/watch?v=dQw4w9WgXcQ"))
        assertNull(VideoLinks.selectedVideoUrl("javascript:alert(1)"))
        assertNull(VideoLinks.selectedVideoUrl("Adele Hello"))
    }
    @Test fun encodesSearchQuery() {
        assertEquals("https://m.youtube.com/results?search_query=SB19+GENTO",
            VideoLinks.searchUrl(" SB19 GENTO "))
    }
}

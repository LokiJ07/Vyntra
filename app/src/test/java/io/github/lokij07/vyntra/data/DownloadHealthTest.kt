package io.github.lokij07.vyntra.data

import org.junit.Assert.*
import org.junit.Test

class DownloadHealthTest {
    @Test fun failuresExplainHttpAndStorage() {
        assertTrue(DownloadHealth.failure(403).contains("denied"))
        assertTrue(DownloadHealth.failure(404).contains("not be found"))
        assertTrue(DownloadHealth.failure(429).contains("rate-limiting"))
        assertTrue(DownloadHealth.failure(1006).contains("storage"))
    }
    @Test fun invalidResponsesAreNotAudio() {
        assertTrue(DownloadHealth.isObviouslyNotAudio("text/html; charset=UTF-8"))
        assertTrue(DownloadHealth.isObviouslyNotAudio("application/json"))
        assertFalse(DownloadHealth.isObviouslyNotAudio("audio/mpeg"))
        assertFalse(DownloadHealth.isObviouslyNotAudio("application/octet-stream"))
        assertFalse(DownloadHealth.isObviouslyNotAudio(null))
    }
}

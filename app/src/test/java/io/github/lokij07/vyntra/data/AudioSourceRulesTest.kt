package io.github.lokij07.vyntra.data

import org.junit.Assert.*
import org.junit.Test

class AudioSourceRulesTest {
    @Test fun urlValidation() {
        assertTrue(AudioSourceRules.validHttpsUrl("https://example.org/audio"))
        assertFalse(AudioSourceRules.validHttpsUrl("http://example.org/a"))
        assertFalse(AudioSourceRules.validHttpsUrl("Song Title"))
        assertFalse(AudioSourceRules.validHttpsUrl("https://person:password@example.org/file"))
        assertFalse(AudioSourceRules.validHttpsUrl("https://example.org" + "a".repeat(2050)))
    }

    @Test fun safeFilenamesAndExtensions() {
        assertEquals("Adele Hello", AudioSourceRules.safeDisplayName("Adele/Hello"))
        assertEquals("Vyntra Audio", AudioSourceRules.safeDisplayName("..."))
        assertTrue(AudioSourceRules.supportedAudioExtension("M4A"))
        assertTrue(AudioSourceRules.supportedAudioExtension("opus"))
        assertFalse(AudioSourceRules.supportedAudioExtension("html"))
    }
}

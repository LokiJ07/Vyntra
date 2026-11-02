package io.github.lokij07.vyntra.data

import org.junit.Assert.*
import org.junit.Test

class AudioFilesTest {
    @Test fun sanitizesAudioNames() {
        assertEquals("Title _ song", AudioFiles.safeName("Title / song"))
        assertEquals("Audio", AudioFiles.safeName("<>"))
    }
    @Test fun acceptsOnlyDirectHttpsAudioLinks() {
        assertEquals("mp3", AudioFiles.directLinkExtension("https://example.com/music.mp3?x=1"))
        assertNull(AudioFiles.directLinkExtension("http://example.com/song.mp3"))
        assertNull(AudioFiles.directLinkExtension("https://example.com/index.html"))
        assertNull(AudioFiles.directLinkExtension("https://user:pass@example.com/song.mp3"))
    }
    @Test fun resolvesDeclaredMimeTypes() {
        assertEquals("flac", AudioFiles.extensionFor("audio/flac", "https://example.com/get"))
        assertEquals("mp3", AudioFiles.extensionFor("audio/mpeg", "https://example.com/get"))
        assertEquals("ogg", AudioFiles.extensionFor("application/ogg", "https://upload.wikimedia.org/audio.oga"))
    }
}

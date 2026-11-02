package io.github.lokij07.vyntra.data

import org.junit.Assert.*
import org.junit.Test

class JamendoPolicyTest {
    @Test fun onlyCreatorAllowedDownloadLinks() {
        assertTrue(JamendoPolicy.mayDownload(true, "https://storage.jamendo.com/example"))
        assertFalse(JamendoPolicy.mayDownload(false, "https://storage.jamendo.com/example"))
        assertFalse(JamendoPolicy.mayDownload(true, "http://storage.jamendo.com/example"))
        assertFalse(JamendoPolicy.mayDownload(true, ""))
    }

    @Test fun onlyPlausibleClientIdsAccepted() {
        assertTrue(JamendoPolicy.isClientIdValid("myExampleId123"))
        assertFalse(JamendoPolicy.isClientIdValid(""))
        assertFalse(JamendoPolicy.isClientIdValid("<script>"))
        assertFalse(JamendoPolicy.isClientIdValid("a b"))
    }
}

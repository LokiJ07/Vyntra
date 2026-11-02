package io.github.lokij07.vyntra.data

import org.junit.Assert.*
import org.junit.Test

class SearchRelevanceTest {
    @Test fun officialArtistSearchPrioritizesArtistOverTribute() {
        val real = SearchRelevance.score("Bruno Mars", "Just the Way You Are", "Bruno Mars", SearchField.BOTH)!!
        val tribute = SearchRelevance.score("Bruno Mars", "Bruno Mars Tribute Karaoke", "Random Performer", SearchField.BOTH)!!
        assertTrue(real > tribute)
    }

    @Test fun artistModeOnlyMatchesPerformer() {
        assertNotNull(SearchRelevance.score("SB19", "Gento", "SB19", SearchField.ARTIST))
        assertNull(SearchRelevance.score("SB19", "SB19 Tribute", "Anonymous", SearchField.ARTIST))
    }

    @Test fun songModeOnlyMatchesSongTitle() {
        assertNotNull(SearchRelevance.score("Gento", "GENTO", "SB19", SearchField.TITLE))
        assertNull(SearchRelevance.score("Gento", "My Track", "Gento Band", SearchField.TITLE))
    }

    @Test fun queryMustMatchAllTerms() {
        assertNotNull(SearchRelevance.score("Gento SB19", "Gento", "SB19", SearchField.BOTH))
        assertNull(SearchRelevance.score("Gento SB19", "Gento", "Another Singer", SearchField.BOTH))
        assertNull(SearchRelevance.score("Bruno Mars", "Mars Attacks", "Some Artist", SearchField.BOTH))
    }

    @Test fun punctuationAndAccentsDoNotBreakNames() {
        assertNotNull(SearchRelevance.score("Ben&Ben", "Leaves", "Ben & Ben", SearchField.ARTIST))
        assertNotNull(SearchRelevance.score("Beyonce", "Halo", "Beyoncé", SearchField.ARTIST))
    }

    @Test fun noQueryNoUnrelatedHits() {
        assertNull(SearchRelevance.score(" ", "Song", "Artist", SearchField.BOTH))
        assertNull(SearchRelevance.score("SB19", "Public announcement", "City Hall", SearchField.BOTH))
    }
}

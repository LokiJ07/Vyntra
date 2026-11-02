package io.github.lokij07.vyntra.data

import java.text.Normalizer
import java.util.Locale

/** Match the actual song and artist, not generic tags, sound effects or unrelated descriptions. */
enum class SearchField(val label: String, val apiAttribute: String?) {
    BOTH("All", null),
    ARTIST("Artist", "artistTerm"),
    TITLE("Song title", "songTerm")
}

/** Pure Kotlin so relevance can be tested without a phone or external API. */
object SearchRelevance {
    private val separator = Regex("[^\\p{L}\\p{N}]+")
    private val diacritics = Regex("\\p{M}+")

    fun normalize(input: String): String =
        Normalizer.normalize(input.lowercase(Locale.ROOT), Normalizer.Form.NFD)
            .replace(diacritics, "")
            .replace(separator, " ").trim().replace(Regex("\\s+"), " ")

    /** Returns null when a track should not appear for this search. Higher means more relevant. */
    fun score(query: String, title: String, artist: String, field: SearchField): Int? {
        val request = normalize(query)
        if (request.isEmpty()) return null
        val song = normalize(title)
        val performer = normalize(artist)
        val searchable = when (field) {
            SearchField.BOTH -> "$song $performer"
            SearchField.ARTIST -> performer
            SearchField.TITLE -> song
        }
        val words = searchable.split(' ').toSet()
        val searchTerms = request.split(' ').toSet()
        // Never show a completely unrelated track: every query term must be represented.
        if (!searchTerms.all { it in words }) return null
        var points = 70 + searchTerms.size * 8
        when (field) {
            SearchField.ARTIST -> {
                if (performer == request) points += 460
                else if (performer.startsWith("$request ")) points += 270
                else if (performer.contains(request)) points += 140
            }
            SearchField.TITLE -> {
                if (song == request) points += 460
                else if (song.startsWith("$request ")) points += 260
                else if (song.contains(request)) points += 135
            }
            SearchField.BOTH -> {
                if (performer == request) points += 480
                if (song == request) points += 430
                if (performer.startsWith("$request ")) points += 255
                if (song.startsWith("$request ")) points += 215
                if (performer.contains(request)) points += 100
                if (song.contains(request)) points += 85
                if (request.split(' ').any { it in song.split(' ') } &&
                    request.split(' ').any { it in performer.split(' ') }) points += 160
            }
        }
        // Prioritize original catalog recordings over unrelated karaoke/tribute recordings.
        val secondaryTerms = setOf("karaoke", "tribute", "instrumental", "type beat", "cover")
        if (secondaryTerms.any { term -> song.contains(term) && !request.contains(term) }) points -= 145
        return points
    }
}

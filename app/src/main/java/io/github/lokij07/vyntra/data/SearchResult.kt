package io.github.lokij07.vyntra.data

import io.github.lokij07.vyntra.model.AudioTrack

/** A page of supported, downloadable HTTPS audio search results. */
data class SearchResult(val tracks: List<AudioTrack>, val hasMore: Boolean)

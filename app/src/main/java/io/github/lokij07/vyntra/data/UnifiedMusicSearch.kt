package io.github.lokij07.vyntra.data

import io.github.lokij07.vyntra.model.AudioTrack
import kotlinx.coroutines.async
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withTimeout

/** Provider failures are isolated. Apple results never turn into a fake MP3 download button. */
data class UnifiedResults(
    val downloadable: List<AudioTrack>,
    val official: List<AudioTrack>,
    val notices: List<String>
)

class UnifiedMusicSearch(
    private val audius: AudiusRepository = AudiusRepository(),
    private val jamendo: JamendoRepository = JamendoRepository(),
    private val itunes: ItunesRepository = ItunesRepository()
) {
    suspend fun search(query: String, field: SearchField, jamendoId: String): UnifiedResults = supervisorScope {
        // Running the provider requests independently prevents a slow server from blocking others.
        val a = async { runCatching { withTimeout(19000) { audius.search(query, 1, field) } } }
        val j = if (JamendoPolicy.isClientIdValid(jamendoId))
            async { runCatching { withTimeout(19000) { jamendo.search(query, field, jamendoId) } } }
            else null
        val i = async { runCatching { withTimeout(32000) { itunes.search(query, 1, field) } } }
        val warnings = mutableListOf<String>()
        val ar = a.await().getOrElse { warnings.add("Audius: ${it.message ?: "unavailable"}"); SearchResult(emptyList(), false) }
        val jr = j?.await()?.getOrElse { warnings.add("Jamendo: ${it.message ?: "unavailable"}"); SearchResult(emptyList(), false) }
            ?: SearchResult(emptyList(), false)
        val ir = i.await().getOrElse { warnings.add("Official catalog: ${it.message ?: "unavailable"}"); SearchResult(emptyList(), false) }
        if (j == null) warnings.add("Jamendo optional: add your free Client ID in About to enable more downloadable songs.")
        UnifiedResults(
            downloadable = (ar.tracks + jr.tracks).filter { it.downloadable && it.downloadUrl != null }
                .distinctBy { it.id },
            official = ir.tracks.filter { it.isStoreCatalog }.distinctBy { it.id },
            notices = warnings
        )
    }
}

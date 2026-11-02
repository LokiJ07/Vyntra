package io.github.lokij07.vyntra.ui

import android.app.Application
import android.app.DownloadManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.lokij07.vyntra.data.AudioFiles
import io.github.lokij07.vyntra.data.DownloadsRepository
import io.github.lokij07.vyntra.data.OpenverseRepository
import io.github.lokij07.vyntra.model.AudioTrack
import io.github.lokij07.vyntra.model.DownloadRecord
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class VyntraViewModel(application: Application) : AndroidViewModel(application) {
    private val audioSearch = OpenverseRepository()
    private val downloader = DownloadsRepository(application)

    var query by mutableStateOf("instrumental")
    var link by mutableStateOf("")
    var tracks by mutableStateOf<List<AudioTrack>>(emptyList())
        private set
    var downloads by mutableStateOf<List<DownloadRecord>>(emptyList())
        private set
    var isSearching by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var page by mutableStateOf(1)
        private set
    var canLoadMore by mutableStateOf(true)
        private set

    init {
        refreshDownloads()
        search()
    }

    fun search(loadMore: Boolean = false) {
        val searchText = query.trim()
        if (searchText.isEmpty() || isSearching) return
        val targetPage = if (loadMore) page + 1 else 1
        isSearching = true
        error = null
        viewModelScope.launch {
            try {
                val found = audioSearch.search(searchText, targetPage)
                tracks = if (loadMore) (tracks + found).distinctBy { it.id } else found
                page = targetPage
                canLoadMore = found.size >= 15 // Results that lack HTTPS are intentionally omitted.
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (ex: Exception) {
                error = ex.message ?: "Unable to search. Check your internet connection."
            } finally {
                isSearching = false
            }
        }
    }

    fun download(track: AudioTrack): String = try {
        val ext = AudioFiles.extensionFor(track.fileType, track.audioUrl)
        if (ext == null) {
            "Unknown or unsupported audio format. Please check the original source."
        } else {
            downloader.enqueue(track.audioUrl, track.title, track.creator, track.licenseLabel,
                track.licenseUrl, track.landingUrl, ext)
            refreshDownloads()
            "Added to Downloads/Vyntra"
        }
    } catch (e: Exception) {
        "Download failed: ${e.message ?: "Unknown error"}"
    }

    fun downloadDirectLink(): String {
        val ext = AudioFiles.directLinkExtension(link.trim())
            ?: return "Enter a direct HTTPS audio link ending in .mp3, .m4a, .flac, .ogg, .opus, .wav or .aac."
        return try {
            val url = link.trim()
            val title = url.substringBefore('?').substringAfterLast('/').substringBeforeLast('.')
            downloader.enqueue(url, title.ifBlank { "Direct audio" }, "Direct link", "Verify source rights", ext = ext)
            link = ""
            refreshDownloads()
            "Added to Downloads/Vyntra"
        } catch (e: Exception) { "Download failed: ${e.message ?: "Unknown error"}" }
    }

    fun refreshDownloads() { downloads = downloader.refresh() }

    fun cancel(record: DownloadRecord) {
        downloader.cancel(record)
        refreshDownloads()
    }

    fun open(record: DownloadRecord): String? = try {
        if (record.status != DownloadManager.STATUS_SUCCESSFUL) "Download not finished yet."
        else { downloader.open(record); null }
    } catch (e: Exception) { e.message ?: "No audio player found." }
}

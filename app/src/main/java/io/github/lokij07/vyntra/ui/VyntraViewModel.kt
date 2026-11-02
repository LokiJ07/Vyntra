package io.github.lokij07.vyntra.ui

import android.app.Application
import android.app.DownloadManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.lokij07.vyntra.data.AudioFiles
import io.github.lokij07.vyntra.data.AudioSourceRules
import io.github.lokij07.vyntra.data.LocalAudioDownloader
import io.github.lokij07.vyntra.data.DownloadsRepository
import io.github.lokij07.vyntra.data.JamendoPolicy
import io.github.lokij07.vyntra.data.SearchField
import io.github.lokij07.vyntra.data.UnifiedMusicSearch
import io.github.lokij07.vyntra.model.AudioTrack
import io.github.lokij07.vyntra.model.DownloadRecord
import io.github.lokij07.vyntra.model.SavedAudio
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class VyntraViewModel(application: Application) : AndroidViewModel(application) {
    private val searcher = UnifiedMusicSearch()
    private val downloader = DownloadsRepository(application)
    private val audioDownloader = LocalAudioDownloader(application)
    private val prefs = application.getSharedPreferences("vyntra_settings", 0)

    var query by mutableStateOf("")
    var searchField by mutableStateOf(SearchField.BOTH)
        private set
    var link by mutableStateOf("")
    var selectedVideoTitle by mutableStateOf("")
    var audioBusy by mutableStateOf(false)
        private set
    var audioProgress by mutableIntStateOf(0)
        private set
    var audioStatus by mutableStateOf("No audio extraction started.")
        private set
    var savedAudio by mutableStateOf<List<SavedAudio>>(emptyList())
        private set
    var jamendoClientId by mutableStateOf(prefs.getString("jamendo_client_id", "").orEmpty())
        private set
    var downloadableTracks by mutableStateOf<List<AudioTrack>>(emptyList())
        private set
    var officialTracks by mutableStateOf<List<AudioTrack>>(emptyList())
        private set
    var notices by mutableStateOf<List<String>>(emptyList())
        private set
    var downloads by mutableStateOf<List<DownloadRecord>>(emptyList())
        private set
    var isSearching by mutableStateOf(false)
        private set
    var hasSearched by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    init { refreshDownloads() }

    fun saveAudioFromLink() {
        if (audioBusy) return
        val url = link.trim()
        if (!AudioSourceRules.validHttpsUrl(url)) {
            audioStatus = "Enter a valid HTTPS link. A song title alone is not a URL."
            return
        }
        audioBusy = true
        audioProgress = 0
        audioStatus = "Preparing audio engine..."
        val displayName = selectedVideoTitle.ifBlank { "Vyntra Audio" }
        viewModelScope.launch {
            try {
                val saved = audioDownloader.save(url, displayName) { progress ->
                    viewModelScope.launch(Dispatchers.Main) {
                        audioProgress = progress
                        audioStatus = "Downloading audio: $progress%"
                    }
                }
                savedAudio = audioDownloader.list()
                audioStatus = "Saved ${saved.fileName} to Music/Vyntra. Open it below in Downloads."
            } catch (ex: Exception) {
                audioStatus = "Audio download failed: ${ex.message ?: "Network/source error"}"
            } finally { audioBusy = false }
        }
    }

    fun openSavedAudio(audio: SavedAudio): String? = try {
        audioDownloader.open(audio)
        null
    } catch (ex: Exception) { ex.message ?: "Audio file could not be opened." }


    fun saveJamendoClientId(value: String): String {
        val sanitized = value.trim()
        if (sanitized.isNotEmpty() && !JamendoPolicy.isClientIdValid(sanitized)) {
            return "Invalid Client ID. Copy the ID from Jamendo developer portal (letters/numbers)."
        }
        prefs.edit().putString("jamendo_client_id", sanitized).apply()
        jamendoClientId = sanitized
        return if (sanitized.isEmpty()) "Jamendo disconnected. Audius and Official music remain available."
            else "Jamendo Client ID saved. Go to Discover and search again."
    }

    fun selectField(field: SearchField) {
        if (isSearching || field == searchField) return
        searchField = field
        if (query.isNotBlank()) search()
    }

    fun search() {
        val q = query.trim()
        if (q.isBlank() || isSearching) return
        // Clear old results immediately, not after the API responds.
        isSearching = true
        hasSearched = true
        error = null
        notices = emptyList()
        downloadableTracks = emptyList()
        officialTracks = emptyList()
        val field = searchField
        val clientId = jamendoClientId
        viewModelScope.launch {
            try {
                val result = searcher.search(q, field, clientId)
                downloadableTracks = result.downloadable
                officialTracks = result.official
                notices = result.notices
            } catch (ex: CancellationException) {
                throw ex
            } catch (ex: Exception) {
                error = ex.message ?: "Music providers are unreachable. Check internet connection."
            } finally { isSearching = false }
        }
    }

    fun download(track: AudioTrack): String {
        if (!track.downloadable || track.downloadUrl.isNullOrBlank())
            return "No permitted full-song download from this provider. Open its source for options."
        return try {
            val targetUrl = track.downloadUrl ?: return "This track has no direct download file."
            val ext = AudioFiles.extensionFor(track.fileType, targetUrl)
            if (ext == null) "Unsupported audio type. Check the original source."
            else {
                downloader.enqueue(targetUrl, track.title, track.creator, track.licenseLabel,
                    track.licenseUrl, track.landingUrl, ext)
                refreshDownloads()
                "Download queued. Check Downloads for completion or failure details."
            }
        } catch (e: Exception) { "Download failed: ${e.message ?: "Unknown error"}" }
    }

    fun downloadDirectLink(): String {
        val ext = AudioFiles.directLinkExtension(link.trim())
            ?: return "Paste a DIRECT HTTPS audio file link ending in .mp3, .m4a, etc. A song title or store page is not a file URL."
        return try {
            val url = link.trim()
            val title = url.substringBefore('?').substringAfterLast('/').substringBeforeLast('.')
            downloader.enqueue(url, title.ifBlank { "Direct audio" }, "Direct link",
                "Verify rights at source", ext = ext)
            link = ""
            refreshDownloads()
            "Download queued. Check Downloads for completion or failure details."
        } catch (e: Exception) { "Download failed: ${e.message ?: "Unknown error"}" }
    }

    fun refreshDownloads() { downloads = downloader.refresh(); savedAudio = audioDownloader.list() }
    fun cancel(record: DownloadRecord) { downloader.cancel(record); refreshDownloads() }
    fun open(record: DownloadRecord): String? = try {
        if (record.status != DownloadManager.STATUS_SUCCESSFUL) "Download not finished yet."
        else { downloader.open(record); null }
    } catch (e: Exception) { e.message ?: "No audio player found." }
}

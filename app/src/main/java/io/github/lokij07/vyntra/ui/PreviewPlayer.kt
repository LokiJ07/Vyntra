package io.github.lokij07.vyntra.ui

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.lokij07.vyntra.model.AudioTrack

/** One preview player at a time; resources are released with the Compose screen. */
class PreviewPlayer(private val context: Context) {
    private var player: MediaPlayer? = null
    var playingId by mutableStateOf<String?>(null)
        private set
    var loadingId by mutableStateOf<String?>(null)
        private set

    fun toggle(track: AudioTrack, onError: (String) -> Unit) {
        if (playingId == track.id || loadingId == track.id) { stop(); return }
        stop()
        val mediaPlayer = MediaPlayer()
        player = mediaPlayer
        loadingId = track.id
        try {
            mediaPlayer.setAudioAttributes(AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build())
            mediaPlayer.setDataSource(context, Uri.parse(track.audioUrl))
            mediaPlayer.setOnPreparedListener { prepared ->
                if (player === prepared) {
                    loadingId = null
                    playingId = track.id
                    prepared.start()
                }
            }
            mediaPlayer.setOnCompletionListener { stop() }
            mediaPlayer.setOnErrorListener { _, _, _ ->
                stop()
                onError("Audio preview is unavailable for this source.")
                true
            }
            mediaPlayer.prepareAsync()
        } catch (_: Exception) {
            stop()
            onError("Audio preview is unavailable for this source.")
        }
    }

    fun stop() {
        playingId = null
        loadingId = null
        player?.let { try { it.release() } catch (_: Exception) { } }
        player = null
    }
}

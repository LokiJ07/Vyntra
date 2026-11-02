package io.github.lokij07.vyntra.ui

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import io.github.lokij07.vyntra.data.VideoLinks

/** Embedded video search. The separate engine may extract audio only from accessible, permitted sources. */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun VideoSearchPage(
    initialQuery: String,
    onCopyLink: (String) -> Unit,
    onUseLink: (String, String) -> Unit,
    onSaveAudio: (String, String) -> Unit,
    audioBusy: Boolean,
    audioProgress: Int,
    audioStatus: String,
    onNotice: (String) -> Unit
) {
    var query by remember { mutableStateOf(initialQuery) }
    var searchRequest by remember { mutableStateOf(if (initialQuery.isBlank()) "about:blank" else VideoLinks.searchUrl(initialQuery)) }
    var selectedVideo by remember { mutableStateOf<String?>(null) }
    var selectedTitle by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(initialQuery.isNotBlank()) }
    var hasPermission by remember { mutableStateOf(false) }
    var canGoBack by remember { mutableStateOf(false) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    fun doSearch() {
        if (query.isBlank()) {
            onNotice("Type a song title or an artist first.")
        } else {
            selectedVideo = null
            selectedTitle = ""
            hasPermission = false
            loading = true
            searchRequest = VideoLinks.searchUrl(query)
        }
    }

    BackHandler(enabled = canGoBack) { webViewRef?.goBack() }

    Column(Modifier.fillMaxSize().padding(horizontal = 14.dp)) {
        Text("Videos inside Vyntra", fontSize = 22.sp, style = MaterialTheme.typography.titleLarge)
        Text(
            "Search and select a video here; copy its link or save accessible audio without opening another app.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.weight(1f),
                singleLine = true,
                label = { Text("Song or artist") },
                placeholder = { Text("e.g. SB19 GENTO") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { doSearch() })
            )
            Spacer(Modifier.width(8.dp))
            Button(onClick = { doSearch() }) {
                Icon(Icons.Default.Search, contentDescription = "Search videos")
            }
        }
        Spacer(Modifier.height(9.dp))
        if (selectedVideo != null) {
            Text("Selected: $selectedVideo", fontSize = 11.sp, maxLines = 2,
                color = MaterialTheme.colorScheme.secondary)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { onCopyLink(selectedVideo!!) }) {
                    Icon(Icons.Default.ContentCopy, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Copy Link", fontSize = 12.sp)
                }
                Button(onClick = { onUseLink(selectedVideo!!, selectedTitle) }) {
                    Icon(Icons.Default.Link, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Use Link", fontSize = 12.sp)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = hasPermission, onCheckedChange = { hasPermission = it })
                Text("I own this audio or have permission to save it.", fontSize = 11.sp,
                    style = MaterialTheme.typography.bodySmall)
            }
            Button(
                onClick = { onSaveAudio(selectedVideo!!, selectedTitle) },
                enabled = hasPermission && !audioBusy,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.AudioFile, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(if (audioBusy) "Saving audio..." else "Save Audio • inside Vyntra")
            }
            Text("This tries to save audio from an accessible source. It is not guaranteed to work for restricted videos.",
                fontSize = 11.sp, style = MaterialTheme.typography.bodySmall)
        } else {
            Text("Tap a video below to select its real link. Search pages cannot be copied as a video.",
                fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (audioBusy) {
            LinearProgressIndicator(progress = { audioProgress / 100f }, modifier = Modifier.fillMaxWidth())
            Text("Audio transfer: $audioProgress%", fontSize = 11.sp)
        }
        if (audioStatus != "No audio extraction started.") {
            Text(audioStatus, fontSize = 11.sp,
                color = if (audioStatus.startsWith("Audio download failed"))
                    MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary,
                maxLines = 3)
        }
        if (searchRequest == "about:blank") {
            Text("Start by searching for a song or artist above.", fontSize = 12.sp)
        }
        if (loading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(7.dp))
        key(searchRequest) {
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.allowFileAccess = false
                        settings.allowContentAccess = false
                        settings.javaScriptCanOpenWindowsAutomatically = false
                        settings.setSupportMultipleWindows(false)
                        settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                        webViewClient = object : WebViewClient() {
                            private fun recordLink(view: WebView?, url: String?) {
                                val videoUrl = VideoLinks.selectedVideoUrl(url)
                                if (videoUrl != selectedVideo) hasPermission = false
                                selectedVideo = videoUrl
                                selectedTitle = view?.title?.removeSuffix(" - YouTube")?.take(100).orEmpty()
                                canGoBack = view?.canGoBack() == true
                            }
                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                loading = true
                                recordLink(view, url)
                            }
                            override fun onPageFinished(view: WebView?, url: String?) {
                                loading = false
                                recordLink(view, url)
                            }
                            override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                                super.doUpdateVisitedHistory(view, url, isReload)
                                recordLink(view, url)
                            }
                            override fun shouldOverrideUrlLoading(
                                view: WebView?, request: android.webkit.WebResourceRequest?
                            ): Boolean {
                                val uri = request?.url ?: return true
                                return uri.scheme != "https" && uri.scheme != "http"
                            }
                        }
                        webViewRef = this
                        loadUrl(searchRequest)
                    }
                },
                modifier = Modifier.weight(1f).fillMaxWidth(),
                onRelease = { it.stopLoading(); it.destroy(); if (webViewRef === it) webViewRef = null }
            )
        }
        Text(
            "Audio downloads may fail on restricted sites. Save only content you own or have permission to download.",
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 8.dp)
        )
    }
}

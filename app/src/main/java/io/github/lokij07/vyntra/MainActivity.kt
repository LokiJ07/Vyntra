package io.github.lokij07.vyntra

import android.app.DownloadManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.lokij07.vyntra.data.VideoLinks
import io.github.lokij07.vyntra.data.SearchField
import io.github.lokij07.vyntra.model.AudioTrack
import io.github.lokij07.vyntra.model.DownloadRecord
import io.github.lokij07.vyntra.ui.VideoSearchPage
import io.github.lokij07.vyntra.ui.PreviewPlayer
import io.github.lokij07.vyntra.ui.VyntraViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import coil.compose.AsyncImage

private val Night = Color(0xFF0B0916)
private val CardColor = Color(0xFF17132A)
private val Accent = Color(0xFFA78BFA)
private val Cyan = Color(0xFF65DFEF)
private val Muted = Color(0xFFB1A9C9)
private val Purple = Color(0xFF6841C7)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { VyntraApp() }
    }
}

@Composable
private fun VyntraApp(vm: VyntraViewModel = viewModel()) {
    var activeTab by remember { mutableIntStateOf(0) }
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val snack = remember { SnackbarHostState() }
    val preview = remember { PreviewPlayer(context) }
    DisposableEffect(preview) { onDispose { preview.stop() } }

    fun announce(message: String) { scope.launch { snack.showSnackbar(message) } }
    fun openBrowser(url: String) {
        if (!url.startsWith("https://") && !url.startsWith("http://")) {
            announce("Source link is unavailable.")
            return
        }
        try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
        catch (_: Exception) { announce("No browser is available.") }
    }

    LaunchedEffect(activeTab) {
        if (activeTab == 2) {
            while (true) {
                vm.refreshDownloads()
                delay(1800)
            }
        }
    }
    val scheme = darkColorScheme(
        primary = Accent, secondary = Cyan, background = Night,
        surface = CardColor, onSurface = Color.White,
        onBackground = Color.White, onPrimary = Night
    )
    MaterialTheme(colorScheme = scheme) {
        Scaffold(
            containerColor = Night,
            snackbarHost = { SnackbarHost(snack) },
            bottomBar = {
                NavigationBar(containerColor = Color(0xFF120F22)) {
                    listOf(Triple("Music", Icons.Default.Search, 0),
                        Triple("Videos", Icons.Default.SmartDisplay, 1),
                        Triple("Downloads", Icons.Default.Download, 2),
                        Triple("About", Icons.Default.Info, 3)).forEach { (label, icon, idx) ->
                        NavigationBarItem(
                            selected = activeTab == idx,
                            onClick = { preview.stop(); activeTab = idx },
                            icon = { Icon(icon, contentDescription = label) },
                            label = { Text(label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Night,
                                selectedTextColor = Accent,
                                indicatorColor = Accent,
                                unselectedIconColor = Muted,
                                unselectedTextColor = Muted
                            )
                        )
                    }
                }
            }
        ) { inner ->
            Column(
                Modifier.fillMaxSize()
                    .background(Brush.verticalGradient(listOf(Night, Color(0xFF100B22), Night)))
                    .padding(inner)
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(start = 22.dp, end = 22.dp, top = 18.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier.size(40.dp).clip(RoundedCornerShape(13.dp))
                            .background(Brush.linearGradient(listOf(Purple, Cyan))),
                        contentAlignment = Alignment.Center
                    ) { Text("V", fontSize = 26.sp, fontWeight = FontWeight.Black, color = Color.White) }
                    Spacer(Modifier.width(10.dp))
                    Text("VYNTRA", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
                    Spacer(Modifier.weight(1f))
                    Text("v0.2.4", color = Muted, fontSize = 12.sp)
                }
                when (activeTab) {
                    0 -> DiscoverPage(vm, preview, ::announce, ::openBrowser,
                        onQueued = { activeTab = 2; vm.refreshDownloads() },
                        onCopy = { value ->
                            clipboard.setText(AnnotatedString(value))
                            announce("Credits copied. This is NOT an audio download URL.")
                        })
                    1 -> VideoSearchPage(
                        initialQuery = vm.query,
                        onCopyLink = { selected ->
                            clipboard.setText(AnnotatedString(selected))
                            announce("Actual video URL copied (not an MP3 file link).")
                        },
                        onUseLink = { selected, title ->
                            vm.link = selected
                            vm.selectedVideoTitle = title
                            activeTab = 0
                            announce("Video link selected. Try Save Audio for permitted content.")
                        },
                        onSaveAudio = { selected, title ->
                            vm.link = selected
                            vm.selectedVideoTitle = title
                            vm.saveAudioFromLink()
                            // Download progress and the saved file list live on this page.
                            activeTab = 2
                        },
                        audioBusy = vm.audioBusy,
                        audioProgress = vm.audioProgress,
                        audioStatus = vm.audioStatus,
                        onNotice = ::announce
                    )
                    2 -> DownloadsPage(vm, ::announce, ::openBrowser)
                    else -> AboutPage(vm, ::announce, ::openBrowser)
                }
            }
        }
    }
}

@Composable
private fun DiscoverPage(vm: VyntraViewModel, preview: PreviewPlayer,
                         toast: (String) -> Unit, openUrl: (String) -> Unit,
                         onQueued: () -> Unit, onCopy: (String) -> Unit) {
    var showDirectLink by remember { mutableStateOf(vm.link.isNotBlank()) }
    LaunchedEffect(vm.link) { if (vm.link.isNotBlank()) showDirectLink = true }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF5730A7), Color(0xFF163853))))
                    .padding(22.dp)
            ) {
                Column {
                    Text("SEARCH • DOWNLOAD • LISTEN", color = Cyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(10.dp))
                    Text("Your music.\nYour world.", fontSize = 31.sp, lineHeight = 36.sp,
                        fontWeight = FontWeight.ExtraBold, color = Color.White)
                    Spacer(Modifier.height(11.dp))
                    Text("One search for OPM and world music. Full downloads appear first when the artist permits them.",
                        fontSize = 13.sp, lineHeight = 19.sp, color = Color(0xFFE6DDF9))
                }
            }
        }
        item {
            SectionTitle("Find the right song", "Search real track titles and performer names")
            Spacer(Modifier.height(10.dp))
            Text("Downloadable music: Audius + Jamendo (optional). Official songs: iTunes PH + US. Store listings are not free MP3 downloads.",
                color = Muted, fontSize = 12.sp, lineHeight = 17.sp)
            Spacer(Modifier.height(10.dp))
            Text("Match by", color = Muted, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SearchField.entries.forEach { field ->
                    FilterChip(
                        selected = vm.searchField == field,
                        onClick = { preview.stop(); vm.selectField(field) },
                        enabled = !vm.isSearching,
                        label = { Text(field.label, fontSize = 11.sp) }
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = vm.query,
                    onValueChange = { vm.query = it },
                    modifier = Modifier.weight(1f), singleLine = true,
                    placeholder = { Text("e.g. SB19, Gento, Bruno Mars") },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = Accent) },
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { preview.stop(); vm.search() })
                )
                Spacer(Modifier.width(8.dp))
                FilledIconButton(onClick = { preview.stop(); vm.search() }, enabled = !vm.isSearching,
                    modifier = Modifier.size(52.dp), colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = Accent, contentColor = Night)) {
                    Icon(Icons.Default.ArrowForward, contentDescription = "Search")
                }
            }
        }
        item {
            ElevatedCard(colors = CardDefaults.elevatedCardColors(containerColor = CardColor),
                shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(17.dp)) {
                    TextButton(onClick = { showDirectLink = !showDirectLink }) {
                        Icon(Icons.Default.Link, null, tint = Cyan)
                        Spacer(Modifier.width(8.dp))
                        Text("Audio downloader • direct file or source link")
                        Spacer(Modifier.weight(1f))
                        Icon(if (showDirectLink) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (showDirectLink) "Collapse" else "Expand")
                    }
                    if (showDirectLink) {
                        Text("You can paste a direct MP3/M4A link, or use the Videos tab to select a source link. The audio engine saves native audio formats when the source permits it. YouTube and other services may block downloads.",
                            fontSize = 12.sp, color = Muted)
                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(value = vm.link, onValueChange = { vm.link = it },
                            singleLine = true, modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("https://example.com/song.mp3") },
                            shape = RoundedCornerShape(12.dp))
                        if (VideoLinks.selectedVideoUrl(vm.link) != null) {
                            Text("Video page detected. The Save Audio engine will try a supported audio-only stream; success is not guaranteed.",
                                fontSize = 12.sp, color = Cyan)
                        }
                        Spacer(Modifier.height(10.dp))
                        Button(onClick = {
                            val result = vm.downloadDirectLink()
                            toast(result)
                            if (result.startsWith("Download queued")) onQueued()
                        }, enabled = io.github.lokij07.vyntra.data.AudioFiles.directLinkExtension(vm.link.trim()) != null,
                            modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.Download, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Download direct MP3/M4A file")
                        }
                        Spacer(Modifier.height(10.dp))
                        var hasPermission by remember { mutableStateOf(false) }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = hasPermission, onCheckedChange = { hasPermission = it })
                            Text("I own this content or have permission to download its audio.",
                                fontSize = 12.sp, color = Muted, lineHeight = 16.sp)
                        }
                        Button(onClick = { vm.saveAudioFromLink(); toast("Audio request started; check Downloads for progress.") },
                            enabled = !vm.audioBusy && hasPermission && vm.link.trim().startsWith("https://"),
                            modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.AudioFile, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(7.dp))
                            Text(if (vm.audioBusy) "Saving audio..." else "Try saving audio from supported link")
                        }
                        if (vm.audioBusy) {
                            Spacer(Modifier.height(7.dp))
                            LinearProgressIndicator(progress = { vm.audioProgress / 100f },
                                modifier = Modifier.fillMaxWidth())
                        }
                        Text(vm.audioStatus, fontSize = 12.sp,
                            color = if (vm.audioStatus.startsWith("Audio download failed")) Color(0xFFFFB7B7) else Muted)
                    }
                }
            }
        }
        item {
            SectionTitle("Downloadable songs", "${vm.downloadableTracks.size} artist-authorized audio files")
        }
        if (vm.isSearching) {
            item { Box(Modifier.fillMaxWidth().padding(18.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Accent)
            } }
        }
        vm.error?.let { message ->
            item { Text(message, color = Color(0xFFFFABAB), fontSize = 13.sp) }
        }
        if (!vm.isSearching && vm.hasSearched && vm.downloadableTracks.isEmpty()) {
            item { EmptyCard("No matching downloads found from the connected sources. This does not mean the artist's official song does not exist; check Official songs below. Jamendo requires a free Client ID in About.") }
        }
        items(vm.downloadableTracks, key = { it.id }) { track ->
            TrackCard(track,
                isPlaying = preview.playingId == track.id,
                isLoading = preview.loadingId == track.id,
                onPreview = { preview.toggle(track, toast) },
                onDownload = {
                    val message = vm.download(track)
                    toast(message)
                    if (message.startsWith("Download queued")) onQueued()
                },
                onSource = { openUrl(track.landingUrl) },
                onLicense = { openUrl(track.licenseUrl) },
                onCopy = { onCopy(track.attribution) }
            )
        }
        item {
            SectionTitle("Official songs & artists", "${vm.officialTracks.size} genuine store listings • preview / buy only")
        }
        if (!vm.isSearching && !vm.hasSearched) {
            item { EmptyCard("Type a song or artist, e.g. SB19, Cup of Joe, Adele, or Bruno Mars. Vyntra searches available music in any language, not a hardcoded artist list.") }
        }
        if (!vm.isSearching && vm.hasSearched && vm.officialTracks.isEmpty()) {
            item { EmptyCard("No matching official songs found in the PH / US catalogs. Availability varies by country and artist.") }
        }
        items(vm.officialTracks, key = { it.id }) { track ->
            TrackCard(track,
                isPlaying = preview.playingId == track.id,
                isLoading = preview.loadingId == track.id,
                onPreview = { preview.toggle(track, toast) },
                onDownload = { toast("This is a store listing, not an MP3 file.") },
                onSource = { openUrl(track.landingUrl) },
                onLicense = { openUrl(track.licenseUrl) },
                onCopy = { onCopy(track.attribution) }
            )
        }
        if (vm.notices.isNotEmpty()) {
            item {
                ElevatedCard(colors = CardDefaults.elevatedCardColors(containerColor = CardColor)) {
                    Column(Modifier.padding(14.dp)) {
                        Text("Provider status", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        vm.notices.forEach { Text(it, color = Muted, fontSize = 12.sp,
                            modifier = Modifier.padding(top = 5.dp)) }
                    }
                }
            }
        }
        item {
            Text("Only genuine downloads are saved to Downloads/Vyntra. An official song listing may link to a store rather than a downloadable audio file.",
                color = Muted, fontSize = 11.sp, lineHeight = 16.sp,
                modifier = Modifier.padding(bottom = 14.dp))
        }
    }
}

@Composable
private fun TrackCard(track: AudioTrack, isPlaying: Boolean, isLoading: Boolean,
                      onPreview: () -> Unit, onDownload: () -> Unit,
                      onSource: () -> Unit, onLicense: () -> Unit, onCopy: () -> Unit) {
    ElevatedCard(shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = CardColor)) {
        Column(Modifier.padding(15.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(54.dp).clip(RoundedCornerShape(15.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF7542CF), Color(0xFF245D83)))),
                    contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.MusicNote, contentDescription = null, tint = Color.White, modifier = Modifier.size(29.dp))
                    if (track.artworkUrl.startsWith("https://")) {
                        AsyncImage(model = track.artworkUrl,
                            contentDescription = "${track.title} cover artwork",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop)
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(track.title, maxLines = 2, overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(Modifier.height(3.dp))
                    Text(track.creator, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        color = Muted, fontSize = 12.sp)
                }
                IconButton(onClick = onPreview, enabled = track.audioUrl.isNotBlank()) {
                    if (isLoading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    else Icon(if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Stop preview" else "Preview", tint = Cyan)
                }
            }
            Spacer(Modifier.height(8.dp))
            Text("${track.licenseLabel.ifBlank { "Details at source" }} • ${track.provider}",
                color = Muted, fontSize = 11.sp, maxLines = 2)
            if (track.isStoreCatalog) {
                Spacer(Modifier.height(5.dp))
                Text("Search result only • NOT a downloadable audio file",
                    color = Cyan, fontSize = 11.sp)
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
                if (track.isStoreCatalog) {
                    Button(onClick = onSource, modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp)) {
                        Icon(Icons.Default.OpenInNew, null, Modifier.size(17.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("View / buy", fontSize = 12.sp)
                    }
                } else {
                    Button(onClick = onDownload, enabled = track.downloadable,
                        modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 8.dp)) {
                        Icon(Icons.Default.Download, null, Modifier.size(17.dp))
                        Spacer(Modifier.width(5.dp))
                        Text(if (track.downloadable) "Download" else "Not downloadable", fontSize = 12.sp)
                    }
                }
                IconButton(onClick = onSource) {
                    Icon(Icons.Default.OpenInNew, contentDescription = "Visit source", tint = Muted)
                }
                IconButton(onClick = onLicense) {
                    Icon(Icons.Default.Gavel, contentDescription = "View license", tint = Muted)
                }
                IconButton(onClick = onCopy) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy credits (not a download link)", tint = Muted)
                }
            }
        }
    }
}

@Composable
private fun DownloadsPage(vm: VyntraViewModel, toast: (String) -> Unit, openUrl: (String) -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Your downloads", fontWeight = FontWeight.ExtraBold, fontSize = 25.sp)
                    Text("Saved to Downloads/Vyntra", fontSize = 12.sp, color = Muted)
                }
                IconButton(onClick = vm::refreshDownloads) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Accent)
                }
            }
        }
        item {
            SectionTitle("Saved audio", "Audio extraction files are saved in Music/Vyntra")
            if (vm.audioBusy) {
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(progress = { vm.audioProgress / 100f },
                    modifier = Modifier.fillMaxWidth())
            }
            Spacer(Modifier.height(6.dp))
            Text(vm.audioStatus, fontSize = 12.sp, color = Muted)
        }
        items(vm.savedAudio, key = { it.uri }) { audio ->
            ElevatedCard(colors = CardDefaults.elevatedCardColors(containerColor = CardColor)) {
                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.MusicNote, null, tint = Cyan)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(audio.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(audio.fileName, color = Muted, fontSize = 11.sp,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    TextButton(onClick = { vm.openSavedAudio(audio)?.let(toast) }) { Text("Play") }
                }
            }
        }
        item { SectionTitle("Direct downloads", "Files saved to Downloads/Vyntra") }
        if (vm.downloads.isEmpty() && vm.savedAudio.isEmpty()) {
            item { EmptyCard("Saved audio will appear here after a successful transfer. Try a permitted download link; unsupported sources may fail.") }
        }
        items(vm.downloads, key = { it.id }) { record ->
            DownloadCard(record, onOpen = { vm.open(record)?.let(toast) },
                onCancel = { vm.cancel(record); toast("Download canceled") },
                onSource = { openUrl(record.sourcePage) },
                onLicense = { openUrl(record.licenseUrl) })
        }
        item { Spacer(Modifier.height(12.dp)) }
    }
}

@Composable
private fun DownloadCard(record: DownloadRecord, onOpen: () -> Unit,
                         onCancel: () -> Unit, onSource: () -> Unit, onLicense: () -> Unit) {
    val wrongFormat = record.status == DownloadManager.STATUS_SUCCESSFUL &&
        io.github.lokij07.vyntra.data.DownloadHealth.isObviouslyNotAudio(record.responseMime)
    val state = when {
        wrongFormat -> "Invalid file: not audio"
        record.status == DownloadManager.STATUS_SUCCESSFUL -> "Completed"
        record.status == DownloadManager.STATUS_RUNNING -> "Downloading"
        record.status == DownloadManager.STATUS_PENDING -> "Queued"
        record.status == DownloadManager.STATUS_PAUSED -> "Paused"
        else -> "Failed"
    }
    val detail = when {
        wrongFormat -> "The source returned a web page or JSON instead of an audio file."
        record.status == DownloadManager.STATUS_FAILED ->
            io.github.lokij07.vyntra.data.DownloadHealth.failure(record.reason)
        record.status == DownloadManager.STATUS_PAUSED ->
            io.github.lokij07.vyntra.data.DownloadHealth.pause(record.reason)
        else -> null
    }
    ElevatedCard(shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = CardColor)) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LibraryMusic, null, tint = Cyan)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(record.title, maxLines = 2, overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.Bold)
                    Text(record.creator, fontSize = 12.sp, color = Muted)
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(state + (record.progress?.let { " • $it%" } ?: ""), fontSize = 12.sp,
                color = if (detail != null && (wrongFormat || record.status == DownloadManager.STATUS_FAILED)) Color(0xFFFFABAB) else Accent)
            if (detail != null) {
                Spacer(Modifier.height(5.dp))
                Text(detail, fontSize = 12.sp, color = Color(0xFFFFB7B7))
            }
            if (record.status == DownloadManager.STATUS_RUNNING && record.progress != null) {
                Spacer(Modifier.height(7.dp))
                LinearProgressIndicator(progress = { (record.progress ?: 0) / 100f },
                    modifier = Modifier.fillMaxWidth().height(5.dp).clip(CircleShape))
            }
            if (record.licenseLabel.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(record.licenseLabel, fontSize = 11.sp, color = Muted)
            }
            Spacer(Modifier.height(11.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (record.status == DownloadManager.STATUS_SUCCESSFUL && !wrongFormat) {
                    Button(onClick = onOpen) {
                        Icon(Icons.Default.PlayArrow, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Play / Open")
                    }
                } else {
                    OutlinedButton(onClick = onCancel) {
                        Text(if (record.status == DownloadManager.STATUS_FAILED || wrongFormat) "Remove failed" else "Cancel / remove")
                    }
                }
                Spacer(Modifier.weight(1f))
                if (record.sourcePage.isNotBlank()) IconButton(onClick = onSource) {
                    Icon(Icons.Default.OpenInNew, contentDescription = "Source", tint = Muted)
                }
                if (record.licenseUrl.isNotBlank()) IconButton(onClick = onLicense) {
                    Icon(Icons.Default.Gavel, contentDescription = "License", tint = Muted)
                }
            }
        }
    }
}

@Composable
private fun AboutPage(vm: VyntraViewModel, toast: (String) -> Unit, openUrl: (String) -> Unit) {
    var clientInput by remember(vm) { mutableStateOf(vm.jamendoClientId) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp))
                .background(Brush.linearGradient(listOf(Color(0xFF502994), Color(0xFF174D62))))
                .padding(26.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("V", fontSize = 70.sp, color = Color.White, fontWeight = FontWeight.Black)
                    Text("VYNTRA", letterSpacing = 4.sp, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                    Spacer(Modifier.height(4.dp))
                    Text("Discover. Download. Listen.", color = Color(0xFFDCD1FC))
                }
            }
        }
        item { SectionTitle("Music sources", "One search, different kinds of access") }
        item { AboutCard("Audius • free to search", "Creator-uploaded music. Only publicly downloadable tracks appear in the Downloadable songs section. Actual availability depends on creator permissions.") }
        item { AboutCard("Official music • Apple iTunes", "Search OPM and international artist/song listings in PH and US catalogs. These results can be previewed or opened in the official store; no full MP3 download from Apple previews.") }
        item {
            ElevatedCard(colors = CardDefaults.elevatedCardColors(containerColor = CardColor),
                shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.padding(17.dp)) {
                    Text("Jamendo downloads (optional)", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(7.dp))
                    Text("More openly licensed artist songs. Register a Jamendo developer app to obtain your public Client ID, then enter it below. Never enter passwords or bearer tokens.",
                        color = Muted, fontSize = 12.sp, lineHeight = 17.sp)
                    Spacer(Modifier.height(9.dp))
                    OutlinedTextField(value = clientInput, onValueChange = { clientInput = it.trim() },
                        label = { Text("Jamendo Client ID") }, singleLine = true,
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                    Spacer(Modifier.height(9.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { toast(vm.saveJamendoClientId(clientInput)) }) { Text("Save ID") }
                        OutlinedButton(onClick = { openUrl("https://devportal.jamendo.com/") }) {
                            Text("Get Client ID")
                        }
                    }
                }
            }
        }
        item { AboutCard("In-app video search and save", "Select a video in the Videos tab, confirm download permission and tap Save Audio. Vyntra attempts an audio-only download using the on-device engine. Availability and success depend on the source; restricted content can fail.") }
        item { AboutCard("Save and play", "Audio saved from a supported video link goes to Music/Vyntra. Direct MP3/M4A downloads go to Downloads/Vyntra. Both can be opened with LokiTunes or another compatible player.") }
        item { AboutCard("Licensing", "Not every song is free to download. Apple previews are for store promotion, not saving. Respect artist licenses and source terms.") }
        item { Text("Vyntra v0.2.3 • Android 10+ • Free & open source", color = Muted, fontSize = 11.sp) }
    }
}

@Composable
private fun AboutCard(title: String, description: String) {
    ElevatedCard(colors = CardDefaults.elevatedCardColors(containerColor = CardColor),
        shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.fillMaxWidth().padding(17.dp)) {
            Text(title, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(5.dp))
            Text(description, color = Muted, fontSize = 13.sp, lineHeight = 19.sp)
        }
    }
}

@Composable
private fun SectionTitle(title: String, subtitle: String) {
    Column {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text(subtitle, fontSize = 12.sp, color = Muted)
    }
}

@Composable
private fun EmptyCard(message: String) {
    ElevatedCard(colors = CardDefaults.elevatedCardColors(containerColor = CardColor),
        shape = RoundedCornerShape(18.dp)) {
        Text(message, modifier = Modifier.fillMaxWidth().padding(25.dp),
            fontSize = 13.sp, color = Muted, lineHeight = 19.sp)
    }
}

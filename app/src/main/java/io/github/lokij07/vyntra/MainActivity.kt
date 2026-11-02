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
import io.github.lokij07.vyntra.data.SearchField
import io.github.lokij07.vyntra.model.AudioTrack
import io.github.lokij07.vyntra.model.DownloadRecord
import io.github.lokij07.vyntra.ui.PreviewPlayer
import io.github.lokij07.vyntra.ui.SearchSource
import io.github.lokij07.vyntra.ui.VyntraViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
        if (activeTab == 1) {
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
                    listOf(Triple("Discover", Icons.Default.Search, 0),
                        Triple("Downloads", Icons.Default.Download, 1),
                        Triple("About", Icons.Default.Info, 2)).forEach { (label, icon, idx) ->
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
                    Text("v0.1.5", color = Muted, fontSize = 12.sp)
                }
                when (activeTab) {
                    0 -> DiscoverPage(vm, preview, ::announce, ::openBrowser,
                        onQueued = { activeTab = 1; vm.refreshDownloads() },
                        onCopy = { value ->
                            clipboard.setText(AnnotatedString(value))
                            announce("Credits copied. This is NOT an audio download URL.")
                        })
                    1 -> DownloadsPage(vm, ::announce, ::openBrowser)
                    else -> AboutPage(::openBrowser)
                }
            }
        }
    }
}

@Composable
private fun DiscoverPage(vm: VyntraViewModel, preview: PreviewPlayer,
                         toast: (String) -> Unit, openUrl: (String) -> Unit,
                         onQueued: () -> Unit, onCopy: (String) -> Unit) {
    var showDirectLink by remember { mutableStateOf(false) }
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
                    Text("DISCOVER • DOWNLOAD • LISTEN", color = Cyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(10.dp))
                    Text("Your music.\nYour world.", fontSize = 31.sp, lineHeight = 36.sp,
                        fontWeight = FontWeight.ExtraBold, color = Color.White)
                    Spacer(Modifier.height(11.dp))
                    Text("Discover real artists and songs. Download only when the creator allows it.",
                        fontSize = 13.sp, lineHeight = 19.sp, color = Color(0xFFE6DDF9))
                }
            }
        }
        item {
            SectionTitle("Find the right song", "Search real track titles and performer names")
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SearchSource.entries.forEach { source ->
                    FilterChip(
                        selected = vm.searchSource == source,
                        onClick = { preview.stop(); vm.selectSource(source) },
                        enabled = !vm.isSearching,
                        label = { Text(source.label, fontSize = 12.sp) }
                    )
                }
            }
            Text(if (vm.searchSource == SearchSource.AUDIUS)
                    "Tap Download on an eligible result; no copying or pasting needed. Only songs enabled by the creator."
                 else "Official titles and artists, preview/store links ONLY. To download artist-approved music, select Artist downloads.",
                 color = Muted, fontSize = 12.sp)
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
                        Text("Advanced: direct audio file URL (optional)")
                        Spacer(Modifier.weight(1f))
                        Icon(if (showDirectLink) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (showDirectLink) "Collapse" else "Expand")
                    }
                    if (showDirectLink) {
                        Text("NOT for song titles, copied credits, iTunes links, or normal music pages. Only paste a direct authorized audio file link ending in .mp3, .m4a, etc.",
                            fontSize = 12.sp, color = Muted)
                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(value = vm.link, onValueChange = { vm.link = it },
                            singleLine = true, modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("https://example.com/song.mp3") },
                            shape = RoundedCornerShape(12.dp))
                        Spacer(Modifier.height(10.dp))
                        Button(onClick = {
                            val result = vm.downloadDirectLink()
                            toast(result)
                            if (result.startsWith("Download queued")) onQueued()
                        }, enabled = vm.link.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.Download, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Download direct audio file")
                        }
                    }
                }
            }
        }
        item {
            SectionTitle(
                "Matching results",
                if (vm.tracks.isEmpty()) {
                    "Real titles and artist names only"
                } else {
                    "${vm.tracks.size} relevant track${if (vm.tracks.size == 1) "" else "s"} found"
                }
            )
        }
        if (vm.isSearching && vm.tracks.isEmpty()) {
            item { Box(Modifier.fillMaxWidth().padding(18.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Accent)
            } }
        }
        vm.error?.let { message ->
            item { Text(message, color = Color(0xFFFFABAB), fontSize = 13.sp) }
        }
        if (!vm.isSearching && vm.error == null && vm.tracks.isEmpty()) {
            item { EmptyCard(if (vm.query.isBlank())
                "Enter an artist or song title to search. Try SB19, Bruno Mars or Gento."
                else if (vm.searchSource == SearchSource.AUDIUS)
                    "No artist-approved downloadable tracks matched exactly. Try Official music to find the song listing."
                else "No matching song or artist in this store catalog. Try another spelling or choose Artist / Song title.") }
        }
        items(vm.tracks, key = { it.id }) { track ->
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
                onCopy = { onCopy(track.attribution.ifBlank {
                    "${track.title} — ${track.creator} | ${track.licenseLabel} | ${track.landingUrl}"
                }) }
            )
        }
        if (vm.tracks.isNotEmpty() && vm.canLoadMore) {
            item {
                OutlinedButton(
                    onClick = { vm.search(loadMore = true) },
                    enabled = !vm.isSearching,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp)
                ) { Text(if (vm.isSearching) "Loading..." else "Load more") }
            }
        }
        item {
            Text("Results must match your artist or song title. Artist downloads are limited to tracks the creator permits. Official music results link to the store, not free MP3 files.",
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
        if (vm.downloads.isEmpty()) {
            item { EmptyCard("Songs you download will appear here. They can also be found in your phone's Downloads/Vyntra folder.") }
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
private fun AboutPage(openUrl: (String) -> Unit) {
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
        item { SectionTitle("How it works", "Independent app • LokiTunes-friendly") }
        item { AboutCard("1 • Discover", "Search official song listings by artist or title, or switch to artist-enabled Audius downloads. Results are filtered for actual matches.") }
        item { AboutCard("2 • Check permissions", "Audius creators control whether their track can be downloaded. Official iTunes listings are browse, preview and store-link only.") }
        item { AboutCard("3 • Download", "Authorized audio downloads go to the shared Downloads/Vyntra folder using Android Download Manager.") }
        item { AboutCard("4 • Listen offline", "Tap Play / Open to choose an installed audio player. LokiTunes may also discover these files in its local library scan.") }
        item {
            ElevatedCard(colors = CardDefaults.elevatedCardColors(containerColor = CardColor)) {
                Column(Modifier.padding(16.dp)) {
                    Text("Music licensing matters", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(7.dp))
                    Text("Vyntra does not rip paid streaming tracks or download Apple preview clips. Audius downloads require artist permission. Check the source rights before reuse.",
                        color = Muted, fontSize = 13.sp, lineHeight = 19.sp)
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(onClick = { openUrl("https://audius.co/") }) {
                        Text("About Audius")
                    }
                }
            }
        }
        item { Text("Vyntra v0.1.5 • Android 10+ • Free & open source", color = Muted, fontSize = 11.sp) }
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

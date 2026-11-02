# Vyntra v0.2.3

A free/ad-free Android music discovery and permitted-audio downloader designed to complement LokiTunes.

## In-app workflow

1. Open **Videos** and type a title or artist.
2. Tap a video in the embedded web search, then wait until the video URL is selected.
3. If you own the audio or have permission to download it, check the permission confirmation and tap **Save Audio**.
4. Vyntra goes to **Downloads**, where transfer progress and errors appear.
5. When complete, select **Play** from the saved audio list. Audio is saved in `Music/Vyntra` and can be opened with LokiTunes.

**Copy Link** and **Use Link** also work if you prefer the manual link field in the Music tab.

## Known limitations

- Embedded third-party search may fail or restrict WebView navigation depending on provider policies.
- The yt-dlp-based engine can fail on restricted or inaccessible sources. It does not bypass DRM and cannot guarantee audio downloads for popular commercial tracks.
- Extraction is performed while the app is running. It is **not** a persistent background download service. If you exit or the system kills the app, the transfer may stop.
- Output is the native source audio format (e.g. M4A or WebM/Opus), not necessarily MP3.
- Direct, permitted `.mp3`, `.m4a`, etc. links use Android Download Manager and save to `Downloads/Vyntra`.
- For music search results, iTunes listings are for discovery and store access; they are not direct downloadable MP3s. Audius/Jamendo depend on creator download permissions.

## Build / signing

Use the repository's GitHub Actions workflow. Keep the existing `VYNTRA_KEYSTORE_BASE64` and `VYNTRA_KEYSTORE_PASSWORD` repository secrets; never commit private keystores. The Android app ID remains `io.github.lokij07.vyntra`. For consistent installation updates, use only the signed release APK and keep versionCode increasing.

## Testing

Offline Kotlin unit tests and XML validation can be performed locally. The Android Gradle build and live provider/download behavior must be verified by GitHub Actions and on a device. No source can guarantee downloads of all commercial music without rights/authorization.

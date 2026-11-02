# VYNTRA 🎵

**Discover. Download. Listen.**

A separate, free, open-source Android music discovery and download companion. Designed to work alongside **LokiTunes** without changing it.

> **Status:** v0.1.0 starter/MVP. Source code included; APK will be produced by the GitHub Actions workflow once CI is green. Android build has **not** been executed in the provided environment.

## Features

- 🎧 Discover openly licensed audio with the **Openverse audio search API**, without an API key.
- ▶️ Stream a quick preview when the source supports Android MediaPlayer.
- 🔗 Download authorized direct HTTPS `.mp3`, `.m4a`, `.flac`, `.ogg`, `.opus`, `.wav`, or `.aac` files.
- 📥 Browse download status and progress from Android's system Download Manager.
- 🗂️ Save files in **Downloads/Vyntra/** (shared public Downloads folder).
- 🎵 Open finished audio in an installed player. LokiTunes can discover songs during its own local media-library scan.
- 🧾 View the original source and license; copy track attribution.
- 🌙 Compose dark UI with violet/cyan accents.

## Requirements

- Android **10 (API 29)** or newer.
- Internet access for discovery and downloading.
- GitHub Actions or Android Studio, JDK 17, Android SDK 35.
- Android Gradle Plugin **8.9.2**, Gradle **8.11.1**, Kotlin **2.0.21**.

## Build without local Android Studio (recommended)

1. Create an empty repository named `Vyntra` on GitHub. Don't add a README or `.gitignore` on GitHub.
2. Extract this zip and open a terminal in the extracted **Vyntra-v0.1.0** folder.
3. Push the project:

   ```bash
   git init
   git add .
   git commit -m "feat: Vyntra v0.1.0 music discovery and downloader"
   git branch -M main
   git remote add origin https://github.com/LokiJ07/Vyntra.git
   git push -u origin main
   ```

4. In GitHub: **Actions → Vyntra Android CI → latest run → Artifacts → Vyntra-v0.1.0-debug**.
5. Download, extract the workflow artifact and transfer `app-debug.apk` to your Android device. Install it (allow your file manager to install unknown apps if prompted).

**Important:** The provided project uses a version-pinned system Gradle installation in GitHub Actions instead of including the Gradle wrapper JAR. For local CLI builds, install Gradle 8.11.1 and run `gradle :app:assembleDebug`. Android Studio can also use a locally installed Gradle distribution. To generate a conventional wrapper from a local Gradle installation: `gradle wrapper --gradle-version 8.11.1`.

## How it works

- **Discover:** Type a search term, tap Search, listen to a source preview where supported, inspect the source's license and choose Download.
- **Direct link:** Paste an actual HTTPS URL to an audio file with a recognized extension. Vyntra does not extract audio from YouTube/Spotify or bypass DRM.
- **Downloads:** Review status; open the completed audio with your preferred installed music app.
- **LokiTunes:** If LokiTunes is configured to scan public audio files, rescan its library. Vyntra does not require LokiTunes to be installed.

## Copyright / license

Vyntra only supports downloads where rights permit them. Openverse aggregates third-party listings, and some metadata can be inaccurate, so confirm license/attribution requirements at the original source. Different Creative Commons licenses can require attribution or restrict certain uses. Downloading and playback do not remove those requirements. Direct link downloads are only for files you own or are authorized to download. API usage must comply with the source provider's terms.

The Vyntra **source code** is provided under MIT; this does *not* cover third-party music.

## Scope of v0.1.0

- No Spotify/YouTube ripping, no DRM bypass, and no guarantee any particular commercial song is indexed.
- Some Openverse links can return a protected/unavailable file, and preview support depends on remote format and server.
- The system Download Manager handles background transfers. Vyntra supports canceling pending downloads, **not pausing/resuming** individual transfers.
- Recorded history is local to Vyntra. Uninstalling Vyntra does not intentionally delete files in the shared Downloads folder.
- This first project is not yet a verified, publicly released APK. Check the CI build and test downloads on a real Android phone before publishing a release.

## Folder structure

```text
Vyntra/
├── .github/workflows/android.yml
├── app/build.gradle.kts
├── app/src/main/java/io/github/lokij07/vyntra/
│   ├── MainActivity.kt
│   ├── data/{AudioFiles,DownloadsRepository,OpenverseRepository}.kt
│   ├── model/{AudioTrack,DownloadRecord}.kt
│   └── ui/{PreviewPlayer,VyntraViewModel}.kt
├── app/src/test/...
├── build.gradle.kts
└── settings.gradle.kts
```

Made as an independent companion to LokiTunes. 💜

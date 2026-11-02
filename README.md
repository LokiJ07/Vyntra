# Vyntra v0.2.1 — In-app Video Search (honest beta)

Android 10+ · Kotlin / Jetpack Compose · free / no in-app ads

## What actually works in this source update

- **Videos tab:** Search YouTube's *mobile web search* **inside Vyntra** (Android WebView). No YouTube/Chrome app switch, API key, subscription, or additional search service required. Search OPM and international music; YouTube determines what results are shown.
- Tap any video in the embedded search, and Vyntra tracks its **real video URL**. Use **Copy Link** or **Use Link** to fill the Discover tab's link field. A search page is **never** mistaken for a video link.
- Keep existing **Music** search (official catalogs + artist-enabled Audius/Jamendo downloads), Android DownloadManager, and download status/history.
- Keep APK application ID, signing workflow, and signing secrets unchanged to support normal in-place upgrades, provided the new versionCode is higher.

## Explicit limitation — do not confuse copying a video URL with downloading MP3

YouTube watch URLs are **video pages**, not direct `.mp3`/`.m4a` files. This release **does not extract or download audio from YouTube videos**. Selecting **Use Link** inserts the URL, but the direct-file downloader refuses it, with an explanation. Android DownloadManager can only save actual downloadable audio files, not convert video web pages into MP3.

Music section's Download buttons work only for providers giving a permitted full-track URL. A downloaded audio file must display **Completed** in Downloads before it can be considered successful.

This is a concrete **in-app search and link-selection milestone**, not a claim that any copyrighted song can be downloaded for free. YouTube's developer policies restrict downloading and separating YouTube audio without prior written approval; see https://developers.google.com/youtube/terms/developer-policies . YouTube can also block or change behavior in embedded WebViews.

## How to update the existing repository

1. Extract the ZIP and copy the **contents of** `Vyntra-v0.2.1/` into the existing Vyntra Git repository, keeping the `.git` directory. Do **not** copy `vyntra-release.jks` or any signing password to Git.
2. From Git Bash in the project:

   ```bash
   git add -A
   git commit -m "feat: add in-app video search and link selection"
   git push origin main
   ```

3. Watch **GitHub Actions → Vyntra Android CI**. Download the **signed release APK** only after a green build. Existing `VYNTRA_KEYSTORE_BASE64` and `VYNTRA_KEYSTORE_PASSWORD` secrets must remain unchanged. GitHub Actions increments versionCode using `GITHUB_RUN_NUMBER`.
4. Install the new APK over existing Vyntra, provided the certificate/app ID match and the versionCode increased.
5. Test **Videos → search `SB19 GENTO` → tap video → Copy Link / Use Link**. The **link field** should contain a real `https://www.youtube.com/watch?v=...` URL. YouTube video links cannot be downloaded as MP3 through this version.
6. Separately test a permitted downloadable track in **Music**, then verify **Downloads → Completed**.

## Validation and troubleshooting

The `VideoLinks` normalizer is independently testable and has JUnit tests covering YouTube watch/short/shorts URL, encoded queries, and rejection of imposter domain or search page. This environment has no configured Android SDK, so full Gradle compilation and on-device WebView behavior still require **GitHub Actions / phone validation**. YouTube may show a sign-in, unavailable or consent page in a WebView depending on region/account/device.

The ZIP contains **source only**, no keystore or signing credentials. See `SIGNING_SETUP.md`.

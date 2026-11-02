# Vyntra v0.1.3 — Music Catalog Update 🎵

This version replaces the default **Wikimedia Commons sound archive** search with two music-oriented experiences:

- **Downloadable music — Audius:** Search for actual artist-uploaded tracks. A download button is enabled only when the creator marks that track downloadable and it has no download gate. Available songs depend on the Audius artist catalog, which may not include a particular OPM or commercial artist. Search results can also be streamed in-app, subject to Audius availability and terms.
- **Official songs — iTunes Search API (Philippines storefront):** Search popular Filipino (OPM) and international artists and track names. Results have official store links and streaming-only promotional previews, **not** full-song downloading. The source link must remain available beside each preview. [Apple's terms](https://performance-partners.apple.com/search-api) prohibit downloading, caching or synchronizing previews.

The app does **not** rip Spotify/YouTube/Apple Music and does not bypass DRM. A successful song search is not a promise that its full file is free to download. The user can still download authorized direct HTTPS audio files in the Direct Link section.

## Upgrade from v0.1.2

1. Extract this **source-only** ZIP into your existing Vyntra local repository (preserve `.git`).
2. **Keep your existing GitHub signing secrets** `VYNTRA_KEYSTORE_BASE64` and `VYNTRA_KEYSTORE_PASSWORD`. **Do not create a new key**.
3. Push your changes:

   ```bash
   git add -A
   git commit -m "feat: v0.1.3 music-focused Audius and official song search"
   git push origin main
   ```

4. GitHub Actions builds a stable-key signed APK artifact `Vyntra-v0.1.3-SIGNED-build-...` with a new `versionCode` using the run number.
5. Install that APK over your existing Vyntra. An in-place update requires the same app ID, same signing certificate, and a higher versionCode.

## What's preserved

- Original Vyntra application ID `io.github.lokij07.vyntra`
- Stable private APK signing through the previous two GitHub Secrets
- Existing download history, Android Download Manager integration, and authorized direct-audio-URL downloads
- LokiTunes interoperability for locally downloaded songs
- Android 10+ support, Compose dark UI, GitHub Actions SDK setup fix

## Test checklist

1. Select **Official songs** and search **SB19** or **Bruno Mars** — results should have proper track and artist labels, and a **View on iTunes** button, not a free full-song Download button.
2. Select **Downloadable music** and search **lofi** or **electronic** — shows music tracks from Audius. Some are stream-only; only creator-enabled tracks show an active Download button.
3. Download an authorized track, confirm it appears in Downloads/Vyntra, and then play in LokiTunes.
4. Confirm your signing certificate is unchanged from v0.1.2, and the APK updates in place.

## Validation notes

Source-level validation only. The complete Android Gradle build and live API requests have not been run in this execution environment. GitHub Actions and on-device testing are required. APIs can change or rate-limit and do not guarantee any particular artist's catalog will appear.

Do not commit your private `.jks`, passwords, or GitHub Secrets.

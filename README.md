# Vyntra v0.1.5 — Search Accuracy Update

This version repairs the relevance problems in v0.1.3. The old app started by searching `pop` on Audius and accepted all API results without checking the actual track title or artist, creating confusing results.

## What's fixed

- **Official music first:** the app opens on the iTunes catalog, not an unrelated Audius `pop` search.
- **No automatic search:** type a specific artist or song title and submit.
- **Search modes:** All, Artist, Song title. Artist and Song title use the official iTunes API's `artistTerm` / `songTerm` attributes and post-filter the actual artist/title metadata.
- **Exact-term filtering:** all words must appear in the artist and/or title as appropriate, with strict sorting; instrumental / karaoke / tribute tracks are ranked lower.
- **PH, then US fallback:** if no relevant PH storefront hits are found, check the US storefront once.
- **Artist downloads:** only Audius tracks marked by the creator as downloadable and not access-gated are displayed. Nonmatching tracks are filtered out. This may honestly return zero results.
- **Build fix:** removed a leftover import of the deleted Openverse repository.
- Added JUnit test cases for song-name, artist-name, multi-word queries and unrelated matches.

**The purpose of the official-song catalog is accurate artist/title discovery, not free full-song downloading.** Apple iTunes previews are streamed promotional audio only (linked to the corresponding store listing). An official song appearing in the search results does not mean the track is authorized for free download. Audius downloads are limited to artist-approved material. Vyntra does not bypass DRM or rip premium streaming services.

## Update from v0.1.3

1. Extract this source ZIP and copy its **contents** over your existing Vyntra project (keep the local `.git` folder).
2. **Keep the existing two GitHub signing secrets.** Do not create a new key. This ZIP includes no private key or password.
3. Run:

   ```bash
   git add -A
   git commit -m "fix: accurate Vyntra song and artist search v0.1.5"
   git push origin main
   ```
4. When GitHub Actions is green, download the **signed APK** artifact. Install it over the previous version (same package ID and signing key; GitHub run number supplies a higher version code).

## Verify on Android

- `SB19` + **Artist** in **Official music**: results should all have SB19 in the artist name.
- `Gento` + **Song title**: track titles should contain Gento.
- `Bruno Mars` + **Artist**: no unrelated cover artists merely mentioning Bruno Mars in their track title.
- `Gento SB19` + **All**: every matching result must include both words across the title and artist.
- **Artist downloads** + search for an independent creator: only author-enabled download links appear; it is normal to find no download for a mainstream commercial recording.
- Confirm a direct authorized MP3 link can still be downloaded to `Downloads/Vyntra` and picked up by LokiTunes.

## Retained

- Package: `io.github.lokij07.vyntra`
- Permanent release signing via `VYNTRA_KEYSTORE_BASE64` and `VYNTRA_KEYSTORE_PASSWORD` secrets
- GitHub Actions Android SDK setup fix
- Existing download-history storage and Android Download Manager
- Android 10+, Compose UI and locally saved music playback through LokiTunes

## Validation status

Pure Kotlin search relevance tests were run locally. XML, Kotlin source structure and ZIP were inspected, but there is no Android SDK / Gradle build or live iTunes/Audius API access in this environment; the GitHub Actions and phone tests are still needed. Song availability varies by provider and region. Do not commit signing secrets or `.jks` files.


## v0.1.5: Download clarity and diagnostics

- Search results are not automatically downloadable. **Official music** uses iTunes listings only; its previews cannot be downloaded as full songs.
- Under **Artist downloads**, tap **Download** on a creator-enabled Audius track. No URL copying is required.
- After tapping Download, Vyntra takes you to **Downloads**. Queueing a file does **not** guarantee it finishes; check its status.
- Failed transfers now show Android DownloadManager's reason, including HTTP 403, 404 and storage/network errors.
- **Advanced direct audio file URL** is optional and is not for song names, copied credits or iTunes pages.
- Audius MP3 download endpoint requests `original=false` as supported by its API, rather than passing the unknown `format=mp3` query parameter.
- The app detects common server HTML/JSON error pages returned instead of audio. Unknown MIME types still need actual device playback verification.
- Same application ID and repository signing secrets as earlier stable signed builds.

**Testing:** XML and ZIP integrity plus small pure-Kotlin unit tests checked locally. Full Android build and provider transfer require GitHub Actions and device/network verification.

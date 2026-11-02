# Vyntra v0.2.0 — Unified Music Search

**Android 10+ · Kotlin + Jetpack Compose · free/open-source**

Vyntra searches multiple *real music catalogs* and offers downloads only when the music source/artist explicitly allows a full audio file to be downloaded. Downloaded files are saved in the Android shared `Downloads/Vyntra` directory for playback with LokiTunes or another local player.

## What is new

- One search box for **Filipino/OPM and international artists/song titles**. The search terms are not hardcoded and can be any language supported by providers.
- **Downloadable songs** first: Audius (publicly downloadable artist tracks) plus Jamendo (when configured).
- **Official songs & artists** second: iTunes Philippines **and** United States catalogs, with correct album art and official-store links.
- Provider error isolation: if one provider times out, others may still return results; status messages appear below search results.
- Download statuses and errors remain visible in Downloads. Genuine downloaded files can be opened using the installed audio player.
- Optional Jamendo public **Client ID** field in About; no rebuild needed when entering or changing it.
- Same `applicationId`, signing workflow and release signing secrets as v0.1.5, enabling normal install-over-installed updates when versionCode is higher.

## Important limits

- **It is not possible to get free MP3 downloads of every commercial OPM/international release from the iTunes API.** iTunes results are *store links and previews*, not downloadable full tracks. Previews are not downloaded or cached.
- Audius searches for music the creator allows to be downloaded; creators can disable downloads or gate access. Provider/server availability is not guaranteed.
- Jamendo requires registering for a Client ID at https://devportal.jamendo.com/; Jamendo results will NOT load until you enter that ID in Vyntra → About → Jamendo downloads. Jamendo downloads are only offered when `audiodownload_allowed=true` AND a full download URL exists.
- No third-party YouTube/Spotify ripper or DRM circumvention is included. Use sources in accordance with their license and terms.
- API providers, available music and playback may differ by territory. Some provider download endpoints may return HTTP errors; status appears in Downloads.

## Updating your existing GitHub repo

1. Extract this ZIP. Copy **the contents of** `Vyntra-v0.2.0` into your existing local `Vyntra` checkout. Keep its `.git` folder, and **do not copy private `.jks` keys into it**.
2. Retain your existing GitHub Actions secrets: `VYNTRA_KEYSTORE_BASE64`, `VYNTRA_KEYSTORE_PASSWORD`. Do not rotate them.
3. In Git Bash from the root of the existing checkout:

   ```sh
   git add -A
   git commit -m "feat: Vyntra v0.2.0 unified music search"
   git push origin main
   ```
4. Actions → **Vyntra Android CI** → successful run → Artifacts → **Vyntra-v0.2.0-SIGNED-build-...** → install signed APK on your phone. The app ID and key have not been intentionally changed.
5. Optional for more authorized downloads: visit the Jamendo developer portal, register an app, copy its public Client ID into **About → Jamendo downloads → Save ID**. This is an **ID**, not a password or bearer token. Entering it triggers Jamendo searches on the next search.
6. Test searches such as `SB19`, `Cup of Joe`, `Bruno Mars`, `indie`, `rock`, or `lofi`. A song appearing in the official catalog does **not** imply it is also downloadable.
7. To test a permitted download: use **Download** on a track under *Downloadable songs*, then **Downloads → Completed → Play / Open**. A queued download is NOT proof of success; wait for Completed.

## Validation

- Pure Kotlin search/permission checks: 12 passed locally.
- Repository files, ZIP and XML checked; Android SDK/Gradle build is **not available in the source-authoring environment**. The user's GitHub Actions run is the first full Android build and must be checked before treating this as a tested APK.
- Live provider calls were not possible from this environment, so success of real searches and downloads must be confirmed on the Android device.

## References

- Jamendo tracks: https://developer.jamendo.com/v3.0/tracks
- Audius tracks: https://docs.audius.co/sdk/tracks/
- Apple Search API terms: https://performance-partners.apple.com/search-api

## Safety / signing

The public ZIP must contain **no keystore, secret, password or private credential**. Keep the original v0.1.1 signing `.jks` backed up securely. Future versions must have higher versionCode and exactly the same Android app ID/signature to update without uninstall.

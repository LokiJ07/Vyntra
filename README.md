# Vyntra v0.2.2 — In-app audio saving (experimental)

Free, ad-free Android music discovery and downloads from **authorized/accessible** sources.

## New in v0.2.2

- The **Videos** tab still searches/opens videos inside Vyntra. Pick a video and tap **Use Link**.
- In **Music > Link downloader**, confirm you have permission, and tap **Save audio from supported link**.
- Uses the free MIT-licensed `dev.ffmpegkit-maintained:yt-dlp-android:2.0.2` Android engine.
- Downloads an **audio-only** stream when the provider allows it. No paid API key, external app, ads, or server.
- Shows extraction progress/errors and saves successful files via MediaStore into **Music/Vyntra**, visible to other players such as LokiTunes.
- Native original formats are retained (M4A, Opus, WebM audio, MP3 etc); **MP3 transcoding is not bundled**, and compatibility varies by music player.
- Existing direct-file DownloadManager remains unchanged (saves into **Downloads/Vyntra**).
- Existing GitHub Actions signing secrets, package ID, and release workflow are preserved.

### Limits

**Not a universal mainstream music downloader.** Search results on video services are not licensed audio downloads. YouTube and other sites can block extraction and have restrictions on downloading. Only download your own audio, openly licensed audio, or music you have permission to save. The user must confirm permission for each session. No bypass of DRM, login, payment, or restricted content is implemented.

The yt-dlp Android dependency is fairly large (~60–80 MB), and supports 64-bit Android (arm64-v8a / x86_64). Some services require cookies/login or specific network clients and may fail. `Save audio` is experimental until verified by real device tests. This app does not promise Spotify-style catalog availability or automatic MP3 conversion.

### Build

Keep your `.git` folder, overwrite the project source, and push to GitHub. Same `VYNTRA_KEYSTORE_BASE64` and `VYNTRA_KEYSTORE_PASSWORD` secrets. CI produces a signed APK on pushes to main. Do not upload the JKS/password to the repo.

```bash
git add -A
git commit -m "feat: v0.2.2 in-app supported audio saving"
git push origin main
```

### Smoke test (suggested)

Use a publicly downloadable, explicitly licensed audio or video URL as the first test; check that it appears under **Music/Vyntra**. Separately test a direct `.mp3` link through the existing direct download field. Next test a permitted video page; if it fails, send the exact `Audio download failed:` message.

### Security

Only HTTPS URLs are accepted for extraction. Files download into a temporary app folder and are copied to Android MediaStore after a successful download; partial temp files are deleted. No private signing keys are bundled. The app does not request broad storage access.

### License

See root LICENSE. Bundled third-party dependencies retain their respective licenses and notices.

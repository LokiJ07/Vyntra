# Vyntra v0.2.3 — In-App Save Audio Flow

- Add a **Save Audio** action on the Videos tab when an actual video URL is selected.
- Require affirmative ownership/permission before starting an audio-only transfer.
- Jump to Downloads after saving is initiated, with persistent progress and status.
- Copy/Use Link remain available; no external app required for searching.
- Start with an empty search view instead of loading random `music` results when there is no query.
- Prevent mistaking a video page for a direct MP3/M4A file download.
- Improve audio source title extraction from the embedded page and cancellation status handling.
- Retain existing package ID and release signing settings for in-place updates.

**Limitations:** This is not a guarantee of a downloadable audio file for every video. Protected, restricted or unsupported sources may fail. A link is not the same as an MP3. The audio engine is experimental and does not transcode to MP3.

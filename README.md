# Media Downloader Pro

Android application for downloading video (`MP4`) and audio (`MP3`) streams using `yt-dlp`, `FFmpeg`, and `aria2c`.

## Features
- **Single & Batch Downloads:** Paste a direct media URL or load a `.txt` batch file with multiple links.
- **Format & Quality Control:** Choose `MP4 Video` (`Best`, `1080p`, `720p`, `480p`) or `MP3 Audio` (`320K`, `192K`, `128K`).
- **Built-in Browser & Cookie Sync:** Browse media sites directly, download with one tap, and sync session cookies for authenticated streams.
- **Custom Save Folder:** Save to `Downloads/MediaDownloader` or pick any custom folder via Android Storage Access Framework (SAF).
- **Media Library:** Search, filter (`All`, `Video`, `Audio`), play, share, and manage downloaded files.

## Downloading the APK from GitHub Releases

Every push to `main` / `master` (or manual run in the **Actions** tab) automatically builds the APK and publishes a new release in the **Releases** section on the right side of the GitHub repository page:

1. Open the **Releases** section of your GitHub repository.
2. Under the latest release (`Media Downloader Pro v1.0.x`), open **Assets** and download **`MediaDownloaderPro.apk`**.
3. The APK is also attached to each workflow run under **Actions → Artifacts (`MediaDownloaderPro-APK`)**.

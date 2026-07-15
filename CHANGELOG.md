# Changelog

## 2.4.1 - Playback continuity and cover rotation

- Keep a playlist advancing after the app task is removed while the sleep timer remains active.
- Persist active playback correctly while the next queue item is preparing.
- Restore the current service state when the Activity returns to the foreground.
- Reset a rotating cover when the track changes and resume rotation after reopening the player.
- Remove the empty bottom inset above the Android navigation bar.
- Add physical-device instrumentation coverage for queue continuation and rotating covers.

## 2.4 - Continuous compatibility checks

- Run Android 8 and Android 16 instrumentation tests for every pull request.
- Keep the complete Android 8–16 emulator matrix on the weekly schedule and manual dispatch.
- Updated GitHub Actions to Node.js 24-compatible major versions.
- Added a reproducible physical-device checklist for Samsung, Xiaomi/Redmi, Pixel, and aggressive battery-saving firmware.
- Release tags, source code, release notes, and the signed APK now advance together as version 2.4.

## 2.3 - Controller redistribution

- Added Android instrumentation tests for application configuration, local crash reports, and background playback with a generated WAV.
- Verified the same test suite on Android 8 through Android 16 (API 26, 28–31, and 33–36) using a GitHub Actions emulator matrix.
- Added per-version JUnit, logcat, crash buffer, DropBox, MediaSession, and PlayerService diagnostic artifacts.
- Added a privacy-preserving local crash store that retains five reports and redacts music URI and storage paths.
- Moved playback queue commands, service start logic, and playback watcher behavior into `PlaybackController`.
- Moved mini-player state rendering into `PlayerUiController`.
- Moved settings screen rendering into `SettingsRenderer`.
- Moved song list rendering, chunked row rendering, and song row click handling into `SongsRenderer`.
- Removed unused generated-style wrapper methods and switched `SongsRenderer` to direct controller calls.

## 2.2 - MainActivity split and README refresh

- Split first UI responsibilities out of `MainActivity` into `SongsRenderer`, `PlayerUiController`, `SettingsRenderer`, `TabsController`, `PlaybackController`, and `ThemeController`.
- Replaced generated-looking `AnonymousClass...`, `RunnableC000...`, and `m...$$Nest...` names with readable names.
- Rebuilt the README in Russian and English with download/navigation buttons, phone screenshots, and contributor-oriented architecture notes.

## 2.1 - Repository quality update

- Moved the main music library, favorites, and playlists from JSON-in-SharedPreferences to a local SQLite database with legacy migration.
- Kept lightweight UI settings and playback resume snapshots in SharedPreferences.
- Added a `SongRowStateRegistry` helper so playback row state is no longer stored directly in `MainActivity`.
- Renamed the playback watcher from a generated-looking anonymous class to `PlaybackWatcher`.
- Added JVM tests for track sorting, legacy track JSON migration, playlist JSON round-trip, and playlist name cleanup.
- Release builds now run with R8 minify and resource shrinking enabled.
- GitHub Actions now builds the release APK artifact instead of publishing a debug APK artifact.
- Removed the generated APK from git tracking; release APKs should be downloaded from GitHub Actions or GitHub Releases.

## 2.0

- Added folder import through Android document tree picker.
- Added custom themes with user-selected background and text colors.
- Improved launcher, splash, and in-app vector icons.
- Added swipe-down close from the full player.
- Improved album art caching and playback duration handling.
- Switched playback preparation to `MediaPlayer.prepareAsync()`.

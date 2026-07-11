# Changelog

## 2.3 - Controller redistribution

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

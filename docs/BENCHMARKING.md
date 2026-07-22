# Voltune performance benchmarks

The `benchmark` module uses AndroidX Macrobenchmark. The app generates up to 10,000 synthetic metadata records in the dedicated benchmark build; no music files are committed.

## Scenarios

- cold, warm, and hot startup;
- opening and scrolling a 10,000-item library;
- switching Songs and Favorites;
- critical startup, library, full-player, playback, and tab paths covered by the baseline profile.

Additional device scenarios to record before publishing performance claims: first-track start, next-track transition, full-player opening, search, theme application, large artwork, and animated GIF background.

## Commands

```powershell
.\gradlew.bat :benchmark:assembleBenchmark
.\gradlew.bat :benchmark:connectedBenchmarkAndroidTest
```

Run benchmarks on a physical release-class device with stable temperature and battery. Reports are written under `benchmark/build/outputs` and `benchmark/build/reports`. Keep raw reports as CI artifacts; do not present emulator numbers as physical-device performance.

No performance value is considered verified until the device, Android version, build commit, iteration count, thermal state, and report artifact are recorded together.

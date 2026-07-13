# MP3 Player 2.4

## Русский

Актуальная официальная сборка локального MP3-плеера для Android.

### Надёжность и совместимость

- Каждый pull request теперь выполняет instrumentation-тесты на Android 8 и Android 16; полная матрица Android 8–16 остаётся еженедельной и доступна вручную.
- GitHub Actions обновлены до версий на Node.js 24, поэтому workflow больше не зависит от устаревшего Node.js 20.
- Добавлены instrumentation-тесты конфигурации приложения, foreground media service и локального crash store.
- Фоновое воспроизведение проверяется настоящим `MediaPlayer`: тест создаёт WAV, запускает песню, закрывает Activity, проверяет продвижение позиции и паузу.
- [Матрица Android Compatibility #1](https://github.com/dumuzeyn/MP3-player-APK/actions/runs/29256170782) успешно пройдена на Android 8, 9, 10, 11, 12, 13, 14, 15 и 16.
- Для каждой версии сформированы отдельные JUnit, logcat, crash buffer, DropBox, MediaSession и PlayerService-отчёты.
- Добавлены локальные обезличенные отчёты о сбоях: хранится не более пяти файлов, URI песен и пути хранилища удаляются.
- Добавлен воспроизводимый checklist для ручной проверки Samsung One UI, Xiaomi/Redmi HyperOS, Pixel и агрессивного энергосбережения. Эти устройства пока не отмечены как проверенные без реального оборудования.

### Архитектура

- `PlayerService` координирует отдельные компоненты очереди, восстановления состояния, аудиофокуса, эффектов и системной медиапанели.
- README содержит актуальные схемы взаимодействия файлов, тестовую матрицу и инструкции для разработчиков.
- Официальный APK подписывается только закрытым release-ключом, проходит R8, unit-тесты, lint и instrumentation-компиляцию.

## English

Current official build of the local Android MP3 player.

### Reliability and compatibility

- Every pull request now runs instrumentation tests on Android 8 and Android 16; the complete Android 8–16 matrix remains weekly and manually available.
- GitHub Actions use Node.js 24-compatible major versions and no longer depend on the deprecated Node.js 20 runtime.
- Added instrumentation tests for application configuration, the foreground media service, and the local crash store.
- Background playback uses a real `MediaPlayer`: the test generates a WAV, starts playback, closes the Activity, and verifies position progress and pause behavior.
- [Android Compatibility matrix #1](https://github.com/dumuzeyn/MP3-player-APK/actions/runs/29256170782) passed on Android 8, 9, 10, 11, 12, 13, 14, 15, and 16.
- Each Android version publishes separate JUnit, logcat, crash buffer, DropBox, MediaSession, and PlayerService reports.
- Added private local crash reports: no more than five files are retained, and music URIs and storage paths are redacted.
- Added a reproducible manual checklist for Samsung One UI, Xiaomi/Redmi HyperOS, Pixel, and aggressive battery saving. These devices remain explicitly unverified until real hardware is available.

### Architecture

- `PlayerService` coordinates dedicated queue, state restoration, audio focus, effects, and system media-panel components.
- README now includes current component diagrams, the compatibility matrix, and contributor instructions.
- The official APK is signed only with the private release key and passes R8, unit tests, lint, and instrumentation compilation.

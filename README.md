# MP3 Player APK

[English version](#engVER)

Нативный музыкальный плеер для Android, предназначенный для воспроизведения локальных MP3 и других поддерживаемых аудиофайлов.

Приложение работает без браузера, `WebView` и локального сервера. Музыка воспроизводится через Android foreground service, поэтому трек продолжает играть после сворачивания приложения или обычного свайпа из списка недавних приложений.

## Возможности

- Добавление аудиофайлов через системный файловый менеджер Android.
- Выбор нескольких аудиофайлов.
- Сохранение списка выбранных треков между запусками приложения.
- Чтение названия трека и имени исполнителя через `MediaMetadataRetriever`.
- Воспроизведение выбранного трека.
- Запуск списка воспроизведения с первого трека.
- Переключение на предыдущий и следующий трек.
- Пауза и продолжение воспроизведения.
- Управление воспроизведением через системное уведомление.
- Фоновое воспроизведение через `PlayerService`.
- Продолжение воспроизведения после сворачивания приложения.
- Работа без браузера, `WebView` и локального веб-сервера.

## Архитектура

Приложение разделено на две основные части:

- `MainActivity` отвечает за интерфейс, отображение списка треков и выбор аудиофайлов.
- `PlayerService` отвечает за воспроизведение музыки, фоновую работу и управление через уведомление.

Для выбора музыки используется системный Android file picker. Приложение получает доступ только к файлам, выбранным пользователем, и не требует полного доступа ко всей памяти устройства.

## Готовый APK

Собранный debug APK находится в каталоге:

```text
output\MP3-Player-debug.apk
```

> Debug APK предназначен для тестирования. Для публикации приложения рекомендуется создать отдельную release-сборку и подписать её собственным ключом.

## Установка на Android

1. Скопируйте файл `output\MP3-Player-debug.apk` на Android-устройство.
2. Откройте APK-файл на телефоне.
3. Разрешите установку приложений из этого источника, если Android запросит разрешение.
4. Завершите установку.
5. Запустите приложение **MP3 Player**.
6. Нажмите кнопку **«+ Добавить»**.
7. Выберите нужные аудиофайлы.

Исходные аудиофайлы не удаляются и не перемещаются.

### Разрешение на уведомления

На Android 13 и новее приложение может запросить разрешение на отправку уведомлений.

Рекомендуется предоставить это разрешение, поскольку уведомление используется для:

- отображения текущего воспроизведения;
- паузы и продолжения;
- переключения между треками;
- стабильной работы фонового воспроизведения.

## Ограничения Android

Приложение продолжает воспроизводить музыку после:

- сворачивания окна;
- блокировки экрана;
- перехода в другое приложение;
- обычного свайпа из списка недавних приложений.

Это возможно благодаря foreground service с постоянным системным уведомлением.

Однако если пользователь откроет настройки Android и нажмёт **«Остановить принудительно»**, система полностью завершит приложение и связанный с ним сервис. Обычное Android-приложение не может обойти это системное ограничение.

Некоторые производители устройств также могут ограничивать фоновую работу приложения из-за агрессивных настроек энергосбережения.

## Сборка APK

Для сборки требуется PowerShell и подключение к интернету при первом запуске скрипта.

Откройте PowerShell в корневой папке проекта и выполните:

```powershell
.\build-apk.ps1
```

Скрипт автоматически:

1. Проверяет наличие локального Android SDK в каталоге `.android-sdk`.
2. Скачивает Android SDK Command-line Tools, если они отсутствуют.
3. Устанавливает необходимые компоненты:
   - `platforms;android-35`;
   - `build-tools;35.0.0`;
   - `platform-tools`.
4. Компилирует ресурсы через `aapt2`.
5. Компилирует Java-код через `javac`.
6. Создаёт `classes.dex` через `d8`.
7. Упаковывает APK.
8. Выравнивает APK.
9. Подписывает APK.
10. Проверяет подпись командой `apksigner verify`.

После успешной сборки готовый файл появится здесь:

```text
output\MP3-Player-debug.apk
```

## Структура проекта

```text
./
├── app/
│   └── src/
│       └── main/
│           ├── java/
│           │   └── com/
│           │       └── rasul/
│           │           └── mp3player/
│           │               ├── MainActivity.java
│           │               ├── PlayerService.java
│           │               ├── Track.java
│           │               └── TrackStore.java
│           ├── res/
│           │   ├── drawable/
│           │   │   └── ic_launcher.xml
│           │   └── values/
│           │       ├── strings.xml
│           │       └── styles.xml
│           └── AndroidManifest.xml
├── .android-sdk/                # создается build-apk.ps1, не хранится в git
├── output/
│   └── MP3-Player-debug.apk
└── build-apk.ps1
```

## Основные файлы

| Файл | Назначение |
|---|---|
| `MainActivity.java` | Главный экран, список треков и выбор аудиофайлов |
| `PlayerService.java` | Фоновое воспроизведение и управление через уведомление |
| `Track.java` | Модель одного аудиотрека |
| `TrackStore.java` | Сохранение списка треков и чтение метаданных |
| `AndroidManifest.xml` | Разрешения, регистрация Activity и foreground service |
| `ic_launcher.xml` | Иконка приложения |
| `strings.xml` | Название приложения и текстовые ресурсы |
| `styles.xml` | Базовая тема Android-приложения |
| `build-apk.ps1` | Установка компонентов SDK и автоматическая сборка APK |
| `MP3-Player-debug.apk` | Готовая debug-сборка приложения |

## Доступ к аудиофайлам

Для выбора музыки используется системный Android file picker.

Такой подход позволяет:

- предоставлять приложению доступ только к выбранным файлам;
- не запрашивать полный доступ ко всей памяти устройства;
- не копировать аудиофайлы без необходимости;
- не изменять и не удалять исходные файлы;
- сохранять доступ к выбранным трекам между запусками приложения.

## Фоновое воспроизведение

Для фонового воспроизведения используется `PlayerService`, работающий как foreground service.

Во время воспроизведения Android показывает постоянное уведомление с элементами управления. Оно сообщает системе, что приложение выполняет заметную для пользователя задачу и не должно быть остановлено сразу после сворачивания интерфейса.

## Поддерживаемые файлы

Приложение рассчитано прежде всего на MP3-файлы. Возможность воспроизведения других форматов зависит от аудиокодеков, поддерживаемых конкретной версией Android и устройством.

## Известные ограничения

- Принудительная остановка приложения завершает воспроизведение.
- Некоторые производители Android-устройств могут ограничивать фоновую работу из-за настроек энергосбережения.
- Наличие названия трека и имени исполнителя зависит от метаданных аудиофайла.
- Повреждённые или неподдерживаемые аудиофайлы могут не воспроизводиться.
- Текущая APK-сборка является debug-версией.

## Безопасность файлов

Приложение не удаляет и не изменяет выбранные аудиофайлы. Оно хранит только информацию, необходимую для повторного доступа к выбранным пользователем трекам.

Если аудиофайл будет удалён или перемещён, сохранённая ссылка на него может перестать работать.

>Автор поекта: Зейналов У.Р.о.
---
<h1 id = engVER>
# MP3 Player APK
</h1>

A native Android music player designed for playing local MP3 files and other supported audio formats.

The application works without a browser, `WebView`, or a local server. Music playback is handled by an Android foreground service, so the current track continues playing after the app is minimized or normally swiped away from the recent apps screen.

## Features

- Add audio files through the Android system file picker.
- Select multiple audio files.
- Save the selected track list between app launches.
- Read track titles and artist names with `MediaMetadataRetriever`.
- Play a selected track.
- Start the playlist from the first track.
- Switch to the previous or next track.
- Pause and resume playback.
- Control playback from the system notification.
- Background playback through `PlayerService`.
- Continue playback after the app is minimized.
- No browser, `WebView`, or local web server required.

## Architecture

The application is divided into two main components:

- `MainActivity` handles the user interface, track list, and audio file selection.
- `PlayerService` handles music playback, background operation, and notification controls.

Music is selected through the Android system file picker. The application receives access only to files explicitly selected by the user and does not require full access to the device storage.

## Prebuilt APK

The compiled debug APK is available at:

```text
output\MP3-Player-debug.apk
```

> The debug APK is intended for testing. For public distribution, create a separate release build and sign it with your own signing key.

## Installing on Android

1. Copy `output\MP3-Player-debug.apk` to your Android device.
2. Open the APK file on the device.
3. Allow installation from this source if Android requests permission.
4. Complete the installation.
5. Launch **MP3 Player**.
6. Tap **“+ Add”**.
7. Select the audio files you want to play.

The original audio files are not deleted or moved.

### Notification permission

On Android 13 and newer, the app may request permission to send notifications.

It is recommended to allow this permission because the notification is used for:

- displaying the currently playing track;
- pausing and resuming playback;
- switching between tracks;
- keeping background playback stable.

## Android limitations

The application continues playing music after:

- the app window is minimized;
- the screen is locked;
- the user switches to another application;
- the app is normally swiped away from the recent apps screen.

This behavior is provided by a foreground service with a persistent system notification.

However, if the user opens Android settings and selects **Force stop**, the system completely terminates the application and its service. A regular Android application cannot bypass this system restriction.

Some device manufacturers may also restrict background activity because of aggressive battery-saving settings.

## Building the APK

PowerShell is required. An internet connection is also required the first time the build script runs.

Open PowerShell in the project root and run:

```powershell
.\build-apk.ps1
```

The script automatically:

1. Checks for a local Android SDK in the `.android-sdk` directory.
2. Downloads the Android SDK Command-line Tools if they are missing.
3. Installs the required components:
   - `platforms;android-35`;
   - `build-tools;35.0.0`;
   - `platform-tools`.
4. Compiles resources with `aapt2`.
5. Compiles the Java source code with `javac`.
6. Creates `classes.dex` with `d8`.
7. Packages the APK.
8. Aligns the APK.
9. Signs the APK.
10. Verifies the signature with `apksigner verify`.

After a successful build, the generated file will be available at:

```text
output\MP3-Player-debug.apk
```

## Project structure

```text
./
├── app/
│   └── src/
│       └── main/
│           ├── java/
│           │   └── com/
│           │       └── rasul/
│           │           └── mp3player/
│           │               ├── MainActivity.java
│           │               ├── PlayerService.java
│           │               ├── Track.java
│           │               └── TrackStore.java
│           ├── res/
│           │   ├── drawable/
│           │   │   └── ic_launcher.xml
│           │   └── values/
│           │       ├── strings.xml
│           │       └── styles.xml
│           └── AndroidManifest.xml
├── .android-sdk/                # created by build-apk.ps1, not stored in git
├── output/
│   └── MP3-Player-debug.apk
└── build-apk.ps1
```

## Main files

| File | Purpose |
|---|---|
| `MainActivity.java` | Main screen, track list, and audio file selection |
| `PlayerService.java` | Background playback and notification controls |
| `Track.java` | Data model for a single audio track |
| `TrackStore.java` | Track list persistence and metadata reading |
| `AndroidManifest.xml` | Permissions and registration of the Activity and foreground service |
| `ic_launcher.xml` | Application icon |
| `strings.xml` | Application name and text resources |
| `styles.xml` | Basic Android application theme |
| `build-apk.ps1` | SDK setup and automated APK build |
| `MP3-Player-debug.apk` | Prebuilt debug version of the application |

## Audio file access

Music is selected through the Android system file picker.

This approach allows the application to:

- access only the files selected by the user;
- avoid requesting full access to device storage;
- avoid copying audio files unnecessarily;
- leave the original files unchanged;
- preserve access to selected tracks between app launches.

## Background playback

Background playback is handled by `PlayerService`, which runs as a foreground service.

While music is playing, Android displays a persistent notification with playback controls. This tells the system that the application is performing a user-visible task and should not be stopped immediately after the interface is minimized.

## Supported files

The application is designed primarily for MP3 files. Support for other formats depends on the audio codecs available on the Android version and device.

## Known limitations

- Force-stopping the application ends playback.
- Some Android device manufacturers may restrict background activity because of battery optimization settings.
- Track titles and artist names depend on the metadata stored in each audio file.
- Damaged or unsupported audio files may not play.
- The current APK is a debug build.

## File safety

The application does not delete or modify selected audio files. It stores only the information required to access the tracks selected by the user.

If an audio file is deleted or moved, the saved reference to that file may stop working.

>**Author of project: Zeynalov U.R.o.


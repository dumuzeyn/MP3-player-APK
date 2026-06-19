# MP3 Player APK

<p align="center">
  <a href="https://github.com/dumuzeyn/MP3-player/raw/main/output/MP3-Player.apk">
    <img src="https://img.shields.io/badge/�������_APK-MP3--Player.apk-black?style=for-the-badge" alt="������� APK">
  </a>
</p>

[English version](#engVer)

MP3 Player APK - �������� Android-���������� ��� ��������� ������. ��� �������� ��� ��������, ��� WebView � ��� ���������� �������: ������������ ������������� APK, �������� ���������� ����� ��������� ����� ������ Android � ������� ������ ����� � ����������.

���������� ������ ������ ������ �� ��������� ������������� ����������. �������� ����� �� ����������, �� ���������� � �� ��������� � ����������. ���� ������������ ������ ��� ���������� ���� �������, ����������� ������ �� ���� ����� ��������� ��������.

## �������

������� APK ����� � ����������� �����:

```text
output/MP3-Player.apk
```

������ ������:

[������� MP3-Player.apk](https://github.com/dumuzeyn/MP3-player/raw/main/output/MP3-Player.apk)

���� Android ������������� �� ��������� �� ������������ ���������, ����� ��������� ��������� APK ��� ����������, ����� ������� ������ ����. ��� ������� ��������� Android ��� ����������, ������������� �� �� ��������.

## ��� ����� ����������

- ��������� ���� ��� ��������� ����� ����� ��������� �������� �������� Android.
- ���������� ������ ����� � ��������� � ���������� �������� ������.
- ������ ��������, �����������, ������, ���� � ������� ����� `MediaMetadataRetriever`.
- ����������� ����������: ������� ���������� ��������, ����� �������.
- ������ �����, ��������� � ��������� �� ���������� � ��������.
- ����������� ���� �����, ���� ������ ������ ��� ������ � ��������� �������.
- ��������� ���������� ���������.
- ��������� ����� � ��������� � ������� �� ��������� �������� �� ������.
- ��������� ����� � ������������ �������� ��� ��������� ����� �������� �� ���� �����.
- ������� ����� �� ���������� ��� �������� ����� � ����������.
- ������� �������� � ��������������.
- ��������� ������� ����� � ��������, ����������, ��������, ������, �������� � ��������.
- ���������� ����-����� ����� ������ �� ����� ���������������.
- �������� � ���� ����� Android foreground service � ��������� �����������.
- ����������� ������� � ������ �����-����� ����.

## ��� ������� ���������

������� ����� ���������� � `MainActivity.java` ��������� �����, ��� XML-��������. ����� ������ ������, ������ ��� ���������� ���������, � ��������� ����� �������: ��� ���������, ������, �������, ��������� ���� � ������ ������� ��������� ����� � ����� �����.

������� ����� ������ �������� �������� ����������, ������ � ������ ����� ����. ���� ��������� �������������� �������������� ������ ��������. ������� ����������� ������, ������� ��������� ������� ����� ������������ ����������.

�������� �������:

- `�����` - ��� ����������.
- `���������` - �����, ���������� �������.
- `���������` - ���������������� ������ �����.
- `�����` - ����������� �� ����� �� ����������.
- `�����������` - ����������� �� �����������.
- `�������` - ����������� �� �������.

������ ����� �������� ������� `songRow(...)`. � ��� �������� �������, ��������, �������� �����, ������ ������� ��� ����� � ������ play/pause. ���� ����� �������� ������ ������, �������, ������ ������� ��� ������������ ������, �������� ����� � ����� ������.

������� ����� ����������� ������� `openFullPlayer()`. � ��� ��������� �������, �������� �����, ������� � �������, ������ �������/�����/�������, �������� ��������� � ������ ����������, ������� � ��������� �����.

����-����� ��������� ������� `buildMiniPlayer()` � ����������� ������� `updateMini()`. �� ��������� ����� ������ � ���������� ��� �������� �������� ������.

## ��� �������� ���������������

��������������� �������� � `PlayerService.java`. ��� Android foreground service: ����� ������ ������, Android ����� ���������� ����������� � �����������. ��������� ����� ���� ���������� ������ ����� ������������ ����������, ���������� ������ ��� �������� ������ ���������� �� ������ ��������.

�����: ���� ������������ ������� ��������� ��������� Android � ������ �������������� ��������� ����������, Android �������� ������. ������� ���������� �� ����� �������� ��� ��������� �����������.

`MainActivity` ���������� ������� ������� ����� `Intent`:

- `PLAY_INDEX` - ��������� ����� �� �������.
- `TOGGLE` - ����� ��� �����������.
- `NEXT` - ��������� �����.
- `PREV` - ���������� �����.
- `STOP` - ���������.
- `SEEK` - ���������.
- `LOOP` - ��������� ������ �������.

������ ������ ��������� �������� �������, ������������, ������� � ��������� ��������������� � ����������� �����. ���������� ������ ��� ��������, ����� ��������� ��������� ��� ������� �������� � �������.

## ������� � ������ �������

����� ������������ �������� play � ��������� �����, ���������� ��������� ��������� ���������������. ����� ���������� ���� ����� ������� �� ������������ �������������, ���� ������������ �� ������� ������.

����� ������������ �������� `��� ������`, ������� ����������� �� �������� ������� � ������� �������. � ������� ������ ������� ������������ ������������ ���� �������, � �� ���� ����������.

����� ������������ �������� `��������`, ���������� ������� ����� ��� ����� �������� �������, ������������ �� � ��������� ������������ �������. ��� �� ���� ��������� �����, � ���� ������ � ��������� �������.

������ ����� ��� ���������:

1. ������ ��������.
2. ������ ����� �����.
3. ������ �������� ������.

## ��� �������� ������

������ ��������� ������ �������� � `SharedPreferences`. �� ��� �������� `TrackStore.java`. ��� �� ���������� ������ ���������� ����� `MediaMetadataRetriever`.

� `MainActivity.java` ������������� �����������:

- ��������� �����;
- ���������;
- ��������� ����;
- ���������������� �������� �������.

������ ���������� � ���������� �������� ��� JSON-������ � `SharedPreferences`. ��� ������� ������� ��� ���� ������, ������ ��� ����� ������ ���������: �������� ������� � URI ��������� ������.

## ������ �����

| ���� | �� ��� �������� |
|---|---|
| `app/src/main/AndroidManifest.xml` | Package name, ����������, ����������� Activity � ������� |
| `app/src/main/java/com/dumuzeyn/mp3player/MainActivity.java` | ���� ���������, ������, �������, ��������� ����, ����, ��������� |
| `app/src/main/java/com/dumuzeyn/mp3player/PlayerService.java` | ������� ���������������, MediaPlayer, �����������, media session |
| `app/src/main/java/com/dumuzeyn/mp3player/Track.java` | ������ ����� ����� |
| `app/src/main/java/com/dumuzeyn/mp3player/TrackStore.java` | ���������� ���������� � ������ ���������� |
| `app/src/main/java/com/dumuzeyn/mp3player/WaveformView.java` | ������� �������� ����� ��� ��������� ����� |
| `app/src/main/res/drawable/ic_launcher.xml` | ������ ���������� �� �������� |
| `app/src/main/res/drawable/ic_music_vector.xml` | ������ � ����� ���������� |
| `app/src/main/res/values/strings.xml` | �������� ���������� |
| `app/src/main/res/values/styles.xml` | ������� Android-���� |
| `build-apk.ps1` | �������������� ������ APK ��� Android Studio |
| `output/MP3-Player.apk` | ������� ������������ ���� |

## ��� �������� �������� ����������

�������� �� ������� ����� Android ��������� �:

```text
app/src/main/res/values/strings.xml
```

����� �������� �������� `app_name`:

```xml
<string name="app_name">MP3 Player</string>
```

����� ��������� ��������� ������ ������:

```powershell
.\build-apk.ps1
```

## ��� �������� package name

Package name ������ � ���� ������:

```text
app/src/main/AndroidManifest.xml
app/src/main/java/com/dumuzeyn/mp3player/*.java
```

���� ������ package name, �����:

1. �������� `package="..."` � `AndroidManifest.xml`.
2. �������� ������ `package ...;` � ������ Java-�����.
3. ������������� ����� ������ `app/src/main/java` ��� ����� package.
4. �������� action-������ � `PlayerService.java`, ����� ��� ��������������� ������ package.
5. ����������� APK.

## ��� �������� ������

�������� ����� �������������� � `MainActivity.java` � ������ `applyTheme()`. ������ ��������� �������� �� ���� �������: ������� � ������. ����� ������������ � ���� `bg`, `fg`, `panel`, `muted`, `line`.

���� ����� �������� ������� ���:

- `applyTheme()` - ����� ����.
- `buildHeader()` - ������� ������ � �������, ��������� � �������������� ����.
- `buildTabs()` � `styleTab(...)` - �������������� ���� ��������.
- `songRow(...)` - �������� �����.
- `renderPlaylists()` - �������� ����������.
- `renderGroups(...)` - �������� ������, ������������ � ��������.
- `openFullPlayer()` - ������� �����.
- `buildMiniPlayer()` � `updateMini()` - ������ ����-�����.
- `coverView()` - ���������� �������.
- `button(...)`, `icon(...)`, `shuffleButton()`, `searchButton()` - ����� ����� ������.

## ��� �������� ������

������ �� ������� ����� Android:

```text
app/src/main/res/drawable/ic_launcher.xml
```

������ � ����� ����������:

```text
app/src/main/res/drawable/ic_music_vector.xml
```

��� ������ ������� ��� vector drawable. ���� �������� �� �� ������ vector drawable, ���������� ��������� ���������� ��� �������������� �����������. ������ � ����� �������� ������ ���� ����� `setColorFilter`, ������� ��� �������� ������ �� ������� � ������ �����.

## ��� �������� �������� �����

�������� ����� �������� � `WaveformView.java`. ��� �� ����������� �������� ����, � ������ ���������� ������� �� ������ ������-����� �����. ����� ������ ������ ����������: �� �� ������ ������� �������� �� ������ ����� � ��� ���� ���� ������ ����� ������������ ���������� �������.

���� ����� ������ �����:

- ������� `onDraw(...)` � `WaveformView.java`;
- ������� ���������� ����� ����� ����;
- ������� ������ ����� ������� `heightFactor`;
- ������� ���� � ����� �������� `WaveformView` � `MainActivity.wave(...)`.

## ��� �������� ������ ����������

��������� �������� � ������ `Playlist` ������ `MainActivity.java`. ������ ���������� ��������� � ���� `playlists`. ���������� � �������� ����������� �������� `savePlaylists()` � `loadPlaylists()`.

������� ������:

- `renderPlaylists()` - ���������� ��� ���������.
- `openPlaylist(...)` - ��������� ���������� ��������.
- `openPlaylistPicker(...)` - ����� ��������� ��� ���������� �����.
- `openPickSongsPanel(...)` - ���� ������ ���������� �����.
- `confirmDeletePlaylist(...)` - ������������� �������� ���������.

## ��� ������� APK

�� Windows �������� PowerShell � ����� ������� � ���������:

```powershell
.\build-apk.ps1
```

������ ������ ��� ���:

1. ��������� ��������� Android SDK � `.android-sdk`.
2. ��� ������ ������� ��������� Android command line tools.
3. ������������� `platforms;android-35`, `build-tools;35.0.0` � `platform-tools`.
4. ����������� ������� ����� `aapt2`.
5. ������� APK.
6. ����������� Java ����� `javac`.
7. ����������� `.class` � `classes.jar`.
8. ������� `classes.dex` ����� `d8`.
9. ��������� dex � APK.
10. ������� ��������� ���� �������, ���� ��� ��� ���.
11. ����������� APK ����� `zipalign`.
12. ����������� APK ����� `apksigner`.
13. ��������� �������.

����� �������� ������ ���� �������� �����:

```text
output/MP3-Player.apk
```

����� `.android-sdk` � `build` �������� �������� ���������� ������. �� �� ����� ��������� � �����������.

## ��� ���������� ����� USB

���� �� ���������� ���������� ADB ��� ��� ������ SDK ���� ��������, ����� ���������� APK ���:

```powershell
.\.android-sdk\platform-tools\adb.exe install -r output\MP3-Player.apk
```

�� �������� ������ ���� �������� ������� �� USB, � ��������� ������ ���� �������� � ������� Android.

## ��������� �������

```text
Apk/
|-- app/
|   `-- src/
|       `-- main/
|           |-- AndroidManifest.xml
|           |-- java/
|           |   `-- com/
|           |       `-- dumuzeyn/
|           |           `-- mp3player/
|           |               |-- MainActivity.java
|           |               |-- PlayerService.java
|           |               |-- Track.java
|           |               |-- TrackStore.java
|           |               `-- WaveformView.java
|           `-- res/
|               |-- drawable/
|               |   |-- ic_launcher.xml
|               |   `-- ic_music_vector.xml
|               `-- values/
|                   |-- strings.xml
|                   `-- styles.xml
|-- output/
|   `-- MP3-Player.apk
|-- build-apk.ps1
|-- LICENSE
`-- README.md
```

## ��������

������ �������� ��� �������, �������� � ��������������� �������������. ������������ �������, ������������ ���������������, ����������� ��� ��������� � ������������ ������� ��� ���������� ���������� ���������. ����������� ��������� � `LICENSE`.

---

## Последние изменения интерфейса и логики APK

Эти изменения относятся к APK-версии приложения и находятся в `app/src/main/java/com/dumuzeyn/mp3player/MainActivity.java`.

### Кольцо меню и свайпы

Верхнее меню теперь работает как независимое горизонтальное кольцо. Его можно прокручивать пальцем свободно, и сама прокрутка не переключает раздел. Это важно для удобства: пользователь может сначала докрутить колесо до нужной области, посмотреть соседние пункты, а потом нажать на конкретный пункт.

Раздел переключается только двумя способами:

1. Нажатием на название раздела в верхнем колесе.
2. Свайпом по основной области экрана, но не по самому колесу меню.

Для этого в `dispatchTouchEvent(...)` все касания проходят через `handlePageSwipe(...)`. Метод `isInsideTabs(...)` проверяет, начался ли жест внутри `HorizontalScrollView` с меню. Если жест начался на колесе, он не считается свайпом страницы: колесо просто крутится. Если жест начался ниже, в основной области, приложение переключает вкладку влево или вправо.

При переключении вкладки вызывается `switchTabAnimated(...)`. Он делает две вещи одновременно:

- плавно сдвигает список текущего раздела в сторону и показывает новый список;
- докручивает верхнее колесо до активного пункта.

Докрутка колеса не использует обычный долгий `smoothScrollTo(...)`. Вместо него используется `ValueAnimator` в `animateTabsScrollTo(...)`, поэтому прокрутка короткая и управляемая. Метод `scrollTabsToActive(...)` ищет ближайшую копию нужной вкладки среди повторяющихся пунктов кольца. Благодаря этому переход между `Settings` и `Songs` не должен проворачивать весь список через все пункты, а должен выбирать ближайший визуальный путь.

### Меню Settings

В приложение добавлен раздел `Settings`. Он находится в том же верхнем колесе, что и `Songs`, `Favorites`, `Playlists`, `Genres`, `Artists` и `Albums`.

В `Settings` перенесено управление темой. Верхняя кнопка смены темы рядом с `MP3 Player` убрана, чтобы шапка была чище и не смешивала основные действия приложения с настройками.

В настройках доступны:

- переключение светлой и темной темы;
- выбор языка интерфейса: `English` или `Русский`;
- переход на GitHub проекта;
- удаление всех песен из приложения с подтверждением;
- удаление всех плейлистов с подтверждением.

Удаление песен из приложения не удаляет файлы с телефона. Оно очищает только список песен, избранное и ссылки на песни внутри плейлистов, после чего сохраняет новое состояние через `TrackStore.save(...)` и `saveState()`.

Удаление всех плейлистов очищает только коллекцию `playlists`; сами песни остаются в приложении.

Все тексты в настройках выровнены по левому краю. Это сделано в `addSettingsButton(...)`, а для кнопок выбора языка отдельно задается `Gravity.START | Gravity.CENTER_VERTICAL`.

### Переключение языка

Язык хранится в `SharedPreferences` под ключом `LANGUAGE`. Основные подписи интерфейса выбираются через метод `tr(String en, String ru)`. Если выбран английский язык, возвращается первая строка; если русский, вторая.

Список вкладок строится заново в `refreshTabLabels()`. Поэтому после смены языка приложение перестраивает интерфейс через `buildUi()`, и названия вкладок меняются сразу.

### Где менять эту логику

- `handlePageSwipe(...)` - распознает свайп страницы.
- `isInsideTabs(...)` - запрещает свайпу страницы срабатывать, если жест начался на колесе меню.
- `switchTabAnimated(...)` - отвечает за плавную смену раздела.
- `scrollTabsToActive(...)` - выбирает ближайшую копию нужной вкладки в бесконечном кольце.
- `animateTabsScrollTo(...)` - быстро и плавно докручивает колесо меню.
- `renderSettings(...)` - рисует раздел настроек.
- `addSettingsButton(...)` - задает стиль кнопок настроек и выравнивание текста влево.
- `refreshTabLabels()` - собирает названия вкладок на выбранном языке.
- `saveState()` - сохраняет тему, язык, избранное, плейлисты и пользовательское время таймера.
<h1 id = engVer>
 MP3 Player APK
</h1>

<p align="center">
  <a href="https://github.com/dumuzeyn/MP3-player/raw/main/output/MP3-Player.apk">
    <img src="https://img.shields.io/badge/Download_APK-MP3--Player.apk-black?style=for-the-badge" alt="Download APK">
  </a>
</p>

MP3 Player APK is a native Android application for local music playback. It does not use a browser, WebView, or a local web server. The user installs the APK, chooses audio files through the Android system file picker, and plays music directly inside the app.

The app stores only references to the audio files selected by the user. Original music files are not copied, modified, or deleted. If a file is deleted or moved manually, the saved reference may stop working.

## Download

The ready-to-install APK is stored here:

```text
output/MP3-Player.apk
```

Direct link:

[Download MP3-Player.apk](https://github.com/dumuzeyn/MP3-player/raw/main/output/MP3-Player.apk)

If Android warns about installing from an unknown source, allow APK installation for the app that opened the file. This is normal Android behavior for apps installed outside an app store.

## Features

- Add one or multiple songs through the Android system file picker.
- Display songs with covers and waveform visuals.
- Read title, artist, album, genre, and embedded cover with `MediaMetadataRetriever`.
- Sort the library with English titles first and Russian titles after that.
- Search songs, favorites, and playlists by title match.
- Play one song, the whole list in order, or the current list in shuffled order.
- Create persistent playlists.
- Add songs to favorites and remove them by tapping the filled heart again.
- Add songs to an existing playlist or create a new playlist from the song menu.
- Remove a song from the app without deleting the file from the device.
- Delete playlists with confirmation.
- Open a full player with cover art, seek bar, timer, like, repeat, and queue controls.
- Show a fixed mini-player at the bottom while music is active.
- Continue playback in the background through an Android foreground service.
- Switch between light and dark black-and-white themes.

## Interface structure

The main screen is built in `MainActivity.java` entirely from Java code, without XML layouts. This was chosen because the app is compact and the interface changed often: screens, buttons, tabs, panels, and list rows are kept close together in one place.

The top area contains the app icon and app name. Below it is a horizontally scrollable tab wheel. Tabs are repeated in cycles, so the section list feels endless while scrolling. Theme switching now lives in `Settings`.

Main sections:

- `Songs` - the full library.
- `Favorites` - songs marked with a heart.
- `Playlists` - user-created song lists.
- `Genres` - grouped by genre metadata.
- `Artists` - grouped by artist metadata.
- `Albums` - grouped by album metadata.

A song row is built by `songRow(...)`. It defines the cover, title, waveform, song action or favorite button, and play/pause button. To change row height, spacing, cover radius, or button placement, start with this method.

The full player is opened by `openFullPlayer()`. It contains the cover, song title, queue position, timer/like/repeat buttons, seek bar, and previous/current/next controls.

The mini-player is created by `buildMiniPlayer()` and updated by `updateMini()`. It is fixed at the bottom of the screen and hidden while the full player is open.

## Playback behavior

Playback is handled by `PlayerService.java`. It is an Android foreground service: while music is playing, Android shows a persistent notification with playback controls. Because of this, the current track continues after the app is minimized, the screen is locked, or the app is normally swiped away from recent apps.

Important: if the user opens Android settings and presses Force stop, Android terminates the service. A normal Android app cannot bypass this system restriction.

`MainActivity` sends commands to the service through `Intent` actions:

- `PLAY_INDEX` - play a song by index.
- `TOGGLE` - pause or resume.
- `NEXT` - next song.
- `PREV` - previous song.
- `STOP` - stop playback.
- `SEEK` - seek to a position.
- `LOOP` - change repeat mode.

The service stores the latest playback index, duration, position, and state in static fields. The activity reads these values to refresh the interface without a heavy service binding layer.

## Queue and play modes

When the user taps play on a single song, the app starts one-shot playback. After that song ends, the queue does not continue automatically unless repeat is enabled.

When the user taps `Play all`, the queue is built from the current section in the current order. The full player shows the position inside this queue, not inside the entire library.

When the user taps `Shuffle`, the app takes all songs from the current section, shuffles them, and starts the resulting queue. It is not one random song; it is the whole current list in random order.

Repeat has three states:

1. Repeat off.
2. Repeat one song.
3. Repeat current list.

## Stored data

The selected track list is stored in `SharedPreferences`. `TrackStore.java` handles this and also reads metadata through `MediaMetadataRetriever`.

`MainActivity.java` additionally saves:

- favorite songs;
- playlists;
- selected theme;
- selected language;
- custom timer value.

Favorites and playlists are stored as JSON strings in `SharedPreferences`. This avoids a database because the stored data is small: playlist names and selected file URIs.

## Important files

| File | Responsibility |
|---|---|
| `app/src/main/AndroidManifest.xml` | Package name, permissions, Activity and service registration |
| `app/src/main/java/com/dumuzeyn/mp3player/MainActivity.java` | Entire interface, lists, tabs, panels, themes, playlists |
| `app/src/main/java/com/dumuzeyn/mp3player/PlayerService.java` | Background playback, MediaPlayer, notification, media session |
| `app/src/main/java/com/dumuzeyn/mp3player/Track.java` | Single-song data model |
| `app/src/main/java/com/dumuzeyn/mp3player/TrackStore.java` | Library persistence and metadata reading |
| `app/src/main/java/com/dumuzeyn/mp3player/WaveformView.java` | Waveform visual under a song title |
| `app/src/main/res/drawable/ic_launcher.xml` | Android launcher icon |
| `app/src/main/res/drawable/ic_music_vector.xml` | Header icon inside the app |
| `app/src/main/res/values/strings.xml` | App name |
| `app/src/main/res/values/styles.xml` | Base Android theme |
| `build-apk.ps1` | Automated APK build without Android Studio |
| `output/MP3-Player.apk` | Ready-to-install APK |

## Changing the app name

The launcher name is stored in:

```text
app/src/main/res/values/strings.xml
```

Change `app_name`:

```xml
<string name="app_name">MP3 Player</string>
```

Then rebuild:

```powershell
.\build-apk.ps1
```

## Changing the package name

The package name is used in two places:

```text
app/src/main/AndroidManifest.xml
app/src/main/java/com/dumuzeyn/mp3player/*.java
```

To change it:

1. Update `package="..."` in `AndroidManifest.xml`.
2. Update the `package ...;` line in every Java file.
3. Rename the folders under `app/src/main/java` to match the new package.
4. Update action strings in `PlayerService.java` so they match the new package.
5. Rebuild the APK.

## Changing the design

Theme colors are calculated in `MainActivity.java` inside `applyTheme()`. The current interface uses two black-and-white modes. Colors are stored in `bg`, `fg`, `panel`, `muted`, and `line`.

Useful places for design changes:

- `applyTheme()` - theme colors.
- `buildHeader()` - top row with icon and app name.
- `buildTabs()`, `styleTab(...)`, `switchTabAnimated(...)`, and `scrollTabsToActive(...)` - horizontal section wheel, active tab styling, and animated navigation.
- `songRow(...)` - song card.
- `renderPlaylists()` - playlist cards.
- `renderGroups(...)` - genre, artist, and album cards.
- `openFullPlayer()` - full player.
- `buildMiniPlayer()` and `updateMini()` - bottom mini-player.
- `coverView()` - cover corner radius.
- `button(...)`, `icon(...)`, `shuffleButton()`, `searchButton()` - shared button style.

## Changing the icon

Android launcher icon:

```text
app/src/main/res/drawable/ic_launcher.xml
```

Header icon inside the app:

```text
app/src/main/res/drawable/ic_music_vector.xml
```

Both icons are vector drawables. Replacing them with other vector drawables keeps the project free from raster image dependencies. The header icon is tinted with the current theme color through `setColorFilter`, so it switches together with the light and dark theme.

## Changing the waveform

The waveform is drawn in `WaveformView.java`. It does not analyze the real audio stream; it creates a stable visual pattern from the song key. This was chosen intentionally to avoid expensive audio analysis while still giving songs visually different rows.

To change it:

- edit `onDraw(...)` in `WaveformView.java`;
- change the number of bars in the loop;
- change height through the `heightFactor` formula;
- change color where `WaveformView` is created in `MainActivity.wave(...)`.

## Changing playlists

Playlists are represented by the `Playlist` class inside `MainActivity.java`. The playlist collection is stored in the `playlists` field. Saving and loading happen in `savePlaylists()` and `loadPlaylists()`.

Main methods:

- `renderPlaylists()` - displays all playlists.
- `openPlaylist(...)` - opens a single playlist.
- `openPlaylistPicker(...)` - selects a playlist while adding a song.
- `openPickSongsPanel(...)` - selects several songs.
- `confirmDeletePlaylist(...)` - confirms playlist deletion.


## Latest APK Interface And Logic Changes

These changes belong to the APK version and are implemented mainly in `app/src/main/java/com/dumuzeyn/mp3player/MainActivity.java`.

### Menu Wheel And Swipe Behavior

The top menu now works as an independent horizontal wheel. The user can scroll it freely with a finger, and scrolling the wheel does not switch the current section. This is intentional: the user can rotate the wheel, inspect nearby sections, and then tap the exact section they want.

A section changes only in two cases:

1. The user taps a section name in the top wheel.
2. The user swipes across the main screen area, outside the menu wheel.

All touch events pass through `dispatchTouchEvent(...)` and then `handlePageSwipe(...)`. The method `isInsideTabs(...)` checks whether the gesture started inside the `HorizontalScrollView` that contains the menu wheel. If the gesture started on the wheel, it is treated as wheel scrolling only. If it started below the wheel, in the main content area, the app switches to the previous or next tab.

When a tab changes, `switchTabAnimated(...)` runs. It does two things at the same time:

- slides the current section list out and slides the new section list in;
- moves the top wheel to the active section.

The wheel movement does not use the default long `smoothScrollTo(...)`. Instead, `animateTabsScrollTo(...)` uses `ValueAnimator`, which makes the movement short and controlled. `scrollTabsToActive(...)` searches for the closest repeated copy of the target tab in the endless wheel. This is why switching between `Settings` and `Songs` should not spin through the whole list; it should choose the nearest visible path.

### Settings Section

The app now has a `Settings` section. It lives in the same top wheel as `Songs`, `Favorites`, `Playlists`, `Genres`, `Artists`, and `Albums`.

Theme switching was moved into `Settings`. The old theme button near `MP3 Player` in the header was removed so the header stays cleaner and does not mix global settings with everyday playback actions.

Settings contains:

- light/dark theme switching;
- interface language selection: `English` or `Russian`;
- a GitHub project link;
- delete all songs from the app, with confirmation;
- delete all playlists, with confirmation.

Deleting all songs from the app does not delete files from the phone. It clears only the app library, favorites, and song references inside playlists, then saves the new state through `TrackStore.save(...)` and `saveState()`.

Deleting all playlists clears only the `playlists` collection. The songs remain in the app.

All Settings text is aligned to the left. This is handled in `addSettingsButton(...)`; the language buttons also use `Gravity.START | Gravity.CENTER_VERTICAL`.

### Language Switching

The selected language is stored in `SharedPreferences` under the `LANGUAGE` key. Main interface labels use `tr(String en, String ru)`. If English is selected, the first string is returned; if Russian is selected, the second string is returned.

Tab labels are rebuilt in `refreshTabLabels()`. After the language changes, the interface is rebuilt through `buildUi()`, so the tab names update immediately.

### Where To Change This Logic

- `handlePageSwipe(...)` - detects screen swipes.
- `isInsideTabs(...)` - prevents page swipes from triggering when the gesture starts on the menu wheel.
- `switchTabAnimated(...)` - controls the animated section transition.
- `scrollTabsToActive(...)` - selects the closest copy of the target tab in the endless wheel.
- `animateTabsScrollTo(...)` - quickly and smoothly moves the menu wheel.
- `renderSettings(...)` - renders the Settings section.
- `addSettingsButton(...)` - defines Settings button style and left text alignment.
- `refreshTabLabels()` - builds tab names for the selected language.
- `saveState()` - saves theme, language, favorites, playlists, and custom timer value.

## Building the APK

On Windows, open PowerShell in the project root and run:

```powershell
.\build-apk.ps1
```

The script automatically:

1. Checks for a local Android SDK in `.android-sdk`.
2. Downloads Android command line tools on the first run.
3. Installs `platforms;android-35`, `build-tools;35.0.0`, and `platform-tools`.
4. Compiles resources with `aapt2`.
5. Links the APK.
6. Compiles Java with `javac`.
7. Packs `.class` files into `classes.jar`.
8. Creates `classes.dex` with `d8`.
9. Adds dex to the APK.
10. Creates a local signing key if it does not exist.
11. Aligns the APK with `zipalign`.
12. Signs the APK with `apksigner`.
13. Verifies the signature.

After a successful build, the file appears here:

```text
output/MP3-Player.apk
```

The `.android-sdk` and `build` folders are temporary build folders and should not be committed.

## Installing through USB

If ADB is available, install the APK with:

```powershell
.\.android-sdk\platform-tools\adb.exe install -r output\MP3-Player.apk
```

USB developer access must be enabled on the phone, and the computer must be allowed in the Android authorization dialog.

## Project structure

```text
Apk/
|-- app/
|   `-- src/
|       `-- main/
|           |-- AndroidManifest.xml
|           |-- java/
|           |   `-- com/
|           |       `-- dumuzeyn/
|           |           `-- mp3player/
|           |               |-- MainActivity.java
|           |               |-- PlayerService.java
|           |               |-- Track.java
|           |               |-- TrackStore.java
|           |               `-- WaveformView.java
|           `-- res/
|               |-- drawable/
|               |   |-- ic_launcher.xml
|               |   `-- ic_music_vector.xml
|               `-- values/
|                   |-- strings.xml
|                   `-- styles.xml
|-- output/
|   `-- MP3-Player.apk
|-- build-apk.ps1
|-- LICENSE
`-- README.md
```

## License

The project is free for personal, educational, and non-commercial use. Commercial sale, commercial redistribution, resale, or inclusion in a commercial product requires separate permission. See `LICENSE` for details.


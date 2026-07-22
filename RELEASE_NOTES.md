# MP3 Player Voltune 3.0

## Русский

Версия 3.0 обновляет основу воспроизведения и ускоряет работу с большой библиотекой.

- Воспроизведение переведено на Media3 `ExoPlayer`, `MediaSessionService` и `MediaController`.
- Очередь, текущая песня, позиция, повтор и мини-плеер восстанавливаются надёжнее после возврата в приложение.
- Исправлено определение активной песни после перехода на Media3: снова работают гистограмма, кнопка паузы и вращение обложки.
- Переход в «Песни» больше не создаёт один и тот же список дважды.
- Для «Песен», «Избранного», жанров, исполнителей и альбомов заранее готовятся 15 карточек, поэтому пользователь не видит поэтапную загрузку первых элементов.
- Сохранение плейлистов и избранного выполняется вне главного потока и одной атомарной операцией.
- Фоновое сохранение состояния больше не перечитывает всю музыкальную библиотеку каждые несколько секунд.
- Добавлены проверки миграции библиотеки из 165 песен и производительности на библиотеке из 1 000 треков.
- При чистой установке интерфейс по умолчанию открывается на русском языке.

Все существующие возможности Voltune сохранены: фоновое воспроизведение, повтор, таймер сна, плейлисты, избранное, темы, эквалайзер, выравнивание громкости, системная медиапанель и адаптивный интерфейс для планшетов.

## English

Version 3.0 modernizes playback and improves responsiveness with large music libraries.

- Playback now uses Media3 `ExoPlayer`, `MediaSessionService`, and `MediaController`.
- Queue, current track, position, repeat mode, and mini-player state restore more reliably after returning to the app.
- Active-track matching was corrected after the Media3 migration, restoring waveform animation, pause state, and rotating artwork.
- Entering Songs no longer builds the same list twice.
- Songs, Favorites, Genres, Artists, and Albums prepare 15 cards before a transition, avoiding visibly staged initial loading.
- Playlist and favorite changes are persisted off the main thread in one atomic operation.
- Periodic playback persistence no longer reloads the complete library every few seconds.
- Coverage now includes a 165-track database migration and a 1,000-track startup/navigation benchmark.
- Clean installations default to Russian while existing language preferences remain unchanged.

All established Voltune features remain available, including background playback, repeat, sleep timer, playlists, favorites, themes, equalizer, volume leveling, system media controls, and adaptive tablet layouts.

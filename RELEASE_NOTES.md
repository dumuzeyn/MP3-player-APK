# MP3 Player Voltune 2.5.1

## Русский

Обновление стабильности воспроизведения, звука и оформления.

- Состояние очереди, плейлиста и повтора надёжнее синхронизируется с фоновым сервисом после выхода и возвращения в приложение.
- Исправлены индикаторы активного плейлиста, вращение его обложки и остановка диска на паузе.
- Перемотка круглой обложки работает как диск: вперёд и назад без лишнего оборота после отпускания пальца.
- Скорость титров плейлистов можно установить в `0`, полностью остановив прокрутку.
- Для основного интерфейса и большого плеера можно отдельно выбрать однотонный фон, градиент, изображение или GIF и настроить размытие.
- Медиафон проверяется до сохранения и декодируется только как растровое изображение без выполнения ссылок, скриптов и метаданных.
- Добавлены готовые профили эквалайзера и сохранение собственной конфигурации.
- Выравнивание громкости анализирует каждый трек и меняет усиление плавно, без резких скачков внутри песни.
- Сохраняются позиции прокрутки разделов, память мини-плеера и исходные названия песен; обложки плейлистов не загружаются заново без необходимости.
- Окно свойств песни стало компактным: его высота определяется содержимым, а все действия выровнены единым столбцом.

## English

An update focused on playback stability, sound, and visual customization.

- Queue, playlist, and repeat state now stay synchronized with the foreground service more reliably after leaving and returning to the app.
- Active playlist indicators, rotating playlist artwork, and paused disc behavior are corrected.
- Circular artwork seeking behaves like a turntable in both directions without an extra rotation after release.
- Playlist ticker speed can be set to `0` for a completely static preview.
- The main interface and full player can independently use a solid color, gradient, validated image, or GIF with adjustable blur.
- Visual media is validated before saving and decoded strictly as raster pixels without executing links, scripts, or metadata.
- Equalizer presets are available while a custom profile remains remembered.
- Volume leveling analyzes each track and applies smooth gain changes without abrupt shifts inside a song.
- Section scroll positions, mini-player memory, and original track titles are preserved; playlist artwork is retained instead of visibly reloading.
- The song actions window is content-sized and compact, with all actions aligned in one consistent column.

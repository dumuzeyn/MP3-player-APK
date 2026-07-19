# MP3 Player Voltune 2.5.2

## Русский

Обновление удобства настроек и навигации по библиотеке.

- Окна темы, языка, памяти мини-плеера и фона больше не закрываются после каждого изменения.
- В окне темы всегда доступны «Светлая», «Тёмная» и «Своя»; дополнительные цвета появляются только после выбора «Своя» без выхода из окна.
- В пользовательской теме кнопка «Готово» закреплена внизу, а длинный список параметров прокручивается отдельно.
- Размытая тень текста заменена чётким контуром с включением, выключением и выбором цвета.
- Кнопка «По умолчанию» отключает контур и возвращает автоматический контрастный цвет.
- В настройках частиц кнопка «По умолчанию» восстанавливает оба цвета темы, частоту, размер и время жизни.
- Позиция длинного списка восстанавливается до его появления: при возврате в «Песни» больше не видно прокрутку сверху.
- Анимированный предпросмотр соседней вкладки использует ту же сохранённую позицию и не сбрасывает пакетную загрузку песен.

Также включены все изменения версии 2.5.1:

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
- Скорость вращения обложки-диска в большом плеере регулируется от 25% до 200%; перемотка учитывает выбранную скорость.
- Для частиц можно независимо выбрать два цвета, сохранив цвета темы как исходный вариант.
- Цвет текста настраивается отдельно от темы и акцента; при необходимости включается контур с собственным цветом.
- Фоновые тесты Android 15/16 теперь проверяют подтверждённое воспроизведение и не принимают краткий переход между треками за остановку.
- Планшетная проверка CI отделена от аудиотестов и проверяет только конфигурацию приложения и адаптивную разметку.

## English

An update focused on stable settings and library navigation.

- Theme, language, mini-player memory, and background dialogs remain open while values change.
- Light, Dark, and Custom always remain available; additional color controls appear only after Custom is selected without leaving the dialog.
- The Done action stays pinned while the longer custom-theme controls scroll independently.
- Blurred text shadows are replaced with a crisp outline that can be toggled and colored.
- A default action disables the outline and restores its automatic contrast color.
- Particle defaults restore both theme colors as well as frequency, size, and lifetime sliders.
- Long lists are positioned before becoming visible, so returning to Songs no longer shows a jump from the top.
- Animated adjacent-tab previews use the same remembered position without disturbing batched song rendering.

This release also includes every change from 2.5.1:

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
- Full-player disc rotation is adjustable from 25% to 200%, and turntable seeking follows the selected speed.
- Two particle colors can be selected independently while theme colors remain the default palette.
- Text color is independent from theme and accent colors, with an optional configurable outline for contrast.
- Android 15/16 background tests now observe confirmed playback instead of treating a brief track transition as a stop.
- Tablet CI is separated from audio scenarios and focuses on application configuration and responsive layout.

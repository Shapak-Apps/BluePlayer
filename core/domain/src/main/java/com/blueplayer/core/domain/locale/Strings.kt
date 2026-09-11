package com.blueplayer.core.domain.locale

object Strings {
    fun tracks(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Треки"
        AppLanguage.ENGLISH -> "Tracks"
        AppLanguage.TURKMEN -> "Aýdymlar"
    }
    fun albums(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Альбомы"
        AppLanguage.ENGLISH -> "Albums"
        AppLanguage.TURKMEN -> "Albomlar"
    }
    fun artists(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Исполнители"
        AppLanguage.ENGLISH -> "Artists"
        AppLanguage.TURKMEN -> "Ýerine ýetirijiler"
    }
    fun folders(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Папки"
        AppLanguage.ENGLISH -> "Folders"
        AppLanguage.TURKMEN -> "Papkalar"
    }
    fun favorites(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Избранное"
        AppLanguage.ENGLISH -> "Favorites"
        AppLanguage.TURKMEN -> "Halanýanlar"
    }
    fun playlists(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Плейлисты"
        AppLanguage.ENGLISH -> "Playlists"
        AppLanguage.TURKMEN -> "Playlistler"
    }
    fun settings(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Настройки"
        AppLanguage.ENGLISH -> "Settings"
        AppLanguage.TURKMEN -> "Sazlamalar"
    }
    fun sections(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Разделы"
        AppLanguage.ENGLISH -> "Sections"
        AppLanguage.TURKMEN -> "Bölümler"
    }
    fun nowPlaying(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Сейчас играет"
        AppLanguage.ENGLISH -> "Now Playing"
        AppLanguage.TURKMEN -> "Häzir çalýar"
    }

    fun retry(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Повторить"
        AppLanguage.ENGLISH -> "Retry"
        AppLanguage.TURKMEN -> "Gaýtadan"
    }
    fun refresh(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Обновить"
        AppLanguage.ENGLISH -> "Refresh"
        AppLanguage.TURKMEN -> "Täzele"
    }
    fun back(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Назад"
        AppLanguage.ENGLISH -> "Back"
        AppLanguage.TURKMEN -> "Yza"
    }
    fun cancel(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Отмена"
        AppLanguage.ENGLISH -> "Cancel"
        AppLanguage.TURKMEN -> "Goýbolsun"
    }
    fun save(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Сохранить"
        AppLanguage.ENGLISH -> "Save"
        AppLanguage.TURKMEN -> "Saklamak"
    }
    fun delete(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Удалить"
        AppLanguage.ENGLISH -> "Delete"
        AppLanguage.TURKMEN -> "Pozmak"
    }
    fun create(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Создать"
        AppLanguage.ENGLISH -> "Create"
        AppLanguage.TURKMEN -> "Döretmek"
    }
    fun rename(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Переименовать"
        AppLanguage.ENGLISH -> "Rename"
        AppLanguage.TURKMEN -> "Adyny üýtgetmek"
    }
    fun playAll(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Воспроизвести всё"
        AppLanguage.ENGLISH -> "Play all"
        AppLanguage.TURKMEN -> "Hemmesini çalmak"
    }
    fun play(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Играть"
        AppLanguage.ENGLISH -> "Play"
        AppLanguage.TURKMEN -> "Çalmak"
    }

    // Количество
    fun tracksCount(lang: AppLanguage, count: Int) = when (lang) {
        AppLanguage.RUSSIAN -> "$count треков"
        AppLanguage.ENGLISH -> "$count tracks"
        AppLanguage.TURKMEN -> "$count aýdym"
    }
    fun albumsCount(lang: AppLanguage, count: Int) = when (lang) {
        AppLanguage.RUSSIAN -> "$count альбомов"
        AppLanguage.ENGLISH -> "$count albums"
        AppLanguage.TURKMEN -> "$count albom"
    }

    // Пустые состояния
    fun noTracks(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Музыка не найдена"
        AppLanguage.ENGLISH -> "No music found"
        AppLanguage.TURKMEN -> "Saz tapylmady"
    }
    fun noAlbums(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Альбомы не найдены"
        AppLanguage.ENGLISH -> "No albums found"
        AppLanguage.TURKMEN -> "Albom tapylmady"
    }
    fun noArtists(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Исполнители не найдены"
        AppLanguage.ENGLISH -> "No artists found"
        AppLanguage.TURKMEN -> "Ýerine ýetirijiler tapylmady"
    }
    fun noFolders(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Папки не найдены"
        AppLanguage.ENGLISH -> "No folders found"
        AppLanguage.TURKMEN -> "Papka tapylmady"
    }
    fun noFavorites(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Пока нет избранных треков"
        AppLanguage.ENGLISH -> "No favorite tracks yet"
        AppLanguage.TURKMEN -> "Häzirlikçe halanýan aýdym ýok"
    }
    fun noFavoritesHint(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Нажми на сердечко в плеере"
        AppLanguage.ENGLISH -> "Tap the heart in the player"
        AppLanguage.TURKMEN -> "Pleýerdäki ýürek düwmesine basyň"
    }
    fun noPlaylists(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Плейлистов пока нет"
        AppLanguage.ENGLISH -> "No playlists yet"
        AppLanguage.TURKMEN -> "Häzirlikçe playlist ýok"
    }
    fun noPlaylistsHint(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Нажми + чтобы создать"
        AppLanguage.ENGLISH -> "Tap + to create"
        AppLanguage.TURKMEN -> "+ düwmesine basyň"
    }
    fun nothingPlaying(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Ничего не играет"
        AppLanguage.ENGLISH -> "Nothing playing"
        AppLanguage.TURKMEN -> "Hiç zat çalynmaýar"
    }

    fun errorLoadTracks(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Не удалось загрузить музыку"
        AppLanguage.ENGLISH -> "Failed to load music"
        AppLanguage.TURKMEN -> "Sazy ýüklemek başartmady"
    }
    fun errorLoadAlbums(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Не удалось загрузить альбомы"
        AppLanguage.ENGLISH -> "Failed to load albums"
        AppLanguage.TURKMEN -> "Albomlary ýüklemek başartmady"
    }

    fun theme(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Тема"
        AppLanguage.ENGLISH -> "Theme"
        AppLanguage.TURKMEN -> "Tema"
    }
    fun themeSystem(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Системная"
        AppLanguage.ENGLISH -> "System"
        AppLanguage.TURKMEN -> "Ulgam"
    }
    fun themeLight(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Светлая"
        AppLanguage.ENGLISH -> "Light"
        AppLanguage.TURKMEN -> "Ýagty"
    }
    fun themeDark(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Тёмная"
        AppLanguage.ENGLISH -> "Dark"
        AppLanguage.TURKMEN -> "Garaňky"
    }
    fun language(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Язык"
        AppLanguage.ENGLISH -> "Language"
        AppLanguage.TURKMEN -> "Dil"
    }
    fun about(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "О приложении"
        AppLanguage.ENGLISH -> "About"
        AppLanguage.TURKMEN -> "Programma barada"
    }
    fun version(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Версия"
        AppLanguage.ENGLISH -> "Version"
        AppLanguage.TURKMEN -> "Wersiýa"
    }
    fun developer(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Разработчик"
        AppLanguage.ENGLISH -> "Developer"
        AppLanguage.TURKMEN -> "Düzediji"
    }
    fun github(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "GitHub"
        AppLanguage.ENGLISH -> "GitHub"
        AppLanguage.TURKMEN -> "GitHub"
    }

    fun newPlaylist(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Новый плейлист"
        AppLanguage.ENGLISH -> "New playlist"
        AppLanguage.TURKMEN -> "Täze playlist"
    }
    fun playlistName(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Название"
        AppLanguage.ENGLISH -> "Name"
        AppLanguage.TURKMEN -> "Ady"
    }
    fun deletePlaylistTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Удалить плейлист?"
        AppLanguage.ENGLISH -> "Delete playlist?"
        AppLanguage.TURKMEN -> "Playlisti pozmakmy?"
    }
    fun deletePlaylistText(lang: AppLanguage, name: String) = when (lang) {
        AppLanguage.RUSSIAN -> "«$name» будет удалён навсегда."
        AppLanguage.ENGLISH -> "«$name» will be deleted forever."
        AppLanguage.TURKMEN -> "«$name» hemişelik pozular."
    }
    // Поиск
    fun searchHint(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Поиск треков, альбомов, исполнителей"
        AppLanguage.ENGLISH -> "Search tracks, albums, artists"
        AppLanguage.TURKMEN -> "Aýdymlary, albomlary, ýerine ýetirijileri gözle"
    }
    fun noResults(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Ничего не найдено"
        AppLanguage.ENGLISH -> "No results"
        AppLanguage.TURKMEN -> "Hiç zat tapylmady"
    }
    fun seeAll(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Все"
        AppLanguage.ENGLISH -> "See all"
        AppLanguage.TURKMEN -> "Hemmesi"
    }
    fun shuffle(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Перемешать"
        AppLanguage.ENGLISH -> "Shuffle"
        AppLanguage.TURKMEN -> "Garyşdyrmak"
    }
    fun repeat(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Повтор"
        AppLanguage.ENGLISH -> "Repeat"
        AppLanguage.TURKMEN -> "Gaýtalama"
    }
    fun removeFromFavorites(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Убрать из избранного"
        AppLanguage.ENGLISH -> "Remove from favorites"
        AppLanguage.TURKMEN -> "Halanýanlardan aýyr"
    }
    fun sleepTimer(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Таймер сна"
        AppLanguage.ENGLISH -> "Sleep timer"
        AppLanguage.TURKMEN -> "Uklamak üçin taýmer"
    }
    fun sleepTimerActive(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Таймер активен"
        AppLanguage.ENGLISH -> "Timer active"
        AppLanguage.TURKMEN -> "Taýmer işjeň"
    }
    fun minutes(lang: AppLanguage, n: Int) = when (lang) {
        AppLanguage.RUSSIAN -> "$n мин"
        AppLanguage.ENGLISH -> "$n min"
        AppLanguage.TURKMEN -> "$n min"
    }
    fun equalizer(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Эквалайзер"
        AppLanguage.ENGLISH -> "Equalizer"
        AppLanguage.TURKMEN -> "Ekwalizer"
    }
    fun equalizerNotAvailable(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Эквалайзер недоступен"
        AppLanguage.ENGLISH -> "Equalizer not available"
        AppLanguage.TURKMEN -> "Ekwalizer elýeterli däl"
    }
    fun addToPlaylist(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Добавить в плейлист"
        AppLanguage.ENGLISH -> "Add to playlist"
        AppLanguage.TURKMEN -> "Playliste goş"
    }
    fun selectPlaylist(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Выберите плейлист"
        AppLanguage.ENGLISH -> "Select playlist"
        AppLanguage.TURKMEN -> "Playlist saýlaň"
    }
    fun search(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Поиск"
        AppLanguage.ENGLISH -> "Search"
        AppLanguage.TURKMEN -> "Gözleg"
    }
    fun eqPresets(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Пресеты"
        AppLanguage.ENGLISH -> "Presets"
        AppLanguage.TURKMEN -> "Presetler"
    }
    fun eqBass(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Бас"
        AppLanguage.ENGLISH -> "Bass"
        AppLanguage.TURKMEN -> "Bass"
    }
    fun eqVirtualizer(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Объём"
        AppLanguage.ENGLISH -> "Virtualizer"
        AppLanguage.TURKMEN -> "Göwrüm"
    }
    fun eqNotAvailable(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Сначала запусти трек — эквалайзер подключится к сессии воспроизведения."
        AppLanguage.ENGLISH -> "Play a track first — the equalizer will attach to the playback session."
        AppLanguage.TURKMEN -> "Ilki bilen aýdym goýuň — ekwalizer sessiýa birikdiriler."
    }
    fun home(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Главный экран"
        AppLanguage.ENGLISH -> "Home"
        AppLanguage.TURKMEN -> "Baş sahypa"
    }
    fun myMusic(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Моя музыка"
        AppLanguage.ENGLISH -> "My music"
        AppLanguage.TURKMEN -> "Meniň aýdymlarym"
    }
    fun soundEffects(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Звуковые эффекты"
        AppLanguage.ENGLISH -> "Sound effects"
        AppLanguage.TURKMEN -> "Ses effekleri"
    }
    fun queue(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Очередь"
        AppLanguage.ENGLISH -> "Queue"
        AppLanguage.TURKMEN -> "Nobat"
    }
    fun genres(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Жанры"
        AppLanguage.ENGLISH -> "Genres"
        AppLanguage.TURKMEN -> "Žanrlar"
    }
    fun exit(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Выход"
        AppLanguage.ENGLISH -> "Exit"
        AppLanguage.TURKMEN -> "Çykyş"
    }
    fun noLyrics(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "(нет текста)"
        AppLanguage.ENGLISH -> "(no lyrics)"
        AppLanguage.TURKMEN -> "(söz ýok)"
    }
    fun info(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Информация о файле"
        AppLanguage.ENGLISH -> "File info"
        AppLanguage.TURKMEN -> "Faýl maglumaty"
    }
    fun addToFavorites(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Добавить в избранное"
        AppLanguage.ENGLISH -> "Add to favorites"
        AppLanguage.TURKMEN -> "Halanýanlara goş"
    }
    fun goToAlbum(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Перейти к альбому"
        AppLanguage.ENGLISH -> "Go to album"
        AppLanguage.TURKMEN -> "Alboma geç"
    }
    fun share(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Поделиться"
        AppLanguage.ENGLISH -> "Share"
        AppLanguage.TURKMEN -> "Paýlaş"
    }
    fun sendToPlaylists(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Отправить в плейлисты"
        AppLanguage.ENGLISH -> "Add to playlists"
        AppLanguage.TURKMEN -> "Playlistlere goş"
    }
    fun bookmarks(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Закладки"
        AppLanguage.ENGLISH -> "Bookmarks"
        AppLanguage.TURKMEN -> "Bellikler"
    }
    fun sleepStopTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Остановить воспроизведение"
        AppLanguage.ENGLISH -> "Stop playback"
        AppLanguage.TURKMEN -> "Oýnatmany duruz"
    }
    fun sleepAfterTime(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "через указанное время"
        AppLanguage.ENGLISH -> "after specified time"
        AppLanguage.TURKMEN -> "görkezilen wagtdan soň"
    }
    fun sleepTrackEnd(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "по окончании трека"
        AppLanguage.ENGLISH -> "after track ends"
        AppLanguage.TURKMEN -> "aýdym gutaranda"
    }
    fun sleepQueueEnd(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "по окончании очереди"
        AppLanguage.ENGLISH -> "after queue ends"
        AppLanguage.TURKMEN -> "nobat gutaranda"
    }
    fun sleepWait(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Дождаться завершения проигрываемого трека"
        AppLanguage.ENGLISH -> "Wait for the current track to finish"
        AppLanguage.TURKMEN -> "Häzirki aýdym gutarýança garaş"
    }
    fun start(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Старт"
        AppLanguage.ENGLISH -> "Start"
        AppLanguage.TURKMEN -> "Başla"
    }
    fun preGain(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Предусиление"
        AppLanguage.ENGLISH -> "Preamp"
        AppLanguage.TURKMEN -> "Öňküçlenme"
    }
    fun off(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "OFF"
        AppLanguage.ENGLISH -> "OFF"
        AppLanguage.TURKMEN -> "OFF"
    }
    fun auto(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "AUTO"
        AppLanguage.ENGLISH -> "AUTO"
        AppLanguage.TURKMEN -> "AUTO"
    }
    fun settingsInterface(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Интерфейс"
        AppLanguage.ENGLISH -> "Interface"
        AppLanguage.TURKMEN -> "Interfeýs"
    }
    fun settingsInterfaceDesc(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Внешний вид, тема, язык"
        AppLanguage.ENGLISH -> "Appearance, theme, language"
        AppLanguage.TURKMEN -> "Daşky görnüş, tema, dil"
    }
    fun settingsSound(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Звук"
        AppLanguage.ENGLISH -> "Sound"
        AppLanguage.TURKMEN -> "Ses"
    }
    fun settingsSoundDesc(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Эквалайзер, нормализация громкости"
        AppLanguage.ENGLISH -> "Equalizer, loudness normalization"
        AppLanguage.TURKMEN -> "Ekwalizer, ses kadalaşdyrmak"
    }
    fun normalize(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Нормализация громкости"
        AppLanguage.ENGLISH -> "Loudness normalization"
        AppLanguage.TURKMEN -> "Ses kadalaşdyrmak"
    }
    fun aboutDesc(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Версия, разработчик, GitHub"
        AppLanguage.ENGLISH -> "Version, developer, GitHub"
        AppLanguage.TURKMEN -> "Wersiýa, düzediji, GitHub"
    }
    fun deleteFromDevice(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Удалить с устройства"
        AppLanguage.ENGLISH -> "Delete from device"
        AppLanguage.TURKMEN -> "Enjamdan poz"
    }
    fun deleteTrackTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Удалить трек?"
        AppLanguage.ENGLISH -> "Delete track?"
        AppLanguage.TURKMEN -> "Aýdymy pozmakmy?"
    }
    fun deleteTrackText(lang: AppLanguage, name: String) = when (lang) {
        AppLanguage.RUSSIAN -> "«$name» будет навсегда удалён с устройства и из приложения. Это действие нельзя отменить."
        AppLanguage.ENGLISH -> "«$name» will be permanently deleted from your device and from the app. This action cannot be undone."
        AppLanguage.TURKMEN -> "«$name» enjamdan we programmadan hemişelik pozular. Bu hereketi yzyna almak mümkin däl."
    }
    fun trackDeleted(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Трек удалён"
        AppLanguage.ENGLISH -> "Track deleted"
        AppLanguage.TURKMEN -> "Aýdym pozuldy"
    }
    fun deleteFailed(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Не удалось удалить файл"
        AppLanguage.ENGLISH -> "Failed to delete file"
        AppLanguage.TURKMEN -> "Faýly pozmak başartmady"
    }
    fun deleteNotAllowed(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Android не разрешает удалить этот файл"
        AppLanguage.ENGLISH -> "Android does not allow deleting this file"
        AppLanguage.TURKMEN -> "Android bu faýly pozmaga rugsat bermeýär"
    }
    fun aboutDescription(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Современный музыкальный плеер для Android с нативным C++ бас-движком и Material 3 дизайном."
        AppLanguage.ENGLISH -> "A modern music player for Android with a native C++ bass engine and Material 3 design."
        AppLanguage.TURKMEN -> "Android üçin C++ bas dwigately we Material 3 dizaýnly häzirki zaman aýdym pleýeri."
    }

    fun aboutFeatures(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "✦ Фоновое воспроизведение\n✦ 15 пресетов эквалайзера + нативный бас\n✦ A-B луп, закладки, таймер сна\n✦ Обложки из iTunes API\n✦ 3 языка интерфейса"
        AppLanguage.ENGLISH -> "✦ Background playback\n✦ 15 EQ presets + native bass engine\n✦ A-B loop, bookmarks, sleep timer\n✦ Online cover art from iTunes\n✦ 3 interface languages"
        AppLanguage.TURKMEN -> "✦ Fon aýdymy\n✦ 15 EQ presety + tebigy bas\n✦ A-B gaýtalama, bellikler, uklaýyş taýmeri\n✦ iTunes-den onlaýn oblozhka\n✦ 3 interfeýs dili"
    }

    fun viewSource(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Исходный код на GitHub"
        AppLanguage.ENGLISH -> "View source on GitHub"
        AppLanguage.TURKMEN -> "GitHub-da çeşme kody"
    }

    fun shareApp(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Поделиться приложением"
        AppLanguage.ENGLISH -> "Share app"
        AppLanguage.TURKMEN -> "Programmany paýlaş"
    }

    fun sendFeedback(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Отправить отзыв"
        AppLanguage.ENGLISH -> "Send feedback"
        AppLanguage.TURKMEN -> "Seslenme iber"
    }

    fun madeWithLove(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Сделано с любовью в Туркменистане"
        AppLanguage.ENGLISH -> "Made with love in Turkmenistan"
        AppLanguage.TURKMEN -> "Türkmenistanda söýgi bilen ýasaldy"
    }

    fun settingsPlayback(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Воспроизведение"
        AppLanguage.ENGLISH -> "Playback"
        AppLanguage.TURKMEN -> "Aýdyş"
    }

    fun settingsPlaybackDesc(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Crossfade, фокус аудио, очередь"
        AppLanguage.ENGLISH -> "Crossfade, audio focus, queue"
        AppLanguage.TURKMEN -> "Crossfade, ses fokusy, nobat"
    }

    fun crossfade(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Плавный переход (crossfade)"
        AppLanguage.ENGLISH -> "Crossfade"
        AppLanguage.TURKMEN -> "Ýumşak geçiş"
    }

    fun crossfadeOff(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Выкл"
        AppLanguage.ENGLISH -> "Off"
        AppLanguage.TURKMEN -> "Öç"
    }

    fun crossfadeSeconds(lang: AppLanguage, seconds: Int) = when (lang) {
        AppLanguage.RUSSIAN -> "$seconds сек"
        AppLanguage.ENGLISH -> "$seconds sec"
        AppLanguage.TURKMEN -> "$seconds sek"
    }

    fun audioFocus(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Приоритет аудио"
        AppLanguage.ENGLISH -> "Audio focus"
        AppLanguage.TURKMEN -> "Ses fokusy"
    }

    fun audioFocusPause(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Пауза"
        AppLanguage.ENGLISH -> "Pause"
        AppLanguage.TURKMEN -> "Pauza"
    }

    fun audioFocusDuck(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Приглушить"
        AppLanguage.ENGLISH -> "Duck"
        AppLanguage.TURKMEN -> "Peselt"
    }

    fun audioFocusIgnore(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Игнорировать"
        AppLanguage.ENGLISH -> "Ignore"
        AppLanguage.TURKMEN -> "Üns berme"
    }

    fun keepScreenOn(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Не выключать экран при воспроизведении"
        AppLanguage.ENGLISH -> "Keep screen on while playing"
        AppLanguage.TURKMEN -> "Aýdym wagty ekrany öçürme"
    }

    fun queuePersistence(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Сохранять очередь между запусками"
        AppLanguage.ENGLISH -> "Persist queue between launches"
        AppLanguage.TURKMEN -> "Nobaty açylyşlaryň arasynda sakla"
    }

    fun dynamicColors(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Динамические цвета (Material You)"
        AppLanguage.ENGLISH -> "Dynamic colors (Material You)"
        AppLanguage.TURKMEN -> "Dinamiki reňkler (Material You)"
    }

    fun accentColor(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Акцентный цвет"
        AppLanguage.ENGLISH -> "Accent color"
        AppLanguage.TURKMEN -> "Aksent reňki"
    }

    fun settingsAdvanced(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Дополнительно"
        AppLanguage.ENGLISH -> "Advanced"
        AppLanguage.TURKMEN -> "Goşmaça"
    }

    fun settingsAdvancedDesc(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Кэш, сброс, отладка"
        AppLanguage.ENGLISH -> "Cache, reset, debug"
        AppLanguage.TURKMEN -> "Keş, täzeden, debug"
    }

    fun clearImageCache(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Очистить кэш обложек"
        AppLanguage.ENGLISH -> "Clear image cache"
        AppLanguage.TURKMEN -> "Oblozhka keşini arassala"
    }

    fun cacheCleared(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Кэш очищен"
        AppLanguage.ENGLISH -> "Cache cleared"
        AppLanguage.TURKMEN -> "Keş arassalandy"
    }

    fun resetSettings(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Сбросить все настройки"
        AppLanguage.ENGLISH -> "Reset all settings"
        AppLanguage.TURKMEN -> "Ähli sazlamalary täzeden düz"
    }

    fun resetSettingsConfirmTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Сбросить настройки?"
        AppLanguage.ENGLISH -> "Reset settings?"
        AppLanguage.TURKMEN -> "Sazlamalary täzeden düzmekmi?"
    }

    fun resetSettingsConfirmText(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Все ваши настройки будут возвращены к значениям по умолчанию. Это действие нельзя отменить."
        AppLanguage.ENGLISH -> "All your settings will be restored to defaults. This cannot be undone."
        AppLanguage.TURKMEN -> "Ähli sazlamalaryňyz deslapky ýagdaýyna gaýdyp geler. Bu hereket yzyna alynmaýar."
    }

    fun settingsReset(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Настройки сброшены"
        AppLanguage.ENGLISH -> "Settings reset"
        AppLanguage.TURKMEN -> "Sazlamalar täzeden düzüldi"
    }

    fun showWaveform(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Показывать волну в плеере"
        AppLanguage.ENGLISH -> "Show waveform in player"
        AppLanguage.TURKMEN -> "Pleýerde tolkuny görkez"
    }

    fun hapticFeedback(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Виброотдача"
        AppLanguage.ENGLISH -> "Haptic feedback"
        AppLanguage.TURKMEN -> "Wibrasiýa"
    }
    fun organizationTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Об организации"
        AppLanguage.ENGLISH -> "About the Organization"
        AppLanguage.TURKMEN -> "Gurama barada"
    }

    fun organizationSub(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Команда, миссия и контакты"
        AppLanguage.ENGLISH -> "Team, mission and contacts"
        AppLanguage.TURKMEN -> "Topar, maksat we habarlaşma"
    }

    fun teamRole(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Мобильная разработка"
        AppLanguage.ENGLISH -> "Mobile development"
        AppLanguage.TURKMEN -> "Mobil programmalaşdyrma"
    }

    fun orgText(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Blue Player создан командой разработчиков Shapak из города Мары — центра Марыйского велаята Туркменистана."
        AppLanguage.ENGLISH -> "Blue Player is built by the Shapak development team from Mary, the heart of the Mary region in Turkmenistan."
        AppLanguage.TURKMEN -> "Blue Player Türkmenistanyň Mary welaýatynyň merkezi Mary şäherindäki Shapak programmistler topary tarapyndan döredildi."
    }

    fun missionTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Наша миссия"
        AppLanguage.ENGLISH -> "Our Mission"
        AppLanguage.TURKMEN -> "Biziň maksadymyz"
    }

    fun missionText(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Наша цель — помогать людям в Туркменистане и за его пределами: мы создаём бесплатные и удобные приложения, которые стирают языковые и технические барьеры и делают технологии доступными каждому."
        AppLanguage.ENGLISH -> "Our goal is to help people in Turkmenistan and beyond: we build free, friendly apps that remove language and technical barriers and make technology accessible to everyone."
        AppLanguage.TURKMEN -> "Bizim maksadymyz — Türkmenistanda we onuň çäginden daşarda ýaşaýanlara kömek etmek: dil we tehniki päsgelçilikleri aýryp, tehnologiýalary hemmelere elýeterli edýän mugt we amatly programmalar döredýäris."
    }

    fun orgGithub(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "GitHub организации"
        AppLanguage.ENGLISH -> "Organization GitHub"
        AppLanguage.TURKMEN -> "Guramanyň GitHub-y"
    }

    fun supportTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.RUSSIAN -> "Поддержка"
        AppLanguage.ENGLISH -> "Support"
        AppLanguage.TURKMEN -> "Goldaw"
    }
}
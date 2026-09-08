package com.blueplayer.app.ui.navigation

object Destinations {
    const val HOME = "home"
    const val LIBRARY = "library"
    const val ALBUMS = "albums"
    const val ARTISTS = "artists"
    const val GENRES = "genres"
    const val FOLDERS = "folders"
    const val FAVORITES = "favorites"
    const val QUEUE = "queue"
    const val PLAYLISTS = "playlists"
    const val SEARCH = "search"
    const val EQUALIZER = "equalizer"
    const val SETTINGS = "settings"
    const val NOW_PLAYING = "now_playing"

    const val ALBUM_DETAIL = "album_detail/{albumId}"
    const val ARTIST_DETAIL = "artist_detail/{artistId}"
    const val PLAYLIST_DETAIL = "playlist_detail/{playlistId}"

    fun albumDetail(albumId: String) = "album_detail/$albumId"
    fun artistDetail(artistId: String) = "artist_detail/$artistId"
    fun playlistDetail(playlistId: Long) = "playlist_detail/$playlistId"
}
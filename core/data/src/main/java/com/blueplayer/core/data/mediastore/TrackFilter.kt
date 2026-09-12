package com.blueplayer.core.data.mediastore

import com.blueplayer.core.domain.model.AppSettings
import com.blueplayer.core.domain.model.SortOrder
import com.blueplayer.core.domain.model.Track

object TrackFilter {

    fun apply(tracks: List<Track>, settings: AppSettings): List<Track> {
        val minDurationMs = settings.minTrackDurationSeconds * 1000L
        val excludedFolders = settings.excludedFolders.map { it.lowercase() }

        return tracks.filter { track ->
            if (minDurationMs > 0 && track.durationMs < minDurationMs) return@filter false
            val folderName = track.folderPath?.substringAfterLast('/')?.lowercase() ?: ""
            if (folderName in excludedFolders) return@filter false
            true
        }
    }

    fun sort(tracks: List<Track>, order: SortOrder): List<Track> = when (order) {
        SortOrder.TITLE_ASC -> tracks.sortedBy { it.title.lowercase() }
        SortOrder.TITLE_DESC -> tracks.sortedByDescending { it.title.lowercase() }
        SortOrder.DATE_ADDED_DESC -> tracks.sortedByDescending { it.dateAdded }
        SortOrder.DATE_ADDED_ASC -> tracks.sortedBy { it.dateAdded }
        SortOrder.DURATION_ASC -> tracks.sortedBy { it.durationMs }
        SortOrder.DURATION_DESC -> tracks.sortedByDescending { it.durationMs }
        SortOrder.ARTIST_ASC -> tracks.sortedBy { it.artist.lowercase() }
        SortOrder.ALBUM_ASC -> tracks.sortedBy { it.album.lowercase() }
    }

    fun applyAndSort(tracks: List<Track>, settings: AppSettings): List<Track> =
        sort(apply(tracks, settings), settings.defaultSortOrder)

    fun mergeCovers(
        tracks: List<Track>,
        settings: AppSettings,
        covers: Map<String, Any>
    ): List<Track> {
        if (!settings.onlineCoversEnabled) return tracks
        return tracks.map { track ->
            val online = covers[track.id] as? String
            if (online != null) {
                track.copy(artworkUri = online)
            } else {
                track
            }
        }
    }
}
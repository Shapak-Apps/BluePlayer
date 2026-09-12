package com.blueplayer.core.player

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.blueplayer.core.domain.model.Track

fun Track.toMediaItem(onlineCoverUrl: String? = null): MediaItem {
    val coverUri: Uri? = when {
        !onlineCoverUrl.isNullOrBlank() -> Uri.parse(onlineCoverUrl)
        !artworkUri.isNullOrBlank() -> runCatching { Uri.parse(artworkUri) }.getOrNull()
        else -> null
    }

    return MediaItem.Builder()
        .setMediaId(id)
        .setUri(Uri.parse(uri))
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artist)
                .setAlbumTitle(album)
                .apply {
                    coverUri?.let { setArtworkUri(it) }
                }
                .build()
        )
        .build()
}
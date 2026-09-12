package com.blueplayer.core.domain.model

data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val artistId: String,
    val album: String,
    val albumId: String,
    val durationMs: Long,
    val uri: String,
    val artworkUri: String?,
    val folderPath: String? = null,
    val sizeBytes: Long = 0L,
    val dateAdded: Long = 0L
)
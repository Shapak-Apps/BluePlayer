package com.blueplayer.core.domain.model

data class Album(
    val id: String,
    val title: String,
    val artist: String,
    val artworkUri: String?,
    val trackCount: Int
)
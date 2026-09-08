package com.blueplayer.core.domain.model

data class Playlist(
    val id: Long,
    val name: String,
    val trackCount: Int = 0
)
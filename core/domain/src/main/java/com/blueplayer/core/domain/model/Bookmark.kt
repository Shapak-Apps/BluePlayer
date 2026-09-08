package com.blueplayer.core.domain.model

data class Bookmark(
    val id: Long,
    val track: Track,
    val positionMs: Long,
    val addedAt: Long
)
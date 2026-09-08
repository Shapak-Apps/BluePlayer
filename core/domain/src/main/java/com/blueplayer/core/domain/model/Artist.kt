package com.blueplayer.core.domain.model

data class Artist(
    val id: String,
    val name: String,
    val albumCount: Int,
    val trackCount: Int
)